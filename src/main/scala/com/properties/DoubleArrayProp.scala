package com.properties
import  scala.util.Try

/**
 * Double array property implementation
 * Supports IN, NOT IN, containment, and size operations for double arrays
 */
case class DoubleArrayProp(
  override val name    : String,
  override val operator: Operator,
  override val value   : Array[Double]
) extends Property[Array[Double]] {

  /**
   * Evaluates this double array property against a given value
   */
  override def evaluate(actualValue: Any): Boolean = {
    val arrayValue = actualValue match {
      case arr : Array[Double]    => arr
      case arr : Array[Float ]    => arr.map(_.toDouble)
      case arr : Array[Int   ]    => arr.map(_.toDouble)
      case arr : Array[Long  ]    => arr.map(_.toDouble)
      case arr : Array[String]    => arr.map(s => Try(s.toDouble).getOrElse(throw new IllegalArgumentException(s"Cannot convert $s to Double"))).toArray

      case coll: Iterable[_]      => convertIterableToDoubleArray(coll)
      
      case num : Double           => Array(num)
      case num : Float            => Array(num.toDouble)
      case num : Int              => Array(num.toDouble)
      case num : Long             => Array(num.toDouble)
      case num : String           => Array(num.toDouble)
      case null                   => null
      case _                      => throw new IllegalArgumentException(s"Cannot convert ${actualValue.getClass} to Double")
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
      
      case _                      => false
    }   
  }

  private def convertIterableToDoubleArray(iterable: Iterable[_]): Array[Double] = {
    iterable.map {
      case d: Double => d
      case f: Float  => f.toDouble
      case i: Int    => i.toDouble
      case l: Long   => l.toDouble
      case s: String => s.toDouble
      case other => throw new IllegalArgumentException(s"Cannot convert ${other.getClass} to Double")
    }.toArray
  }

  override def toString: String = s"$name ${operator.symbol} [${value.mkString(", ")}]"
}

/**
 * Companion object for DoubleArrayProp
*/
object DoubleArrayProp {
  // Convenience methods for common operators
  def equal(name: String, value: Array[Double]): DoubleArrayProp = 
    DoubleArrayProp(
      name, 
      Operator.Equal, 
      value
    )
  
  def notEqual(name: String, value: Array[Double]): DoubleArrayProp = 
    DoubleArrayProp(
      name, 
      Operator.NotEqual,
      value
    )
  
  def In(name: String, values: Array[Double]): DoubleArrayProp = 
    DoubleArrayProp(
      name, 
      Operator.In,
      values
    )

  def notIn(name: String, values: Array[Double]): DoubleArrayProp = 
    DoubleArrayProp(
      name, 
      Operator.NotIn,
      values
    )
  
  def someIn(name: String, values: Array[Double]): DoubleArrayProp = 
    DoubleArrayProp(
      name,
      Operator.SomeIn,
      values
    )

  def contains(name: String, values: Array[Double]): DoubleArrayProp = 
    DoubleArrayProp(
      name,
      Operator.Contains,
      values
    )

  def notContains(name: String, values: Array[Double]): DoubleArrayProp = 
    DoubleArrayProp(
      name,
      Operator.NotContains,
      values
    )
  
  def isEmpty(name: String): DoubleArrayProp = 
    DoubleArrayProp(
      name, 
      Operator.ArrayEmpty,
      null.asInstanceOf[Array[Double]]
    )
  
  def isNotEmpty(name: String): DoubleArrayProp = 
    DoubleArrayProp(
      name, 
      Operator.ArrayNotEmpty, 
      null.asInstanceOf[Array[Double]]
    )
  
  def isNull(name: String): DoubleArrayProp = 
    DoubleArrayProp(
      name, 
      Operator.IsNull, 
      null.asInstanceOf[Array[Double]]
    )
  
  def isNotNull(name: String): DoubleArrayProp = 
    DoubleArrayProp(
      name, 
      Operator.IsNotNull,
      null.asInstanceOf[Array[Double]]
    )
} 