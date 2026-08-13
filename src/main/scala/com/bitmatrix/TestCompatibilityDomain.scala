package com.bitmatrix

import com.sparkconfiguration.SparkHandler
import com.sparkmultigraph.bitmatrix.CompatibilityDomainEngine
import com.query.{QueryNode, QueryEdge, QueryStructure}
import org.apache.spark.sql.functions._

object TestCompatibilityDomain {
  def main(args: Array[String]): Unit = {
    val dbName = if (args.length > 0) args(0) else "panama_db"

    println(s"=== Starting BitMatrix Compatibility Domain Test on $dbName ===")
    val spark = SparkHandler.spConfig("Test Compatibility Domain")

    // IMPORTANT: Enable $ interpolators and Encoders from Spark
    import spark.implicits._

    // Disable nullability check for reading/filtering compatibility
    spark.conf.set("spark.sql.iceberg.check-nullability", "false")

    // -----------------------------------------------------------------------
    // 1. Build Sample Query Pattern
    // -----------------------------------------------------------------------
    // Pattern: (q1: Officer) -[e1: director]-> (q2: Entity)
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

    // -----------------------------------------------------------------------
    // 3. Inspect Results cleanly
    // -----------------------------------------------------------------------
    println("\n--- Compatibility Domain Summary ---")
    
    // Print match counts per query edge
    compatibilityDomainDF
      .agg(count("*").as("total_candidate_pairs"))
      .show(truncate = false)

    println("\n--- Sample Matched Node Pairs ---")
    compatibilityDomainDF
      .select(
        $"q1",
        $"q2",
        $"t1".as("target_src_node"),
        $"t2".as("target_dst_node")
      )
      .show(20, truncate = false)

    spark.stop()
    println("=== Test Completed Successfully ===")
  }
}