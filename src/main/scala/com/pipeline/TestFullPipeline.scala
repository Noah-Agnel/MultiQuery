package com.pipeline

import com.sparkconfiguration.SparkHandler

/**
 * Runs a single Cypher query against panama_db multiple times back to back and
 * reports timing stats (total wall clock + per-stage breakdown) across the runs.
 * Run with: sbt "runMain com.pipeline.TestFullPipeline [numRuns]"
 */
object TestFullPipeline {

  private val dbName = "panama_db"

  private val query =
    "MATCH (q1:Officer)-[e1:`director of`]->(q2:Entity)<-[e2:`shareholder of`]-(q3:Officer), " +
    "(q2)-[e3:`registered address`]->(q4:Address) " +
    "WHERE q1.name <> q3.name AND (q2.countries CONTAINS 'France' OR q4.countries CONTAINS 'France') " +
    "RETURN q1, q2, q3, q4"

  /*
  private val query =
    "MATCH (q1:Officer)-[e1:`director of`]->(q2:Entity)<-[e2:`shareholder of`]-(q3:Officer), " +
    "(q2)-[e3:`registered address`]->(q4:Address) " +
    "RETURN q1, q2, q3, q4"
  */

  def main(args: Array[String]): Unit = {
    val numRuns = if (args.nonEmpty) args(0).toInt else 5

    val spark = SparkHandler.spConfig("Test FullPipeline Perf")
    spark.conf.set("spark.sql.iceberg.check-nullability", "false")

    val runResults = (1 to numRuns).map { i =>
      println(s"\n=== Run $i/$numRuns ===")
      val start                     = System.nanoTime()
      val (result, stageTimings)    = FullPipeline.runWithTimings(spark, dbName, query)
      val matchCount = result match {
        case Right((_, count)) => count
        case Left(err)          => sys.error(s"Run $i failed: $err")
      }
      val totalMs = (System.nanoTime() - start) / 1e6
      println(f"Run $i total: $totalMs%.2f ms, matches: $matchCount")
      (totalMs, matchCount, stageTimings)
    }

    printRecap(runResults)

    spark.stop()
  }

  private def printRecap(runResults: Seq[(Double, Long, Seq[(String, Double)])]): Unit = {
    println(s"\n\n=== Recap across ${runResults.size} run(s) ===")

    println("\n-- Total time per run --")
    runResults.zipWithIndex.foreach { case ((totalMs, matches, _), idx) =>
      val label = if (idx == 0) s"Run ${idx + 1} (cold)" else s"Run ${idx + 1}"
      println(f"${label.padTo(14, ' ')}: $totalMs%10.2f ms   (matches: $matches)")
    }

    // The first run pays one-time JVM/Spark warm-up costs (codegen, catalog init, ...) that
    // don't recur, so it's excluded from the averaged stats below to keep them representative
    // of steady-state performance. It's still shown above so cold-start cost stays visible.
    val warmRuns = runResults.drop(1)
    if (warmRuns.isEmpty) {
      println("(only one run - no warm runs to average)")
      return
    }

    val totals = warmRuns.map(_._1)
    println(s"\n-- Stats across ${warmRuns.size} warm run(s) (run 1 excluded) --")
    println(f"avg: ${totals.sum / totals.size}%10.2f ms   min: ${totals.min}%10.2f ms   max: ${totals.max}%10.2f ms")
  }
}
