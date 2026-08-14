package com.matching

import com.query.QueryEdge
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._

object MultiJoinMatching {

  /**
   * Joins the per-edge candidate pairs from `compatibilityDomainDF` (columns: q1, q2, t1, t2)
   * into full query solutions, one column per query node bound to its matched target node id.
   *
   * `edges` is taken as-is, unordered: no join-order optimization happens here, it's a
   * later, separate concern layered on top of this.
   */
  def computeMatches(compatibilityDomainDF: DataFrame, edges: Seq[QueryEdge]): DataFrame = {
    require(edges.nonEmpty, "Query must contain at least one edge")

    // This edge's candidate pairs, with t1/t2 renamed to its actual query node names
    def edgeCandidates(edge: QueryEdge): DataFrame = {
      val q1 = edge.getSrcNodeName
      val q2 = edge.getDstNodeName

      compatibilityDomainDF
        .filter(col("q1") === q1 && col("q2") === q2)
        .select(col("t1").as(q1), col("t2").as(q2))
    }

    edges.tail.foldLeft(edgeCandidates(edges.head)) { (partialMatches, edge) =>
      val nextCandidates = edgeCandidates(edge)
      val sharedNodes     = partialMatches.columns.toSet.intersect(nextCandidates.columns.toSet)

      if (sharedNodes.nonEmpty)
        partialMatches.join(nextCandidates, sharedNodes.toSeq.sorted)
      else
        partialMatches.crossJoin(nextCandidates)
    }
  }
}
