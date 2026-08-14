package com.matching

import com.sparkconfiguration.SparkHandler
import com.sparkmultigraph.bitmatrix.CompatibilityDomainEngine
import com.query.{QueryNode, QueryEdge, QueryStructure}
import org.apache.spark.sql.functions._

object TestMultiJoinMatching {
  def main(args: Array[String]): Unit = {
    val dbName = if (args.length > 0) args(0) else "panama_db"

    println(s"=== Starting MultiJoin Matching Test on $dbName ===")
    val spark = SparkHandler.spConfig("Test MultiJoin Matching")

    // IMPORTANT: Enable $ interpolators and Encoders from Spark
    import spark.implicits._

    // Disable nullability check for reading/filtering compatibility
    spark.conf.set("spark.sql.iceberg.check-nullability", "false")

    // -----------------------------------------------------------------------
    // 1. Build Sample Query Pattern
    // -----------------------------------------------------------------------
    // Pattern: (q1: Officer) -[e1: director]-> (q2: Entity) <-[e2: secretary]- (q3: Officer)
    println("\n--- Constructing Sample Query Graph Structure ---")

    val q1Node = new QueryNode("q1", Array("Officer"))
    val q2Node = new QueryNode("q2", Array("Entity"))
    val q3Node = new QueryNode("q3", Array("Officer"))
    val q1q2Edge = new QueryEdge("e1", "q1", "q2", "director")
    val q2q3Edge = new QueryEdge("e2", "q3", "q2", "secretary")

    val queryStructure = new QueryStructure(
      Map("q1" -> q1Node, "q2" -> q2Node, "q3" -> q3Node),
      Map("e1" -> q1q2Edge, "e2" -> q2q3Edge)
    )

    // -----------------------------------------------------------------------
    // 2. Execute BitMatrix Compatibility Engine
    // -----------------------------------------------------------------------
    println("\n--- Executing Compatibility Domain Engine ---")
    val compatibilityDomainDF = CompatibilityDomainEngine.computeCompatibilityDomain(
      spark,
      dbName,
      queryStructure
    )

    println("\n--- Compatibility Domain Summary ---")
    compatibilityDomainDF
      .agg(count("*").as("total_candidate_pairs"))
      .show(truncate = false)

    // -----------------------------------------------------------------------
    // 3. Execute MultiJoin Matching
    // -----------------------------------------------------------------------
    println("\n--- Executing MultiJoin Matching ---")
    val edges = queryStructure.getEdges.values.toSeq
    val matchesDF = MultiJoinMatching.computeMatches(compatibilityDomainDF, edges)

    // -----------------------------------------------------------------------
    // 4. Inspect Results cleanly
    // -----------------------------------------------------------------------
    println("\n--- MultiJoin Matches Summary ---")
    matchesDF
      .agg(count("*").as("total_matches"))
      .show(truncate = false)

    println("\n--- Sample Matches ---")
    matchesDF.show(20, truncate = false)

    spark.stop()
    println("=== Test Completed Successfully ===")
  }
}
