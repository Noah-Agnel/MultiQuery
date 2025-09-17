package com.properties
import  scala.util.Try


/**
 * Integer array property implementation
 * Supports IN, NOT IN, containment, and size operations for integer arrays
 */
case class IntegerArrayProp(
  override val name: String,
  override val operator: Operator,
  override val value: Array[Int]
) extends Property[Array[Int]] {

  /**
   * Evaluates this integer array property against a given value
   */
  override def evaluate(actualValue: Any): Boolean = {
    val arrayValue = actualValue match {
      case arr : Array[Int ] => arr
      case arr : Array[Long] => arr.map(_.toInt)
      case list: List[Int  ] => list.toArray
      case list: List[Long ] => list.map(_.toInt).toArray
      case seq : Seq[Int   ] => seq.toArray
      case seq : Seq[Long  ] => seq.map(_.toInt).toArray
      case num : Int         => Array(num)
      case num : Long        => Array(num.toInt)
      case num : String      => Array(num.toInt)
      case null => null
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
 * Companion object for IntegerArrayProp
 */
object IntegerArrayProp {
  
  // Convenience methods for common operators
  def equal(name: String, value: Array[Int]): IntegerArrayProp = 
    IntegerArrayProp(
      name, 
      Operator.Equal, 
      value
    )
  
  def notEqual(name: String, value: Array[Int]): IntegerArrayProp = 
    IntegerArrayProp(
      name,
      Operator.NotEqual,
      value
    )
  
  def In(name: String, values: Array[Int]): IntegerArrayProp = 
    IntegerArrayProp(
      name, 
      Operator.In,
      values
    )
  
  def notIn(name: String, values: Array[Int]): IntegerArrayProp = 
    IntegerArrayProp(
      name, 
      Operator.NotIn,
      values
    )
  
  def someIn(name: String, values: Array[Int]): IntegerArrayProp = 
    IntegerArrayProp(
      name, 
      Operator.SomeIn, 
      values
    )

  def contains(name: String, values: Array[Int]): IntegerArrayProp = 
    IntegerArrayProp(
      name,
      Operator.Contains,
      values
    )
  
  def notContains(name: String, values: Array[Int]): IntegerArrayProp = 
    IntegerArrayProp(
      name,
      Operator.NotContains,
      values
    )
  
  def isEmpty(name: String): IntegerArrayProp = 
    IntegerArrayProp(
      name, 
      Operator.ArrayEmpty, 
      null
    )
  
  def isNotEmpty(name: String): IntegerArrayProp = 
    IntegerArrayProp(
      name, 
      Operator.ArrayNotEmpty, 
      null
    )
  
  def isNull(name: String): IntegerArrayProp = 
    IntegerArrayProp(
      name, 
      Operator.IsNull, 
      null
    )
  
  def isNotNull(name: String): IntegerArrayProp = 
    IntegerArrayProp(
      name, 
      Operator.IsNotNull, 
      null
    )
} 