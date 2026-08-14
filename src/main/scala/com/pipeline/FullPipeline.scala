package com.pipeline

import com.matching.MultiJoinMatching
import com.query.QueryStructure
import com.query.wrapper.CypherQueryWrapper
import com.sparkconfiguration.SparkHandler
import com.sparkmultigraph.bitmatrix.CompatibilityDomainEngine
import org.apache.spark.sql.{DataFrame, SparkSession}
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

    val compatibilityDomainDF = CompatibilityDomainEngine.computeCompatibilityDomain(spark, dbName, queryStructure)
    Right(MultiJoinMatching.computeMatches(compatibilityDomainDF, edges))
  }
}
