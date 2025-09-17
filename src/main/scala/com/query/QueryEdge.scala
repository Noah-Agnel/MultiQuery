package com.query
import  scala.collection.mutable.ArrayBuffer
import  com.properties.Property
import  java.security.MessageDigest
import  java.nio.charset.StandardCharsets

/**
 * Represents a query edge with a name and associated type
 */
class QueryEdge extends StatCondPropsMG {  
  // Attributes for the query edge    
  private var _edgeName        : String         = ""
  private var _type            : String         = ""
  private var _signature       : String         = ""
  
  // =============================== CONSTRUCTORS ================================
  // Primary constructor
  def this(edgeName: String) = {
    this()
    this._edgeName = edgeName
  }
  
  // Constructor with name and type
  def this(
    edgeName: String,
    edgeType: String
  ) = {
    this(edgeName)
    this._type = edgeType
  }

  // Constructor with name, type, and cond_props
  def this(
    edgeName       : String,
    edgeType       : String,
    stat_cond_props: StatCondProps
  ) = {
    this(edgeName, edgeType)
    this._stat_cond_props = stat_cond_props
  }

  // ============================= GETTERS AND SETTERS =============================
  // 1. Edge name
  def getEdgeName: String = _edgeName
  
  def setEdgeName(edgeName: String): Unit = {
    this._edgeName = edgeName
  }
  
  // 2. Edge type
  def getType: String = _type
  
  def setType(edgeType: String): Unit = {
    this._type = edgeType
  }
  
  // 3. Edge signature
  def getSignature: String = _signature

  def setSignature(signature: String): Unit = {
    this._signature = signature
  }

  def refreshSignature(): Unit = {
      _generateSignature()
  }
  

  // ============================= SIGNATURE GENERATION =============================
  /**
   * Generates an MD5 signature for this QueryEdge based on all its properties
   * This signature can be used for equality checking and caching
   * @return MD5 hash string representing the object's signature
  */
  private def _generateSignature(): Unit = {
    val digest = MessageDigest.getInstance("MD5")
    
    // Create a normalized string representation of the object
    val sb     = new StringBuilder()
    
    // 1. Add edge name
    sb.append("edgeName:").append(_edgeName).append("|")
    
    // 2. Add type
    sb.append("type:").append(_type).append("|")
    
    // 3. Add stat_cond_props using common method from trait
    appendStatCondPropsToSignature(sb)
    
    // Generate MD5 hash
    val bytes = digest.digest(sb.toString.getBytes(StandardCharsets.UTF_8))
    _signature = bytes.map("%02x".format(_)).mkString
  }
  

  // ============================= EQUALS AND HASHCODE =============================
  override def equals(otherEdge: Any): Boolean = {
    otherEdge match {
      case that: QueryEdge => 
        that.refreshSignature()
        this.refreshSignature()
        this._signature == that._signature
      case _ => false
    }
  }
  

  // ============================= TO STRING =============================
  override def toString: String = {
    val sb = new StringBuilder()
    sb.append("QueryEdge(\n")
    sb.append("\tname=").append(_edgeName).append("\n")
    sb.append("\ttype=").append(_type).append("\n")
    
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