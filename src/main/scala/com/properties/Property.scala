package com.properties

import scala.util.{Try, Success, Failure}

/**
 * Enumeration of supported operators
 */
sealed trait Operator {
  def symbol: String
}

object Operator {
  case object Equal extends Operator {
    val symbol = "="
  }

  case object NotEqual extends Operator {
    val symbol = "!="
  }
  
  case object GreaterThan extends Operator {
    val symbol = ">"
  }
  
  case object GreaterThanOrEqual extends Operator {
    val symbol = ">="
  }  

  case object LessThan extends Operator {
    val symbol = "<"
  }
  
  case object LessThanOrEqual extends Operator {
    val symbol = "<="
  }
    
  case object In extends Operator {
    val symbol = "IN"
  }
  
  case object NotIn extends Operator {
    val symbol = "NOT IN"
  }
  
  case object IsNull extends Operator {
    val symbol = "IS NULL"
  }
  
  case object IsNotNull extends Operator {
    val symbol = "IS NOT NULL" 
  }
  
  case object Contains extends Operator {
    val symbol = "CONTAINS"
  }

  case object NotContains extends Operator {
    val symbol = "NOT CONTAINS"
  }
  
  case object StartsWith extends Operator {
    val symbol = "STARTS WITH"
  }
  
  case object EndsWith extends Operator {
    val symbol = "ENDS WITH"
  }
  
  case object SomeIn extends Operator {
    val symbol = "SOME IN"
  }
  
  case object ArrayEmpty extends Operator {
    val symbol = "IS EMPTY"
  }
  
  case object ArrayNotEmpty extends Operator {
    val symbol = "IS NOT EMPTY"
  }
}


/**
 * Base trait for all property types with operators
 * Represents a property with a name, operator, and value that can be evaluated
 */
trait Property[T] {
  val name    : String
  val operator: Operator
  val value   : T
  
  /**
   * Evaluates this property against a given value
   * @param actualValue the value to compare against
   * @return true if the property condition is satisfied
   */
  def evaluate(actualValue: Any): Boolean
}


/**
 * Enumeration of supported property types
 */
sealed trait PropertyType
object PropertyType {
  case object StringType       extends PropertyType
  case object IntegerType      extends PropertyType
  case object DoubleType       extends PropertyType
  case object StringArrayType  extends PropertyType
  case object IntegerArrayType extends PropertyType
  case object DoubleArrayType  extends PropertyType
}

/**
 * Companion object with factory methods and utilities
 */
object Property {
  /**
   * Factory method to create properties from strings
   */
  def parse(expression: String, properties: Map[String, PropertyType]): Try[Property[_]] = {
    Try {
      val trimmed = expression.trim
      
      // Find operator
      val operatorPattern  = "(>=|<=|!=|>|<|=|IN|NOT IN|IS NULL|IS NOT NULL|CONTAINS|NOT CONTAINS|STARTS WITH|ENDS WITH)".r
      operatorPattern.findFirstMatchIn(trimmed) match {
        case Some(opMatch) =>
          val op           = opMatch.matched
          val parts        = trimmed.split(op, 2)
          
          if (parts.length != 2) {
            throw new IllegalArgumentException(s"Invalid expression format: $expression")
          }
          
          val nodePropName = parts(0).trim.split("\\.")
          val nodeName     = nodePropName(0)
          val propertyName = nodePropName(1)
          val valueStr     = parts(1).trim
          
          val operator     = op.toUpperCase match {
            case "="            => Operator.Equal
            case "!="           => Operator.NotEqual
            case ">"            => Operator.GreaterThan
            case ">="           => Operator.GreaterThanOrEqual
            case "<"            => Operator.LessThan
            case "<="           => Operator.LessThanOrEqual
            case "IN"           => Operator.In
            case "NOT IN"       => Operator.NotIn
            case "IS NULL"      => Operator.IsNull
            case "IS NOT NULL"  => Operator.IsNotNull
            case "CONTAINS"     => Operator.Contains
            case "NOT CONTAINS" => Operator.NotContains
            case "STARTS WITH"  => Operator.StartsWith
            case "ENDS WITH"    => Operator.EndsWith
            case _              => throw new IllegalArgumentException(s"Unsupported operator: $op")
          }
          
          // If property is in properties map, use it to create the property
          if (properties.contains(propertyName)) {
            if (operator == Operator.IsNull || operator == Operator.IsNotNull) {
              properties.get(propertyName) match {
                case Some(PropertyType.StringType)       => 
                  return Success(StringProp(propertyName,       operator, null.asInstanceOf[String]       ))
                case Some(PropertyType.IntegerType)      => 
                  return Success(IntegerProp(propertyName,      operator, null.asInstanceOf[Int]          ))
                case Some(PropertyType.DoubleType)       => 
                  return Success(DoubleProp(propertyName,       operator, null.asInstanceOf[Double]       ))
                case Some(PropertyType.StringArrayType)  => 
                  return Success(StringArrayProp(propertyName,  operator, null.asInstanceOf[Array[String]]))
                case Some(PropertyType.IntegerArrayType) => 
                  return Success(IntegerArrayProp(propertyName, operator, null.asInstanceOf[Array[Int]]   ))
                case Some(PropertyType.DoubleArrayType)  => 
                  return Success(DoubleArrayProp(propertyName,  operator, null.asInstanceOf[Array[Double]]))
              }
            }
            
            properties.get(propertyName) match {
              case Some(PropertyType.StringType)       => 
                return Success(StringProp(propertyName,       operator, valueStr))
              case Some(PropertyType.IntegerType)      => 
                return Success(IntegerProp(propertyName,      operator, valueStr.toInt))
              case Some(PropertyType.DoubleType)       => 
                return Success(DoubleProp(propertyName,       operator, valueStr.toDouble))
              case Some(PropertyType.StringArrayType)  => 
                return Success(StringArrayProp(propertyName,  operator, valueStr.split(",").map(_.trim)))
              case Some(PropertyType.IntegerArrayType) => 
                return Success(IntegerArrayProp(propertyName, operator, valueStr.split(",").map(_.trim.toInt)))
              case Some(PropertyType.DoubleArrayType)  => 
                return Success(DoubleArrayProp(propertyName,  operator, valueStr.split(",").map(_.trim.toDouble)))
            }
          }
          
          // If property is not in properties map, create a typed property
          else {
            if (
              operator == Operator.IsNull    || 
              operator == Operator.IsNotNull
            )
              return Success(StringProp(propertyName, operator, ""))
          
            // Try to determine type and create appropriate property
            createTypedProperty(propertyName, operator, valueStr)
          }
          
        case None =>
          throw new IllegalArgumentException(s"No operator found in expression: $expression")
      }
    }
  }
  
