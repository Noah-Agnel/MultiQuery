package com.sparkmultigraph.bitmatrix

import com.query.QueryStructure
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._

object CompatibilityDomainEngine {

  def computeCompatibilityDomain(
      spark: SparkSession,
      dbName: String,
      queryStructure: QueryStructure
  ): DataFrame = {

    // 1. Load config from metadata tables
    val config = BitMatrixConfig.loadFromMetadata(spark, dbName)

    // 2. Build Bit Matrices
    val targetMatrix = BitMatrixPopulator.buildTargetMatrix(spark, dbName, config)
    val queryMatrix  = BitMatrixPopulator.buildQueryMatrix(spark, queryStructure, config)

    // 3. Bitwise AND UDF: Check if target fulfills query requirements
    val isCompatibleUDF = udf((targetBits: Seq[Long], queryBits: Seq[Long]) => {
      var matchOk = true
      var i = 0
      while (i < queryBits.length && matchOk) {
        val q = queryBits(i)
        val t = if (i < targetBits.length) targetBits(i) else 0L
        if ((t & q) != q) matchOk = false
        i += 1
      }
      matchOk
    })

    // 4. Match Query Pairs against Target Pairs
    val matchingPairs = targetMatrix
      .crossJoin(broadcast(queryMatrix))
      .filter(isCompatibleUDF(col("target_bitmask"), col("query_bitmask")))
      .select("q1", "q2", "pair_id")

    // 5. Join to get target node IDs, flattened for downstream multijoin
    matchingPairs
      .join(spark.table(s"$dbName.node_pair_matches"), "pair_id")
      .select(
        col("q1"),
        col("q2"),
        col("source_node_id").as("t1"),
        col("target_node_id").as("t2")
      )
      .distinct()
  }
}