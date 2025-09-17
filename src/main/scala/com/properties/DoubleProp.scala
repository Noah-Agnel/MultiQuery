package com.properties
import  scala.util.Try

/**
 * Double property implementation
 * Supports all comparison operators for double/floating-point values
 */
case class DoubleProp(
  override val name    : String,
  override val operator: Operator,
  override val value   : Double
) extends Property[Double] {

  /**
   * Evaluates this double property against a given value
   */
  override def evaluate(actualValue: Any): Boolean = {
    val doubleValue = actualValue match {
      case d: Double => d
      case f: Float  => f.toDouble
      case i: Int    => i.toDouble
      case l: Long   => l.toDouble
      case s: String => Try(s.toDouble).getOrElse(throw new IllegalArgumentException(s"Cannot convert $s to Double"))
      case _         => throw new IllegalArgumentException(s"Cannot convert ${actualValue.getClass} to Double")
    }
    
    operator match {
      case Operator.Equal              => doubleValue == value
      case Operator.NotEqual           => doubleValue != value
      case Operator.GreaterThan        => doubleValue > value
      case Operator.GreaterThanOrEqual => doubleValue >= value
      case Operator.LessThan           => doubleValue < value
      case Operator.LessThanOrEqual    => doubleValue <= value
      case Operator.IsNull             => actualValue == null
      case Operator.IsNotNull          => actualValue != null
      case _ => false // Other operators not supported for doubles
    }
  }

  override def toString: String = s"$name ${operator.symbol} $value"
}


/**
 * Companion object for DoubleProp
*/
object DoubleProp {
  def equal(name: String, value: Double): DoubleProp = 
    DoubleProp(
        name     = name, 
        operator = Operator.Equal, 
        value    = value
    )
  
  def notEqual(name: String, value: Double): DoubleProp = 
    DoubleProp(
        name     = name, 
        operator = Operator.NotEqual, 
        value    = value
    )
  
  def greaterThan(name: String, value: Double): DoubleProp = 
    DoubleProp(
        name     = name, 
        operator = Operator.GreaterThan, 
        value    = value
    )
  
  def greaterThanOrEqual(name: String, value: Double): DoubleProp = 
    DoubleProp(
        name     = name, 
        operator = Operator.GreaterThanOrEqual, 
        value    = value
    )
  
  def lessThan(name: String, value: Double): DoubleProp = 
    DoubleProp(
        name     = name, 
        operator = Operator.LessThan, 
        value    = value
    )
  
  def lessThanOrEqual(name: String, value: Double): DoubleProp = 
    DoubleProp(
        name     = name, 
        operator = Operator.LessThanOrEqual, 
        value    = value
    )
  
  def isNull(name: String): DoubleProp = 
    DoubleProp(
        name     = name, 
        operator = Operator.IsNull, 
        value    = null.asInstanceOf[Double]
    )
  
  def isNotNull(name: String): DoubleProp = 
    DoubleProp(
        name     = name, 
        operator = Operator.IsNotNull, 
        value    = null.asInstanceOf[Double]
    )
} 