package com.pipeline

import com.matching.MultiJoinMatching
import com.query.{QueryStructure, WherePredicatePlanner, WhereClauseSparkTranslator}
import com.query.wrapper.CypherQueryWrapper
import com.sparkconfiguration.SparkHandler
import com.sparkmultigraph.bitmatrix.CompatibilityDomainEngine
import com.properties.{Property, PropertyType, PropertyValueLoader, LiteralCondition, VariableCondition}
import org.apache.spark.sql.{Column, DataFrame, SparkSession}
import org.apache.spark.sql.functions._

object FullPipeline {

  /** Accumulates stage timings for a single pipeline invocation. Not shared across runs. */
  private class Timings {
    private val buffer = scala.collection.mutable.ArrayBuffer[(String, Double)]()

    /** Times `block`, prints its elapsed time, and records it for the end-of-run summary. */
    def apply[T](label: String)(block: => T): T = {
      val start     = System.nanoTime()
      val result    = block
      val elapsedMs = (System.nanoTime() - start) / 1e6
      buffer += (label -> elapsedMs)
      //println(f"[TIMER] $label%-35s $elapsedMs%10.2f ms")
      result
    }

    def toSeq: Seq[(String, Double)] = buffer.toSeq
  }

  def main(args: Array[String]): Unit = {
    require(args.nonEmpty, "Usage: FullPipeline <cypherQuery> [dbName]")

    val cypherQuery = args(0)
    val dbName      = if (args.length > 1) args(1) else "panama_db"

    val spark = SparkHandler.spConfig("Full Pipeline")
    spark.conf.set("spark.sql.iceberg.check-nullability", "false")

    val overallStart = System.nanoTime()
    val display = new Timings

    val (result, pipelineTimings) = runWithTimings(spark, dbName, cypherQuery)

    val overallElapsedMs = (System.nanoTime() - overallStart) / 1e6

    result match {
      case Right((df, matchCount)) =>
        display("Display results") {
          println("\n--- MultiJoin Matches Summary ---")
          println(s"total_matches: $matchCount")
          println(s"=== Solutions for query on $dbName ===")
          df.show(20, truncate = false)
        }
      case Left(err) =>
        println(s"=== Pipeline failed ===\n$err")
    }

    val allTimings        = pipelineTimings ++ display.toSeq

    val width = (allTimings.map(_._1.length) :+ "TOTAL (wall clock)".length).max
    println("\n=== Pipeline Timing Summary ===")
    allTimings.foreach { case (label, ms) => println(f"${label.padTo(width, ' ')}  $ms%10.2f ms") }
    println("-" * (width + 15))
    println(f"${"TOTAL (wall clock)".padTo(width, ' ')}  $overallElapsedMs%10.2f ms")

    spark.stop()
  }

  /**
   * Runs a Cypher query end-to-end: parses it into a QueryStructure, computes the
   * per-edge compatibility domain against the target graph, and joins the domain
   * into full query solutions (one column per query node, bound to a matched target
   * node id).
   *
   * TODO: this currently returns the raw multijoin solutions (query node -> target node id).
   * It still needs to be projected down to whatever the query's RETURN clause actually asks for.
   */
  def run(spark: SparkSession, dbName: String, cypherQuery: String): Either[String, DataFrame] =
    runWithTimings(spark, dbName, cypherQuery)._1.map(_._1)

  /**
   * Same as `run`, but also returns this invocation's own stage timings (not shared across
   * calls) and the final match count. The count is computed here, under its own "Final
   * materialization" timer, so callers never need their own untimed `.count()`/`.collect()`
   * to materialize the result - every bit of work is attributed to a named stage.
   */
  def runWithTimings(spark: SparkSession, dbName: String, cypherQuery: String): (Either[String, (DataFrame, Long)], Seq[(String, Double)]) = {
    val timed = new Timings
    val result = for {
      queryStructure    <- timed("Cypher parsing")(CypherQueryWrapper.convert(cypherQuery))
      solutions         <- computeSolutions(spark, dbName, queryStructure, timed)
      materialized      <- timed("Final materialization") {
        val cached = solutions.cache()
        val count  = cached.count()
        Right((cached, count)): Either[String, (DataFrame, Long)]
      }
    } yield materialized
    (result, timed.toSeq)
  }

