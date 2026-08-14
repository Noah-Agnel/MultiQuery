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

  def main(args: Array[String]): Unit = {
    require(args.nonEmpty, "Usage: FullPipeline <cypherQuery> [dbName]")

    val cypherQuery = args(0)
    val dbName      = if (args.length > 1) args(1) else "panama_db"

    val spark = SparkHandler.spConfig("Full Pipeline")
    spark.conf.set("spark.sql.iceberg.check-nullability", "false")

    run(spark, dbName, cypherQuery) match {
      case Right(df) =>
        println("\n--- MultiJoin Matches Summary ---")
        df.agg(count("*").as("total_matches"))
            .show(truncate = false)
        println(s"=== Solutions for query on $dbName ===")
        df.show(20, truncate = false)
      case Left(err) =>
        println(s"=== Pipeline failed ===\n$err")
    }

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
  def run(spark: SparkSession, dbName: String, cypherQuery: String): Either[String, DataFrame] = {
    for {
      queryStructure <- CypherQueryWrapper.convert(cypherQuery)
      solutions      <- computeSolutions(spark, dbName, queryStructure)
    } yield solutions
  }

  private def computeSolutions(
      spark: SparkSession,
      dbName: String,
      queryStructure: QueryStructure
  ): Either[String, DataFrame] = {
    val edges = queryStructure.getEdges.values.toSeq
    if (edges.isEmpty) return Left("Query must contain at least one edge")

    var compatibilityDomainDF = CompatibilityDomainEngine.computeCompatibilityDomain(spark, dbName, queryStructure)

    queryStructure.getWhereClause match {
      case None =>
        Right(MultiJoinMatching.computeMatches(compatibilityDomainDF, edges))

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
        plan.hoistable.foreach { case (variable, conditions) =>
          val propertyTypes = conditions.map(lc => lc.prop.name -> Property.typeOf(lc.prop)).toMap
          val propsDF       = PropertyValueLoader.loadNodeProperties(spark, dbName, propertyTypes)
          val allowedIds     = propsDF
            .filter(WhereClauseSparkTranslator.hoistableFilterColumn(conditions))
            .select(col("node_id").as("id"))

          compatibilityDomainDF = restrictVariableDomain(compatibilityDomainDF, variable, allowedIds)
        }

        val matchesDF = MultiJoinMatching.computeMatches(compatibilityDomainDF, edges)

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

        val withLiteralProps = literalNeedsByVariable.foldLeft(matchesDF) { case (df, (variable, propertyTypes)) =>
          val propsDF = PropertyValueLoader.loadNodeProperties(spark, dbName, propertyTypes)
          val renames = propertyTypes.keySet.map(p => p -> s"${variable}__$p").toMap
          joinVariableProperties(df, variable, propsDF, renames)
        }

        val joined = variableNeedsByVariable.foldLeft(withLiteralProps) { case (df, (variable, propertyNames)) =>
          val propsDF = PropertyValueLoader.loadNodePropertiesUntyped(spark, dbName, propertyNames)
          val renames = propertyNames.flatMap(p => Seq(
            s"${p}__string"  -> s"${variable}__${p}__string",
            s"${p}__numeric" -> s"${variable}__${p}__numeric"
          )).toMap
          joinVariableProperties(df, variable, propsDF, renames)
        }

        val literalColumnFor : (String, String) => Column = (variable, prop) => col(s"${variable}__$prop")
        val variableColumnFor: (String, String) => (Column, Column) =
          (variable, prop) => (col(s"${variable}__${prop}__string"), col(s"${variable}__${prop}__numeric"))

        val filtered = WhereClauseSparkTranslator.deferredFilterColumn(whereClause, literalColumnFor, variableColumnFor) match {
          case Some(filterColumn) => joined.filter(filterColumn)
          case None               => joined
        }

        Right(filtered.select(nodeVariables.toSeq.map(col): _*))
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
    baseDF.join(renamed, baseDF(variable) === renamed("node_id"), "left").drop("node_id")
  }
}
