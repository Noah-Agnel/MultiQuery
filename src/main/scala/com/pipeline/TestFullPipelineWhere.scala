package com.pipeline

import com.sparkconfiguration.SparkHandler
import org.apache.spark.sql.DataFrame

/**
 * End-to-end WHERE-clause test against the real local panama_db Iceberg warehouse.
 * Run with: sbt "runMain com.pipeline.TestFullPipelineWhere"
 *
 * Pattern used throughout: (q1:Officer)-[e1:director]->(q2:Entity)<-[e2:secretary]-(q3:Officer)
 * q2.status is a real string property on Entity nodes (values seen: ACTIVE, SUSPENDED, ...).
 */
object TestFullPipelineWhere {

  private val dbName = "panama_db"
  private val basePattern = "MATCH (q1:Officer)-[e1:director]->(q2:Entity)<-[e2:secretary]-(q3:Officer)"

  private var failures = 0

  def main(args: Array[String]): Unit = {
    val spark = SparkHandler.spConfig("Test FullPipeline WHERE")
    spark.conf.set("spark.sql.iceberg.check-nullability", "false")

    val twoHopPattern = "MATCH (q1:Officer)-[e1:director]->(q2:Entity)"
    val twoHopBaseline = runCount(spark, s"$twoHopPattern RETURN q1, q2")
    val twoHopStartsWithA = runCount(spark, s"$twoHopPattern WHERE q2.name STARTS WITH 'A' RETURN q1, q2")
    println(s"2-hop baseline: $twoHopBaseline, q2.name STARTS WITH 'A': $twoHopStartsWithA")
    check("STARTS WITH genuinely narrows the 2-hop baseline (not 0, not everything)",
      twoHopStartsWithA > 0 && twoHopStartsWithA < twoHopBaseline)

    val baseline = runCount(spark, s"$basePattern RETURN q1, q2, q3")
    println(s"baseline (no WHERE): $baseline")

    val active = runCount(spark, s"$basePattern WHERE q2.status = 'ACTIVE' RETURN q1, q2, q3")
    println(s"q2.status = 'ACTIVE': $active")
    check("single literal condition narrows or matches the baseline", active <= baseline)

    val suspended = runCount(spark, s"$basePattern WHERE q2.status = 'SUSPENDED' RETURN q1, q2, q3")
    println(s"q2.status = 'SUSPENDED': $suspended")

    val activeOrSuspended = runCount(spark, s"$basePattern WHERE q2.status = 'ACTIVE' OR q2.status = 'SUSPENDED' RETURN q1, q2, q3")
    println(s"q2.status = 'ACTIVE' OR q2.status = 'SUSPENDED': $activeOrSuspended")
    check("OR of two disjoint conditions equals the sum of each alone",
      activeOrSuspended == active + suspended)
    check("OR result is a superset of either branch alone",
      activeOrSuspended >= active && activeOrSuspended >= suspended)

    val activeAndSuspended = runCount(spark, s"$basePattern WHERE q2.status = 'ACTIVE' AND q2.status = 'SUSPENDED' RETURN q1, q2, q3")
    println(s"q2.status = 'ACTIVE' AND q2.status = 'SUSPENDED' (impossible): $activeAndSuspended")
    check("AND of two mutually-exclusive conditions on the same variable matches nothing",
      activeAndSuspended == 0)

    val crossVar = runCount(spark, s"$basePattern WHERE q1.name = q3.name RETURN q1, q2, q3")
    println(s"q1.name = q3.name (cross-variable): $crossVar")
    check("cross-variable condition narrows or matches the baseline", crossVar <= baseline)

    val mixedOr = runCount(spark, s"$basePattern WHERE q2.status = 'ACTIVE' OR q1.name = q3.name RETURN q1, q2, q3")
    println(s"q2.status = 'ACTIVE' OR q1.name = q3.name (mixed literal/cross-variable): $mixedOr")
    check("mixed literal/cross-variable OR is a superset of either branch alone",
      mixedOr >= active && mixedOr >= crossVar)

    spark.stop()

    if (failures == 0) println("\nALL TESTS PASSED")
    else { println(s"\n$failures TEST(S) FAILED"); System.exit(1) }
  }

  private def runCount(spark: org.apache.spark.sql.SparkSession, query: String): Long = {
    FullPipeline.run(spark, dbName, query) match {
      case Right(df: DataFrame) => df.count()
      case Left(err)            => sys.error(s"Query failed: $query\n$err")
    }
  }

  private def check(name: String, condition: Boolean): Unit = {
    if (condition) println(s"[PASS] $name")
    else { println(s"[FAIL] $name"); failures += 1 }
  }
}
