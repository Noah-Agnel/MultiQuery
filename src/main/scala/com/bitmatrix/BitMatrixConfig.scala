package com.sparkmultigraph.bitmatrix

import org.apache.spark.sql.SparkSession
import java.util.BitSet

case class BitMatrixConfig(
    nodeLabelToIdx: Map[String, Int],
    edgeTypeToIdx: Map[String, Int]
) extends Serializable {

  val numNodeLabels: Int = nodeLabelToIdx.size
  val numEdgeTypes: Int  = edgeTypeToIdx.size

  // Offsets layout: [srcLabels | inEdges | outEdges | dstLabels]
  val srcLabelOffset: Int = 0
  val inEdgeOffset: Int   = numNodeLabels
  val outEdgeOffset: Int  = numNodeLabels + numEdgeTypes
  val dstLabelOffset: Int = numNodeLabels + (2 * numEdgeTypes)
  val totalBitSize: Int   = (2 * numNodeLabels) + (2 * numEdgeTypes)

  def createBitSet(
      srcLabels: Seq[String],
      dstLabels: Seq[String],
      outEdges: Seq[String] = Seq.empty,
      inEdges: Seq[String]  = Seq.empty
  ): BitSet = {
    val bs = new BitSet(totalBitSize)

    if (srcLabels != null) srcLabels.foreach(lbl => nodeLabelToIdx.get(lbl).foreach(i => bs.set(srcLabelOffset + i)))
    if (inEdges != null)   inEdges.foreach(e => edgeTypeToIdx.get(e).foreach(i => bs.set(inEdgeOffset + i)))
    if (outEdges != null)  outEdges.foreach(e => edgeTypeToIdx.get(e).foreach(i => bs.set(outEdgeOffset + i)))
    if (dstLabels != null) dstLabels.foreach(lbl => nodeLabelToIdx.get(lbl).foreach(i => bs.set(dstLabelOffset + i)))

    bs
  }
}

object BitMatrixConfig {
  def loadFromMetadata(spark: SparkSession, dbName: String): BitMatrixConfig = {
    import spark.implicits._

    // Instantly load indexing from metadata tables
    val nodeLabels = spark.table(s"$dbName.metadata_node_labels")
      .select("label", "label_id").as[(String, Int)].collect().toMap

    val edgeTypes = spark.table(s"$dbName.metadata_edge_types")
      .select("edge_type", "type_id").as[(String, Int)].collect().toMap

    BitMatrixConfig(nodeLabels, edgeTypes)
  }
}