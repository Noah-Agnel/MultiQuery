package com.properties
import  scala.util.Try

/**
 * Integer property implementation
 * Supports all comparison operators for integer values
 */
case class IntegerProp(
  override val name     : String,
  override val operator : Operator,
  override val value    : Int
) extends Property[Int] {

  /**
   * Evaluates this integer property against a given value
   */
  override def evaluate(actualValue: Any): Boolean = {
    val intValue = actualValue match {
      case i: Int    => i
      case l: Long   => l.toInt
      case d: Double => d.toInt
      case f: Float  => f.toInt
      case s: String => Try(s.toInt).getOrElse(return false)
      case _ => return false
    }

    if (intValue == false)
      throw new IllegalArgumentException(s"Cannot convert ${actualValue.getClass} to Int")
    
    operator match {
      case Operator.Equal              => intValue == value
      case Operator.NotEqual           => intValue != value
      case Operator.GreaterThan        => intValue > value
      case Operator.GreaterThanOrEqual => intValue >= value
      case Operator.LessThan           => intValue < value
      case Operator.LessThanOrEqual    => intValue <= value
      case Operator.IsNull             => actualValue == null
      case Operator.IsNotNull          => actualValue != null
      case _ => false
    }
  }

  override def toString: String = s"$name ${operator.symbol} $value"
}

/**
 * Companion object for IntegerProp
 */
object IntegerProp {
  
  // Convenience methods for common operators
  def equal(name: String, value: Int): IntegerProp = 
    IntegerProp(
        name     = name, 
        operator = Operator.Equal, 
        value    = value
    )
  
  def notEqual(name: String, value: Int): IntegerProp = 
    IntegerProp(
        name     = name, 
        operator = Operator.NotEqual,
        value    = value
    )
  
  def greaterThan(name: String, value: Int): IntegerProp = 
    IntegerProp(
        name     = name, 
        operator = Operator.GreaterThan, 
        value    = value
    )
  
  def greaterThanOrEqual(name: String, value: Int): IntegerProp = 
    IntegerProp(
        name     = name, 
        operator = Operator.GreaterThanOrEqual, 
        value    = value
    )
  
  def lessThan(name: String, value: Int): IntegerProp = 
    IntegerProp(
        name     = name, 
        operator = Operator.LessThan,
        value    = value
    )
  
  def lessThanOrEqual(name: String, value: Int): IntegerProp = 
    IntegerProp(
        name     = name, 
        operator = Operator.LessThanOrEqual, 
        value    = value
    )
  
  def isNull(name: String): IntegerProp = 
    IntegerProp(
        name     = name, 
        operator = Operator.IsNull, 
        value    = null.asInstanceOf[Int]
    )
  
  def isNotNull(name: String): IntegerProp = 
    IntegerProp(
        name     = name, 
        operator = Operator.IsNotNull, 
        value    = null.asInstanceOf[Int]
    )
} 