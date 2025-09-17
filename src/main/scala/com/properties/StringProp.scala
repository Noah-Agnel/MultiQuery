package com.properties

/**
 * String property implementation
 * Supports equality, inequality, null checks, and pattern matching for string values
 */
case class StringProp(
  override val name    : String,
  override val operator: Operator,
  override val value   : String
) extends Property[String] {

  /**
   * Evaluates this string property against a given value
   */
  override def evaluate(actualValue: Any): Boolean = {
    val stringValue = actualValue match {     
      case null      => null
      case s: String => s
      case i: Int    => i.toString
      case l: Long   => l.toString
      case d: Double => d.toString
      case f: Float  => f.toString
      case other     => return false
    }

    if (stringValue == false)
      throw new IllegalArgumentException(s"Cannot convert ${actualValue.getClass} to String")
    
    operator match {
      case Operator.Equal              => stringValue == value
      case Operator.NotEqual           => stringValue != value
        
      case Operator.IsNull             => stringValue == null
      case Operator.IsNotNull          => stringValue != null
      
      case Operator.GreaterThan        => stringValue > value 
      case Operator.GreaterThanOrEqual => stringValue >= value
        
      case Operator.LessThan           => stringValue < value
      case Operator.LessThanOrEqual    => stringValue <= value
        
      case Operator.Contains           => stringValue.contains(value)
      case Operator.StartsWith         => stringValue.startsWith(value)
      case Operator.EndsWith           => stringValue.endsWith(value)
      
      case Operator.In                 => value.contains(stringValue)
      case Operator.NotIn              => !value.contains(stringValue)
        
      case _ => false
    }
  } 

  override def toString: String = s"$name ${operator.symbol} '$value'"
}
     

/**
 * Companion object for StringProp
 */
object StringProp {
  
  // Convenience methods for common operators
  def equal(name: String, value: String): StringProp = 
    StringProp(
        name     = name, 
        operator = Operator.Equal, 
        value    = value
    )
  
  def notEqual(name: String, value: String): StringProp = 
    StringProp(
        name     = name, 
        operator = Operator.NotEqual, 
        value    = value
    )
  
  def greaterThan(name: String, value: String): StringProp = 
    StringProp(
        name     = name, 
        operator = Operator.GreaterThan, 
        value    = value
    )
  
  def greaterThanOrEqual(name: String, value: String): StringProp = 
    StringProp(
        name     = name, 
        operator = Operator.GreaterThanOrEqual, 
        value    = value
    )
  
  def lessThan(name: String, value: String): StringProp = 
    StringProp(
        name     = name, 
        operator = Operator.LessThan, 
        value    = value
    )
  
  def lessThanOrEqual(name: String, value: String): StringProp = 
    StringProp(
        name     = name, 
        operator = Operator.LessThanOrEqual, 
        value    = value
    )
  
  def contains(name: String, value: String): StringProp = 
    StringProp(
        name     = name, 
        operator = Operator.Contains, 
        value    = value
    )
  
  def startsWith(name: String, value: String): StringProp = 
    StringProp(
        name     = name, 
        operator = Operator.StartsWith, 
        value    = value
    )
  
  def endsWith(name: String, value: String): StringProp = 
    StringProp(
        name     = name, 
        operator = Operator.EndsWith, 
        value    = value
    )
  
  def isNull(name: String): StringProp = 
    StringProp(
        name     = name, 
        operator = Operator.IsNull, 
        value    = null.asInstanceOf[String]
    )
  
  def isNotNull(name: String): StringProp = 
    StringProp(
        name     = name, 
        operator = Operator.IsNotNull, 
        value    = null.asInstanceOf[String]
    ) 
}