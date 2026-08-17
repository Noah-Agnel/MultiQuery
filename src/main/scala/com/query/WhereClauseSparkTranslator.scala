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
    conditions.map(lc => literalConditionColumn(lc, col(lc.prop.name), col(s"${lc.prop.name}__array"))).reduce(_ && _)
  }

  /**
   * The whole WHERE expression, translated into one Spark Column, for filtering rows
   * that already have every referenced variable's properties joined in.
   *
   * @param literalColumnFor  resolves a (variable, property) pair used in a LiteralCondition to
   *                          a (declared-type value, array-fallback value) Column pair - see
   *                          literalConditionColumn for why a fallback is needed.
   * @param variableColumnFor resolves a (variable, property) pair used in a VariableCondition
   *                          to a (stringValue, numericValue) Column pair - cross-variable
   *                          conditions carry no declared type, so both are loaded and
   *                          whichever side actually has a value is used, see PropertyValueLoader.
   */
  def deferredFilterColumn(
      whereClause      : WhereClause,
      literalColumnFor : (String, String) => (Column, Column),
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
      literalColumnFor : (String, String) => (Column, Column),
      variableColumnFor: (String, String) => (Column, Column)
  ): Column = condition match {
    case lc: LiteralCondition =>
      val (valueColumn, arrayFallbackColumn) = literalColumnFor(lc.variable, lc.prop.name)
      literalConditionColumn(lc, valueColumn, arrayFallbackColumn)

    case VariableCondition(leftVar, leftProp, operator, rightVar, rightProp) =>
      val (leftStrCol, leftNumCol)   = variableColumnFor(leftVar, leftProp)
      val (rightStrCol, rightNumCol) = variableColumnFor(rightVar, rightProp)
      val compareUDF = udf((ls: String, ln: java.lang.Double, rs: String, rn: java.lang.Double) =>
        compareValues(ls, ln, rs, rn, operator))
      compareUDF(leftStrCol, leftNumCol, rightStrCol, rightNumCol)
  }

  /**
   * A WHERE condition's declared property type comes purely from how the Cypher literal was
   * written (e.g. `n.countries CONTAINS 'France'` always parses to a scalar StringProp, since
   * a quoted string literal looks scalar), which doesn't necessarily match how the property is
   * actually stored -- ICIJ's country fields, for instance, are always arrays even for a single
   * country, so the scalar column is null for every row and the condition would silently never
   * match. `arrayFallbackColumn` is the property's array-shaped Iceberg column (populated
   * whenever the scalar one isn't); when the declared-type value is null, this evaluates the
   * condition against every element of the array instead and matches if any element does
   * (mirroring how a single-valued array is indistinguishable from a scalar).
   */
  private def literalConditionColumn(condition: LiteralCondition, valueColumn: Column, arrayFallbackColumn: Column): Column = {
    val prop = condition.prop
    val evalUDF = udf((value: Any, arrayFallback: Any) => evaluateWithArrayFallback(prop, value, arrayFallback))
    evalUDF(valueColumn, arrayFallbackColumn)
  }

  private def evaluateWithArrayFallback(prop: com.properties.Property[_], value: Any, arrayFallback: Any): Boolean = {
    if (value != null) prop.evaluate(value)
    else arrayFallback match {
      case arr: Seq[_]   => arr.nonEmpty && arr.exists(elem => prop.evaluate(elem))
      case arr: Array[_] => arr.nonEmpty && arr.exists(elem => prop.evaluate(elem))
      case other         => prop.evaluate(other) // null, or (for an already-array-typed prop) the real value
    }
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
