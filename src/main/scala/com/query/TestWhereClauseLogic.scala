package com.query

import dnf.{BooleanExpressionParser, DNFTransformer}
import com.properties.{IntegerProp, StringProp, Operator, LiteralCondition, VariableCondition}
import com.query.wrapper.CypherQueryWrapper

/**
 * Pure-logic regression tests for WHERE-clause handling: no Spark, no data - just the
 * DNF/hoistability planner and the Cypher-to-Condition parser. Run with:
 * sbt "runMain com.query.TestWhereClauseLogic"
 */
object TestWhereClauseLogic {

  private var failures = 0

  def main(args: Array[String]): Unit = {
    testHoistSingleVariableAnd()
    testRefuseHoistSingleVariableOr()
    testRefuseHoistMixedLiteralAndVariable()
    testNestedAndInsideOrNoConditionLoss()
    testConverterParsesCrossVariableCondition()
    testConverterParsesIntegerIn()

    if (failures == 0) println("\nALL TESTS PASSED")
    else { println(s"\n$failures TEST(S) FAILED"); System.exit(1) }
  }

  private def check(name: String, condition: Boolean): Unit = {
    if (condition) println(s"[PASS] $name")
    else { println(s"[FAIL] $name"); failures += 1 }
  }

  private def parseDNF(logic: String) =
    DNFTransformer(BooleanExpressionParser.parse(logic).getOrElse(sys.error(s"bad logic: $logic")))

  // A & B, both single-variable on "n" -> both should be hoistable
  private def testHoistSingleVariableAnd(): Unit = {
    val a = LiteralCondition("n", IntegerProp("age", Operator.LessThan, 25))
    val b = LiteralCondition("n", StringProp.equal("name", "Bob"))
    val wc = new WhereClause(parseDNF("A & B"), Map("A" -> a, "B" -> b))

    val plan = WherePredicatePlanner.classify(wc)
    check("AND of two single-variable conditions: both hoistable",
      plan.hoistable.getOrElse("n", Nil).toSet == Set(a, b))
  }

  // A | B, both single-variable on "n" -> neither is required in both disjuncts -> nothing hoistable
  private def testRefuseHoistSingleVariableOr(): Unit = {
    val a = LiteralCondition("n", IntegerProp("age", Operator.LessThan, 25))
    val b = LiteralCondition("n", StringProp.equal("name", "Bob"))
    val wc = new WhereClause(parseDNF("A | B"), Map("A" -> a, "B" -> b))

    val plan = WherePredicatePlanner.classify(wc)
    check("OR of two single-variable conditions: nothing hoistable", plan.hoistable.isEmpty)
  }

  // (n.age < 25) OR (n.age > m.age) - the case that isn't safe to pre-filter, discussed at length
  private def testRefuseHoistMixedLiteralAndVariable(): Unit = {
    val a = LiteralCondition("n", IntegerProp("age", Operator.LessThan, 25))
    val b = VariableCondition("n", "age", Operator.GreaterThan, "m", "age")
    val wc = new WhereClause(parseDNF("A | B"), Map("A" -> a, "B" -> b))

    val plan = WherePredicatePlanner.classify(wc)
    check("(n.age<25) OR (n.age>m.age): nothing hoistable", plan.hoistable.isEmpty)
  }

  // (A AND (B OR C)) OR D - the shape that broke the old project's string-DNF ownership map
  // (a condition duplicated across disjuncts by DNF distribution got silently dropped from
  // all but the last clause it appeared in). Here we just use the DNF BooleanExpression's own
  // evaluate() directly, with no per-clause ownership bookkeeping at all, and check every
  // combination of truth values against the expected result by hand.
  private def testNestedAndInsideOrNoConditionLoss(): Unit = {
    val original = BooleanExpressionParser.parse("(A & (B | C)) | D").getOrElse(sys.error("parse failed"))
    val dnf       = DNFTransformer(original)

    def expected(a: Boolean, b: Boolean, c: Boolean, d: Boolean): Boolean = (a && (b || c)) || d

    val allPassed = (for {
      a <- Seq(true, false)
      b <- Seq(true, false)
      c <- Seq(true, false)
      d <- Seq(true, false)
    } yield {
      val assignment = Map("A" -> a, "B" -> b, "C" -> c, "D" -> d)
      dnf.evaluate(assignment) == expected(a, b, c, d)
    }).forall(identity)

    check("(A AND (B OR C)) OR D: DNF evaluates correctly for all 16 truth assignments", allPassed)
  }

  private def testConverterParsesCrossVariableCondition(): Unit = {
    val result = CypherQueryWrapper.convert(
      "MATCH (n)-[e]->(m) WHERE n.age > m.age RETURN n"
    )
    check("n.age > m.age parses to a VariableCondition", result match {
      case Right(qs) =>
        qs.getWhereClause.exists(_.getConditions.values.exists {
          case VariableCondition("n", "age", Operator.GreaterThan, "m", "age") => true
          case _                                                               => false
        })
      case Left(err) =>
        println(s"  (parse failed: $err)"); false
    })
  }

  private def testConverterParsesIntegerIn(): Unit = {
    val result = CypherQueryWrapper.convert(
      "MATCH (n)-[e]->(m) WHERE n.age IN [25, 30, 35] RETURN n"
    )
    check("n.age IN [25, 30, 35] parses to a LiteralCondition", result match {
      case Right(qs) =>
        qs.getWhereClause.exists(_.getConditions.values.exists {
          case LiteralCondition("n", _) => true
          case _                        => false
        })
      case Left(err) =>
        println(s"  (parse failed: $err)"); false
    })
  }
}
