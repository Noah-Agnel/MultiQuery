package com.query
import  com.properties.Property


trait StatCondPropsMG {
  
  // Type alias
  //                    simple cond id -> key -> property
  type StatCondProps = Map[Integer, Map[String, Property[_]]]
  
  // Protected field to be implemented by classes using this trait
  protected var _stat_cond_props: StatCondProps = Map.empty[Integer, Map[String, Property[_]]]
  
  
  // ============================= STAT COND PROPS MANAGEMENT =============================
  def getStatCondProps: StatCondProps = _stat_cond_props
    
  def setStatCondProps(stat_cond_props: StatCondProps): Unit = {
    this._stat_cond_props = stat_cond_props
  }
  
  def addStatCondProp(status: Integer, key: String, value: Property[_]): Unit = {
    val currentMap   = _stat_cond_props.getOrElse(status, Map.empty[String, Property[_]])
    val updatedMap   = currentMap + (key -> value)
    _stat_cond_props = _stat_cond_props + (status -> updatedMap)
  }

  def getStatCondProp(status: Integer, key: String): Property[_] = {
    _stat_cond_props.get(status).flatMap(_.get(key)).getOrElse(null)
  }
  
  def removeStatCondProp(status: Integer, key: String): Unit = {
    _stat_cond_props.get(status) match {
      case Some(innerMap) =>
        val updatedInnerMap = innerMap - key
        if (updatedInnerMap.isEmpty)
          _stat_cond_props = _stat_cond_props - status
        else
          _stat_cond_props = _stat_cond_props + (status -> updatedInnerMap)
        
      case None => // Do nothing if key doesn't exist
    }
  }

  def hasStat(status: Integer): Boolean = {
    _stat_cond_props.contains(status)
  }

  def hasStatCondProp(status: Integer, key: String): Boolean = {
    _stat_cond_props.get(status).exists(_.contains(key))
  }
  
  def getStatCondPropSize: Int = _stat_cond_props.values.map(_.size).sum
  
  def clearStatCondProps(): Unit = {
    _stat_cond_props = Map.empty[Integer, Map[String, Property[_]]]
  }

  // ============================= COMMON SIGNATURE GENERATION METHODS =============================
  /**
   * Generates a string representation of stat_cond_props for signature generation
   * This method ensures consistent ordering for reliable signature generation
   * @param sb StringBuilder to append the stat_cond_props representation to
   */
  protected def appendStatCondPropsToSignature(sb: StringBuilder): Unit = {
    sb.append("statCondProps:{")
    _stat_cond_props.toSeq.sortBy(_._1).foreach { 
       case (status, propsMap) => {
          sb.append("status:")
            .append(status)
            .append("->{")
          propsMap.toSeq.sortBy(_._1).foreach {
              case (key, property) =>
                  sb.append(key)
                    .append("->")
                    .append(propertyToString(property))
                    .append(",")
          }
          sb.append("},")
       }
    }
    sb.append("}")
  }

  /**
   * Converts a Property to its string representation for signature generation
   * @param property The property to convert
   * @return String representation of the property
   */
  protected def propertyToString(property: Property[_]): String = {
    if (property == null) 
      return "null"
    "Property(" + property.toString + ")"
  }
} 