  private def computeSolutions(
      spark: SparkSession,
      dbName: String,
      queryStructure: QueryStructure,
      timed: Timings
  ): Either[String, DataFrame] = {
    val edges = queryStructure.getEdges.values.toSeq
    if (edges.isEmpty) return Left("Query must contain at least one edge")

    var compatibilityDomainDF = timed("Compatibility domain computation") {
      val df = CompatibilityDomainEngine.computeCompatibilityDomain(spark, dbName, queryStructure).cache()
      df.count() // force materialization so this stage's time isn't attributed to a later stage
      df
    }

    queryStructure.getWhereClause match {
      case None =>
        Right(timed("MultiJoin matching") {
          val df = MultiJoinMatching.computeMatches(compatibilityDomainDF, edges).cache()
          df.count()
          df
        })

      case Some(whereClause) =>
        val nodeVariables       = queryStructure.getNodes.keySet
        val referencedVariables = whereClause.getConditions.values.flatMap {
          case lc: LiteralCondition  => Seq(lc.variable)
          case vc: VariableCondition => Seq(vc.leftVar, vc.rightVar)
        }.toSet

        val unsupportedVariables = referencedVariables.diff(nodeVariables)
        if (unsupportedVariables.nonEmpty)
          return Left(
            s"WHERE conditions on ${unsupportedVariables.mkString(", ")} are not supported yet: " +
            "matching only tracks bound node ids, not edge ids, so edge-variable properties can't be resolved."
          )

        // --- PRE-JOIN: shrink candidate domains using conditions required by every OR-branch ---
        val plan = WherePredicatePlanner.classify(whereClause)
        compatibilityDomainDF = timed("Pre-join hoistable filtering") {
          plan.hoistable.foreach { case (variable, conditions) =>
            val propertyTypes = conditions.map(lc => lc.prop.name -> Property.typeOf(lc.prop)).toMap
            val propsDF       = PropertyValueLoader.loadNodeProperties(spark, dbName, propertyTypes)
            val allowedIds     = propsDF
              .filter(WhereClauseSparkTranslator.hoistableFilterColumn(conditions))
              .select(col("node_id").as("id"))

            compatibilityDomainDF = restrictVariableDomain(compatibilityDomainDF, variable, allowedIds).cache()
          }
          compatibilityDomainDF.count()
          compatibilityDomainDF
        }

        val matchesDF = timed("MultiJoin matching") {
          val df = MultiJoinMatching.computeMatches(compatibilityDomainDF, edges).cache()
          df.count()
          df
        }

        // --- POST-JOIN: evaluate the whole WHERE expression against every matched row ---
        val literalNeedsByVariable: Map[String, Map[String, PropertyType]] =
          whereClause.getConditions.values.collect { case lc: LiteralCondition => lc }
            .groupBy(_.variable)
            .map { case (variable, conds) => variable -> conds.map(c => c.prop.name -> Property.typeOf(c.prop)).toMap }

        val variableNeedsByVariable: Map[String, Set[String]] =
          whereClause.getConditions.values.collect { case vc: VariableCondition => vc }
            .flatMap(vc => Seq(vc.leftVar -> vc.leftProp, vc.rightVar -> vc.rightProp))
            .groupBy(_._1)
            .map { case (variable, entries) => variable -> entries.map(_._2).toSet }

        val withLiteralProps = timed("Literal property join") {
          // Candidate ids are looked up from matchesDF (already cached) rather than the
          // fold's own accumulator: the accumulator is uncached until the fold finishes, so
          // reading a variable's ids from it would force Spark to recompute every earlier
          // iteration's join from scratch just to answer "what ids does this variable have".
          val df = literalNeedsByVariable.foldLeft(matchesDF) { case (df, (variable, propertyTypes)) =>
            val propsDF = PropertyValueLoader.loadNodeProperties(spark, dbName, propertyTypes, Some(candidateIds(matchesDF, variable)))
            val renames = propertyTypes.keySet.flatMap(p => Seq(
              p              -> s"${variable}__$p",
              s"${p}__array" -> s"${variable}__${p}__array"
            )).toMap
            joinVariableProperties(df, variable, propsDF, renames)
          }.cache()
          df.count()
          df
        }

        val joined = timed("Variable property join") {
          // Same reasoning as above: read candidate ids from withLiteralProps (cached), not
          // the fold's own accumulator.
          val df = variableNeedsByVariable.foldLeft(withLiteralProps) { case (df, (variable, propertyNames)) =>
            val propsDF = PropertyValueLoader.loadNodePropertiesUntyped(spark, dbName, propertyNames, Some(candidateIds(withLiteralProps, variable)))
            val renames = propertyNames.flatMap(p => Seq(
              s"${p}__string"  -> s"${variable}__${p}__string",
              s"${p}__numeric" -> s"${variable}__${p}__numeric"
            )).toMap
            joinVariableProperties(df, variable, propsDF, renames)
          }.cache()
          df.count()
          df
        }

        val literalColumnFor : (String, String) => (Column, Column) =
          (variable, prop) => (col(s"${variable}__$prop"), col(s"${variable}__${prop}__array"))
        val variableColumnFor: (String, String) => (Column, Column) =
          (variable, prop) => (col(s"${variable}__${prop}__string"), col(s"${variable}__${prop}__numeric"))

        val filtered = timed("WHERE clause filtering") {
          val filterColumn = WhereClauseSparkTranslator.deferredFilterColumn(whereClause, literalColumnFor, variableColumnFor)
          val df = (filterColumn match {
            case Some(fc) => joined.filter(fc)
            case None     => joined
          }).cache()
          df.count()
          df
        }

        Right(timed("Final projection") {
          filtered.select(nodeVariables.toSeq.map(col): _*)
        })
    }
  }

