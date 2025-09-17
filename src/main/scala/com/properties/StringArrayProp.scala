package com.properties

/**
 * String array property implementation
 * Supports IN, NOT IN, containment, and size operations for string arrays
 */
case class StringArrayProp(
  override val name: String,
  override val operator: Operator,
  override val value: Array[String]
) extends Property[Array[String]] {

  /**
   * Evaluates this string array property against a given value
   */
  override def evaluate(actualValue: Any): Boolean = {
    val arrayValue = actualValue match {
      case arr : Array[String] => arr
      case list: List[String]  => list.toArray
      case seq : Seq[String]   => seq.toArray
      case _ => return false
    }
    
    operator match {
      case Operator.Equal         => arrayValue.sameElements(value)
      case Operator.NotEqual      => !arrayValue.sameElements(value)
        
      case Operator.In            => arrayValue.forall(elem => value.contains(elem))
      case Operator.NotIn         => !arrayValue.forall(elem => value.contains(elem))
      case Operator.SomeIn        => arrayValue.exists(elem => value.contains(elem))
      case Operator.Contains      => value.forall(elem => arrayValue.contains(elem))
      case Operator.NotContains   => !value.forall(elem => arrayValue.contains(elem))
        
      case Operator.ArrayEmpty    => arrayValue.isEmpty 
      case Operator.ArrayNotEmpty => arrayValue.nonEmpty
        
      case Operator.IsNull        => actualValue == null
      case Operator.IsNotNull     => actualValue != null
      
      case _ => false
    }
  }

  override def toString: String = s"$name ${operator.symbol} [${value.mkString(", ")}]"
}

/**
 * Companion object for StringArrayProp
 */
object StringArrayProp {
  // Convenience methods for common operators
  def equal(name: String, value: Array[String]): StringArrayProp = 
    StringArrayProp(
      name, 
      Operator.Equal, 
      value
    )
  
  def notEqual(name: String, value: Array[String]): StringArrayProp = 
    StringArrayProp(
      name,
      Operator.NotEqual,
      value
    )
  
  def In(name: String, values: Array[String]): StringArrayProp = 
    StringArrayProp(
      name, 
      Operator.In, 
      values
    )

  def notIn(name: String, values: Array[String]): StringArrayProp = 
    StringArrayProp(
      name, 
      Operator.NotIn, 
      values
    )
  
  def someIn(name: String, values: Array[String]): StringArrayProp = 
    StringArrayProp(
      name,
      Operator.SomeIn,
      values
    )
  
  def contains(name: String, values: Array[String]): StringArrayProp = 
    StringArrayProp(
      name,
      Operator.Contains,
      values
    )

  def notContains(name: String, values: Array[String]): StringArrayProp = 
    StringArrayProp(
      name,
      Operator.NotContains,
      values
    )
  
  def isEmpty(name: String): StringArrayProp = 
    StringArrayProp(
      name,
      Operator.ArrayEmpty,
      null
    )
  
  def isNotEmpty(name: String): StringArrayProp = 
    StringArrayProp(
      name,
      Operator.ArrayNotEmpty,
      null
    )
  
  def isNull(name: String): StringArrayProp = 
    StringArrayProp(
      name,
      Operator.IsNull,
      null
    )
  
  def isNotNull(name: String): StringArrayProp = 
    StringArrayProp(
      name,
      Operator.IsNotNull,
      null
    )
} 