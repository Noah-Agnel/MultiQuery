package com.sparkmultigraph.bitmatrix

import com.query.QueryStructure
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._

object BitMatrixPopulator {

  /** Populates Target Bit Matrix DataFrame */
  def buildTargetMatrix(spark: SparkSession, dbName: String, config: BitMatrixConfig): DataFrame = {
    val bConfig = spark.sparkContext.broadcast(config)

    val buildBitmaskUDF = udf((srcLabels: Seq[String], dstLabels: Seq[String], edgeMap: Map[String, Int]) => {
      val outEdges = if (edgeMap != null) edgeMap.keys.toSeq else Seq.empty[String]
      bConfig.value.createBitSet(srcLabels, dstLabels, outEdges = outEdges).toLongArray
    })

    spark.table(s"$dbName.node_label_pair")
      .select(
        col("pair_id"),
        buildBitmaskUDF(col("src_labels"), col("dst_labels"), col("edge_1_2_types")).as("target_bitmask")
      )
  }

  /** Recomputes the target bit matrix from node_label_pair and persists it to the
    * target_bit_matrix Iceberg table, so queries can read it instead of rebuilding it. */
  def populateTargetBitMatrixTable(spark: SparkSession, dbName: String, config: BitMatrixConfig): Unit = {
    buildTargetMatrix(spark, dbName, config)
      .withColumn("created_at", current_timestamp())
      .write
      .mode("overwrite")
      .insertInto(s"$dbName.target_bit_matrix")
  }

  /** Reads the persisted target bit matrix instead of recomputing it. */
  def readTargetBitMatrix(spark: SparkSession, dbName: String): DataFrame =
    spark.table(s"$dbName.target_bit_matrix").select("pair_id", "target_bitmask")

  /** Populates Query Bit Matrix DataFrame */
  def buildQueryMatrix(spark: SparkSession, queryStructure: QueryStructure, config: BitMatrixConfig): DataFrame = {
    import spark.implicits._

    val nodes = queryStructure.getNodes
    val rows = queryStructure.getEdges.values.map { edge =>
      val q1 = edge.getSrcNodeName
      val q2 = edge.getDstNodeName

      val srcLabels = nodes.get(q1).map(_.getLabels).getOrElse(Array.empty[String]).toSeq
      val dstLabels = nodes.get(q2).map(_.getLabels).getOrElse(Array.empty[String]).toSeq
      
      val bs = config.createBitSet(srcLabels, dstLabels, outEdges = Seq(edge.getType))
      (q1, q2, bs.toLongArray)
    }.toSeq

    rows.toDF("q1", "q2", "query_bitmask")
  }
}