  /**
   * Restricts a query variable's candidate ids (wherever it appears as either edge
   * endpoint, q1 or q2) to only those present in allowedIds - used to push hoistable
   * WHERE conditions down before the multijoin.
   */
  private def restrictVariableDomain(domainDF: DataFrame, variable: String, allowedIds: DataFrame): DataFrame = {
    def restrictColumn(df: DataFrame, queryCol: String, idCol: String): DataFrame = {
      val matching    = df.filter(col(queryCol) === variable)
        .join(broadcast(allowedIds.withColumnRenamed("id", idCol)), Seq(idCol))
      val nonMatching = df.filter(col(queryCol) =!= variable)
      matching.unionByName(nonMatching)
    }

    restrictColumn(restrictColumn(domainDF, "q1", "t1"), "q2", "t2")
  }

  /**
   * Left-joins a variable's property values (keyed by node_id) onto its bound target id
   * column in `baseDF`, renaming columns per `renames` so multiple variables sharing a
   * property name don't collide. A left join (not inner) preserves match rows even when
   * a candidate has no value for a requested property, since that's a legitimate input
   * to conditions like IS NULL / IS NOT NULL rather than something to silently drop.
   */
  private def joinVariableProperties(
      baseDF   : DataFrame,
      variable : String,
      propsDF  : DataFrame,
      renames  : Map[String, String]
  ): DataFrame = {
    val renamed = renames.foldLeft(propsDF) { case (df, (from, to)) => df.withColumnRenamed(from, to) }
    // propsDF was already restricted to baseDF's candidate ids for `variable` (see call sites),
    // so it's small enough to broadcast rather than shuffle-join.
    baseDF.join(broadcast(renamed), baseDF(variable) === renamed("node_id"), "left").drop("node_id")
  }

  /** The distinct ids `variable` is actually bound to in `df`, for restricting property loads/joins. */
  private def candidateIds(df: DataFrame, variable: String): DataFrame =
    df.select(col(variable).as("id")).distinct()
}
