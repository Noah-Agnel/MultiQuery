package com.properties

/**
 * String array property implementation
 * Supports IN, NOT IN, containment, and size operations for string arrays
 */
case class StringArrayProp(
  override val name     : String,
  override val operator : Operator,
  override val value    : Array[String]
) extends Property[Array[String]] {

  /**
   * Evaluates this string array property against a given value
   */
  override def evaluate(actualValue: Any): Boolean = {
    val arrayValue = actualValue match {
      case arr : Array[String]    => arr
      case arr : Array[Int]       => arr.map(_.toString)
      case arr : Array[Long]      => arr.map(_.toString)
      case arr : Array[Double]    => arr.map(_.toString)
      case arr : Array[Float]     => arr.map(_.toString)

      case coll: Iterable[_]      => convertIterableToArray(coll)

      case num: Int               => Array(num.toString)
      case num: Long              => Array(num.toString)
      case num: Double            => Array(num.toString)
      case num: Float             => Array(num.toString)
      case num: String            => Array(num)

      case null                   => null
      case _                      => return false
    }
    
    if (arrayValue == false)
      throw new IllegalArgumentException(s"Cannot convert ${actualValue.getClass} to String")
    
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
      
      case _                      => false
    }
  }

  private def convertIterableToArray(iterable: Iterable[_]): Array[String] = {
    iterable.map {
      case s: String => s
      case i: Int    => i.toString
      case l: Long   => l.toString
      case d: Double => d.toString
      case f: Float  => f.toString
      case other     => throw new IllegalArgumentException(s"Cannot convert ${other.getClass} to String")
    }.toArray
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
      null.asInstanceOf[Array[String]]
    )
  
  def isNotEmpty(name: String): StringArrayProp = 
    StringArrayProp(
      name,
      Operator.ArrayNotEmpty,
      null.asInstanceOf[Array[String]]
    )
  
  def isNull(name: String): StringArrayProp = 
    StringArrayProp(
      name,
      Operator.IsNull,
      null.asInstanceOf[Array[String]]
    )
  
  def isNotNull(name: String): StringArrayProp = 
    StringArrayProp(
      name,
      Operator.IsNotNull,
      null.asInstanceOf[Array[String]]
    )
} 