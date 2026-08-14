package com.properties

/**
 * An atomic WHERE condition: either a comparison of one variable's property
 * against a literal, or a comparison between two variables' properties.
 */
sealed trait Condition

/**
 * A single-variable condition, e.g. n.age < 25
 */
case class LiteralCondition(variable: String, prop: Property[_]) extends Condition

/**
 * A cross-variable condition, e.g. n.age > m.age
 */
case class VariableCondition(
  leftVar   : String,
  leftProp  : String,
  operator  : Operator,
  rightVar  : String,
  rightProp : String
) extends Condition
