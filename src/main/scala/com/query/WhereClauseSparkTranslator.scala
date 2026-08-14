package com.query

import dnf.{BooleanExpression, Variable => BoolVariable, Constant, Negation, Conjunction, Disjunction}
import com.properties.{Condition, LiteralCondition, VariableCondition, Operator}
import org.apache.spark.sql.Column
import org.apache.spark.sql.functions.{col, lit, udf}

/**
 * Translates WhereClause conditions into Spark Columns, reusing Property.evaluate for
 * the actual comparison semantics rather than re-implementing per-operator SQL logic.
 */
object WhereClauseSparkTranslator {

  /**
   * A single variable's own hoistable conditions, ANDed together, for filtering that
   * variable's property DataFrame (columns named by property, see PropertyValueLoader)
   * before any join.
   */
  def hoistableFilterColumn(conditions: Seq[LiteralCondition]): Column = {
    require(conditions.nonEmpty, "Must have at least one condition to build a filter")
    conditions.map(lc => literalConditionColumn(lc, col(lc.prop.name))).reduce(_ && _)
  }

  /**
   * The whole WHERE expression, translated into one Spark Column, for filtering rows
   * that already have every referenced variable's properties joined in.
   *
   * @param literalColumnFor  resolves a (variable, property) pair used in a LiteralCondition
   *                          to the Column holding its (statically typed) value.
   * @param variableColumnFor resolves a (variable, property) pair used in a VariableCondition
   *                          to a (stringValue, numericValue) Column pair - cross-variable
   *                          conditions carry no declared type, so both are loaded and
   *                          whichever side actually has a value is used, see PropertyValueLoader.
   */
  def deferredFilterColumn(
      whereClause      : WhereClause,
      literalColumnFor : (String, String) => Column,
      variableColumnFor: (String, String) => (Column, Column)
  ): Option[Column] = {
    whereClause.getExpression.map { expr =>
      val letterColumns = whereClause.getConditions.map { case (letter, condition) =>
        letter -> conditionColumn(condition, literalColumnFor, variableColumnFor)
      }
      toColumn(expr, letterColumns)
    }
  }

  private def conditionColumn(
      condition        : Condition,
      literalColumnFor : (String, String) => Column,
      variableColumnFor: (String, String) => (Column, Column)
  ): Column = condition match {
    case lc: LiteralCondition =>
      literalConditionColumn(lc, literalColumnFor(lc.variable, lc.prop.name))

    case VariableCondition(leftVar, leftProp, operator, rightVar, rightProp) =>
      val (leftStrCol, leftNumCol)   = variableColumnFor(leftVar, leftProp)
      val (rightStrCol, rightNumCol) = variableColumnFor(rightVar, rightProp)
      val compareUDF = udf((ls: String, ln: java.lang.Double, rs: String, rn: java.lang.Double) =>
        compareValues(ls, ln, rs, rn, operator))
      compareUDF(leftStrCol, leftNumCol, rightStrCol, rightNumCol)
  }

  private def literalConditionColumn(condition: LiteralCondition, valueColumn: Column): Column = {
    val prop = condition.prop
    val evalUDF = udf((value: Any) => prop.evaluate(value))
    evalUDF(valueColumn)
  }

  private def toColumn(expr: BooleanExpression, letterColumns: Map[String, Column]): Column = expr match {
    case BoolVariable(name)       => letterColumns(name)
    case Constant(value)          => lit(value)
    case Negation(operand)        => !toColumn(operand, letterColumns)
    case Conjunction(left, right) => toColumn(left, letterColumns) && toColumn(right, letterColumns)
    case Disjunction(left, right) => toColumn(left, letterColumns) || toColumn(right, letterColumns)
  }

  /**
   * Generic comparator for cross-variable conditions (n1.prop OP n2.prop). Since
   * VariableCondition carries no declared type, both sides are loaded as a
   * (string, numeric) pair (see PropertyValueLoader.loadNodePropertiesUntyped) and
   * compared numerically if both sides have a numeric value, else as strings if both
   * sides have a string value, mirroring the semantics IntegerProp/DoubleProp/StringProp
   * already implement for a fixed literal.
   */
  private def compareValues(
      leftString : String,
      leftNumeric: java.lang.Double,
      rightString: String,
      rightNumeric: java.lang.Double,
      operator   : Operator
  ): Boolean = {
    if (leftNumeric != null && rightNumeric != null) {
      val lv = leftNumeric.doubleValue()
      val rv = rightNumeric.doubleValue()
      operator match {
        case Operator.Equal              => lv == rv
        case Operator.NotEqual           => lv != rv
        case Operator.GreaterThan        => lv > rv
        case Operator.GreaterThanOrEqual => lv >= rv
        case Operator.LessThan           => lv < rv
        case Operator.LessThanOrEqual    => lv <= rv
        case _                           => false
      }
    } else if (leftString != null && rightString != null) {
      operator match {
        case Operator.Equal              => leftString == rightString
        case Operator.NotEqual           => leftString != rightString
        case Operator.GreaterThan        => leftString > rightString
        case Operator.GreaterThanOrEqual => leftString >= rightString
        case Operator.LessThan           => leftString < rightString
        case Operator.LessThanOrEqual    => leftString <= rightString
        case _                           => false
      }
    } else {
      val leftIsNull = leftNumeric == null && leftString == null
      operator match {
        case Operator.IsNull    => leftIsNull
        case Operator.IsNotNull => !leftIsNull
        case _                  => false
      }
    }
  }
}
