package com.query
import  scala.collection.mutable.ArrayBuffer
import  com.properties.Property
import  java.security.MessageDigest
import  java.nio.charset.StandardCharsets

/**
 * Represents a query node with a name and associated labels
 */
class QueryNode extends StatCondPropsMG {
  // Attributes for the query node    
  private var _nodeName        : String              = ""
  private var _labels          : ArrayBuffer[String] = ArrayBuffer.empty[String]
  private var _signature       : String              = ""
  
  // =============================== CONSTRUCTORS ================================
  // Primary constructor
  def this(nodeName: String) = {
    this()
    this._nodeName = nodeName
  }
  
  // Constructor with name and labels
  def this(
    nodeName: String,
    labels  : Array[String]
  ) = {
    this(nodeName)
    this._labels = ArrayBuffer(labels: _*)
  }

  // Constructor with name, labels, and cond_props
  def this(
    nodeName       : String,
    labels         : Array[String],
    stat_cond_props: Map[Integer, Map[String, Property[_]]]
  ) = {
    this(nodeName, labels)
    this._stat_cond_props = stat_cond_props
  }

  // ============================= GETTERS AND SETTERS =============================
  // 1. Node name
  def getNodeName: String = _nodeName
  
  def setNodeName(nodeName: String): Unit = {
    this._nodeName = nodeName
  }
  
  // 2. Node labels
  def getLabels: Array[String] = _labels.toArray
  
  def setLabels(labels: Array[String]): Unit = {
    this._labels = ArrayBuffer(labels: _*)
  }
  
  def addLabel(label: String): Unit = {
    if (!_labels.contains(label))
      _labels += label
  }
    
  def removeLabel(label: String): Unit = {
    _labels -= label
  }
  
  def hasLabel(label: String): Boolean = {
    _labels.contains(label)
  }
  
  def getLabelCount: Int = _labels.size
  
  def clearLabels(): Unit = {
    _labels.clear()
  }
  
  // 3. Node signature
  def getSignature: String = _signature

  def setSignature(signature: String): Unit = {
    this._signature = signature
  }

  def refreshSignature(): Unit = {
    _generateSignature()
  }
  

  // ============================= SIGNATURE GENERATION =============================
  /**
   * Generates an MD5 signature for this QueryNode based on all its properties
   * This signature can be used for equality checking and caching
   * @return MD5 hash string representing the object's signature
  */
  private def _generateSignature(): Unit = {
    val digest = MessageDigest.getInstance("MD5")
    
    // Create a normalized string representation of the object
    val sb     = new StringBuilder()
    
    // 1. Add node name
    sb.append("nodeName:").append(_nodeName).append("|")
    
    // 2. Add labels (sorted to ensure consistent ordering)
    sb.append("labels:[")
    _labels.sorted.foreach {label => sb.append(label).append(",")}
    sb.append("]|")
    
    // 3. Add stat_cond_props using common method from trait
    appendStatCondPropsToSignature(sb)
    
    // Generate MD5 hash
    val bytes = digest.digest(sb.toString.getBytes(StandardCharsets.UTF_8))
    _signature = bytes.map("%02x".format(_)).mkString
  }
  

  // ============================= EQUALS AND HASHCODE =============================
  override def equals(otherNode: Any): Boolean = {
    otherNode match {
      case that: QueryNode => 
        that.refreshSignature()
        this.refreshSignature()
        this._signature == that._signature
      case _ => false
    }
  }

 
  // ============================= TO STRING =============================
  override def toString: String = {
    val sb = new StringBuilder()
    sb.append("QueryNode(\n")
    sb.append("\tname=").append(_nodeName).append("\n")
    sb.append("\tlabels=[").append(_labels.mkString(", ")).append("]\n")
    
    if (_stat_cond_props.nonEmpty) {
      sb.append("\tstatCondProps={\n")
      _stat_cond_props.toSeq.sortBy(_._1).foreach { 
        case (status, propsMap) => {
          sb.append("\t\tstatus:").append(status).append("->{\n")
          propsMap.toSeq.sortBy(_._1).foreach {
            case (key, property) =>
              sb.append("\t\t\t").append(key).append("->").append(propertyToString(property)).append("\n")
          }
          sb.append("\t\t}\n")
        }
      }
      sb.append("\t}\n")
    }
    
    sb.append(")")
    sb.toString
  }
}