  /**
   * Creates a typed property based on the value string
  */
  private def createTypedProperty(name: String, operator: Operator, valueStr: String): Property[_] = {
    val cleanValue = valueStr.trim
    
    // Array properties
    if (cleanValue.startsWith("[") && cleanValue.endsWith("]")) {
      val arrayContent = cleanValue.substring(1, cleanValue.length - 1).trim
      
      if (arrayContent.isEmpty)
        return StringArrayProp(name, operator, Array.empty[String])
      
      val elements = arrayContent.split(",").map(_.trim)
      
      // Is integer array
      if (elements.forall(e => Try(e.toInt).isSuccess))
        return IntegerArrayProp(name, operator, elements.map(_.toInt))
      
      // Is double array
      if (elements.forall(e => Try(e.toDouble).isSuccess))
        return DoubleArrayProp(name, operator, elements.map(_.toDouble))
      
      // Is string array
      val stringArray = elements.map { element =>
        if ((element.startsWith("'")  && element.endsWith("'") ) ||
            (element.startsWith("\"") && element.endsWith("\""))) {
          element.substring(1, element.length - 1)
        } else {
          element
        }
      }
      return StringArrayProp(name, operator, stringArray)
    }
    
    // Single value properties
    // Try integer
    Try(cleanValue.toInt) match {
      case Success(intValue)    => return IntegerProp(name, operator, intValue)
      case Failure(_)           => 
    }
    
    // Try double
    Try(cleanValue.toDouble) match {
      case Success(doubleValue) => return DoubleProp(name, operator, doubleValue)
      case Failure(_)           => 
    }
    
    // Default to string (remove quotes if present)
    val stringValue = 
      if (cleanValue.startsWith("'") && cleanValue.endsWith("'")) {
        cleanValue.substring(1, cleanValue.length - 1)
      } else if (cleanValue.startsWith("\"") && cleanValue.endsWith("\"")) {
        cleanValue.substring(1, cleanValue.length - 1)
      } else {
        cleanValue
      }
    
    StringProp(name, operator, stringValue)
  }

  /**
   * The PropertyType a given Property instance was built with
   */
  def typeOf(prop: Property[_]): PropertyType = prop match {
    case _: StringProp       => PropertyType.StringType
    case _: IntegerProp      => PropertyType.IntegerType
    case _: DoubleProp       => PropertyType.DoubleType
    case _: StringArrayProp  => PropertyType.StringArrayType
    case _: IntegerArrayProp => PropertyType.IntegerArrayType
    case _: DoubleArrayProp  => PropertyType.DoubleArrayType
  }
}