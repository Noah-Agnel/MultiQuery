package com.pipeline

import com.sparkconfiguration.SparkHandler

/**
 * Runs 4 distinct Cypher queries against panama_db, once each, and reports match count +
 * total wall clock per query. Unlike TestFullPipeline (same query repeated, so Spark's cache
 * manager reuses cached DataFrames across runs), each query here has a different logical plan,
 * so every run is a cold computation - useful for seeing, in the event log, what
 * "no cache reuse between runs" actually costs (fresh broadcast-exchange jobs, fresh joins,
 * etc. every time) instead of just the first run.
 *
 * Run with: sbt "runMain com.pipeline.TestFullPipelineMultiQuery"
 */
object TestFullPipelineMultiQuery {

  private val dbName = "panama_db"

  val queries: Seq[(String, String)] = Seq(
    (
      "directors-of-british-virgin-island-entities",
      """
      MATCH (o:Officer)-[:`director of`]->(e:Entity)
      WHERE e.jurisdiction = 'BVI'
      RETURN o.name, e.name, e.jurisdiction
      LIMIT 100
      """
    ),
    (
      "shareholders-of-panama-entities",
      """
      MATCH (o:Officer)-[:`shareholder of`]->(e:Entity)
      WHERE e.address CONTAINS 'Panama'
      RETURN o.name, e.name, e.jurisdiction
      """
    ),
    (
      "russian-beneficiaries",
      """
      MATCH (o:Officer)-[:`beneficiary of`]->(e:Entity)
      WHERE o.countries CONTAINS 'Russia'
      RETURN o.name, o.countries, e.name, e.jurisdiction
      """
    ),
    (
      "intermediaries-in-Samoa",
      """
      MATCH (i:Intermediary)-[:`intermediary of`]->(e:Entity)
      WHERE e.jurisdiction = 'SAM'
      RETURN i.name, e.name, e.jurisdiction
      """
    )
  )

  def main(args: Array[String]): Unit = {
    val spark = SparkHandler.spConfig("Test FullPipeline MultiQuery")
    spark.conf.set("spark.sql.iceberg.check-nullability", "false")

    val results = queries.zipWithIndex.map { case ((label, query), idx) =>
      println(s"\n=== Query ${idx + 1}/${queries.size}: $label ===")
      val start      = System.nanoTime()
      val (result, _) = FullPipeline.runWithTimings(spark, dbName, query)
      val matchCount = result match {
        case Right((_, count)) => count
        case Left(err)         => sys.error(s"Query ${idx + 1} failed: $err")
      }
      val totalMs = (System.nanoTime() - start) / 1e6
      println(f"Query ${idx + 1} total: $totalMs%.2f ms, matches: $matchCount")
      (label, totalMs, matchCount)
    }

    println("\n\n=== Recap across 5 unique queries ===")
    results.foreach { case (label, totalMs, matches) =>
      println(f"${label.padTo(48, ' ')}: $totalMs%10.2f ms   (matches: $matches)")
    }

    spark.stop()
  }
}
