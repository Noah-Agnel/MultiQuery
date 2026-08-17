package com.properties
import  scala.util.Try


/**
 * Integer array property implementation
 * Supports IN, NOT IN, containment, and size operations for integer arrays
 */
case class IntegerArrayProp(
  override val name    : String,
  override val operator: Operator,
  override val value   : Array[Int]
) extends Property[Array[Int]] {

  /**
   * Evaluates this integer array property against a given value
   */
  override def evaluate(actualValue: Any): Boolean = {
    val arrayValue = actualValue match {
      case arr : Array[Int ]      => arr
      case arr : Array[Long]      => arr.map(_.toInt)
      case arr : Array[Double]    => arr.map(_.toInt)
      case arr : Array[Float]     => arr.map(_.toInt)
      case arr : Array[String]    => arr.map(s => Try(s.toInt).getOrElse(throw new IllegalArgumentException(s"Cannot convert $s to Int"))).toArray

      case coll: Iterable[_]      => convertIterableToArray(coll)
            
      case num : Int              => Array(num)
      case num : Long             => Array(num.toInt)
      case num : Double           => Array(num.toInt)
      case num : Float            => Array(num.toInt)
      case num : String           => Array(num.toInt)

      case null                   => null
      case _                      => throw new IllegalArgumentException(s"Cannot convert ${actualValue.getClass} to Int")
    }
    
    operator match {
      case Operator.IsNull        => actualValue == null
      case Operator.IsNotNull     => actualValue != null

      // A null property value makes every other comparison unknown/not-a-match
      // (matches SQL/Cypher three-valued NULL semantics), rather than NPE-ing.
      case _ if arrayValue == null => false

      case Operator.Equal         => arrayValue.sameElements(value)
      case Operator.NotEqual      => !arrayValue.sameElements(value)

      // nonEmpty-guarded: arrayValue.forall(...) is vacuously true on an empty array (e.g. a
      // node with no recorded value for this property), which would otherwise make In match
      // -- and NotIn not match -- every such node regardless of what's being searched for.
      case Operator.In            => arrayValue.nonEmpty && arrayValue.forall(elem => value.contains(elem))
      case Operator.NotIn         => arrayValue.nonEmpty && !arrayValue.forall(elem => value.contains(elem))
      case Operator.SomeIn        => arrayValue.exists(elem => value.contains(elem))
      case Operator.Contains      => value.forall(elem => arrayValue.contains(elem))
      case Operator.NotContains   => !value.forall(elem => arrayValue.contains(elem))

      case Operator.ArrayEmpty    => arrayValue.isEmpty
      case Operator.ArrayNotEmpty => arrayValue.nonEmpty

      case _ => false
    }
  }

  private def convertIterableToArray(iterable: Iterable[_]): Array[Int] = {
    iterable.map {
      case d: Double => d.toInt
      case f: Float  => f.toInt
      case i: Int    => i
      case l: Long   => l.toInt
      case s: String => s.toInt
      case other     => throw new IllegalArgumentException(s"Cannot convert ${other.getClass} to Int")
    }.toArray
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
      null.asInstanceOf[Array[Int]]
    )
  
  def isNotEmpty(name: String): IntegerArrayProp = 
    IntegerArrayProp(
      name, 
      Operator.ArrayNotEmpty, 
      null.asInstanceOf[Array[Int]]
    )
  
  def isNull(name: String): IntegerArrayProp = 
    IntegerArrayProp(
      name, 
      Operator.IsNull, 
      null.asInstanceOf[Array[Int]]
    )
  
  def isNotNull(name: String): IntegerArrayProp = 
    IntegerArrayProp(
      name, 
      Operator.IsNotNull, 
      null.asInstanceOf[Array[Int]]
    )
} 