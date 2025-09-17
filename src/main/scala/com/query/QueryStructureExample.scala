package com.query

import com.properties.{StringProp, IntegerProp, Operator}

/**
 * Example demonstrating how to create a QueryStructure with three nodes:
 * - n1: Job node with specialization "Java Developer"
 * - n2: People node (Rossi, age 40) connected to n1 with "is_a" edge (since 2020)
 * - n3: People node (Verdi, age 18) connected to n1 with "is_a" edge (since 2022)
 */
object QueryStructureExample {

  def main(args: Array[String]): Unit = {
    
    // Create the QueryStructure with the specified example
    val queryStructure = createExampleQueryStructure()
    
    // Print the structure
    println("=== Query Structure Example ===")
    println(queryStructure.toString)
  }

  /**
   * Creates and returns the example QueryStructure with three nodes and two edges
   */
  def createExampleQueryStructure(): QueryStructure = {
    
    // Create the query structure
    val queryStructure = new QueryStructure()
    
    // ================ Create Node n1 (Job) ================
    val n1 = new QueryNode("n1", Array("Job"))
    // Add specialization property: "Java Developer"
    n1.addStatCondProp(0, "specialization", StringProp.equal("specialization", "Java Developer"))
    
    // ================ Create Node n2 (People - Rossi) ================
    val n2 = new QueryNode("n2", Array("People"))
    // Add surname: Rossi
    n2.addStatCondProp(0, "surname", StringProp.equal("surname", "Rossi"))
    // Add age: 40
    n2.addStatCondProp(0, "age", IntegerProp("age", Operator.Equal, 40))
    
    // ================ Create Node n3 (People - Verdi) ================
    val n3 = new QueryNode("n3", Array("People"))
    // Add surname: Verdi
    n3.addStatCondProp(0, "surname", StringProp.equal("surname", "Verdi"))
    // Add age: 18
    n3.addStatCondProp(0, "age", IntegerProp("age", Operator.Equal, 18))
    
    // ================ Create Edge 1 (n2 -> n1) ================
    // Updated constructor: edgeName, srcNodeName, dstNodeName, edgeType
    val edge1 = new QueryEdge("e1", "n2", "n1", "is_a")
    // Add since: 2020 property
    edge1.addStatCondProp(0, "since", IntegerProp("since", Operator.Equal, 2020))
    
    // ================ Create Edge 2 (n3 -> n1) ================
    // Updated constructor: edgeName, srcNodeName, dstNodeName, edgeType
    val edge2 = new QueryEdge("e2", "n3", "n1", "is_a")
    // Add since: 2022 property
    edge2.addStatCondProp(0, "since", IntegerProp("since", Operator.Equal, 2022))
    
    // ================ Add nodes and edges to the structure ================
    queryStructure.addNode(n1)
    queryStructure.addNode(n2)
    queryStructure.addNode(n3)
    
    queryStructure.addEdge(edge1)
    queryStructure.addEdge(edge2)
    
    // ================ Establish edge connections ================
    // For edge1 (n2 -> n1): n2 is source, n1 is destination
    queryStructure.addEdgeToNodeOutEdges("n2", "e1")  // n2 has outgoing edge e1
    queryStructure.addEdgeToNodeInEdges("n1", "e1")   // n1 has incoming edge e1 
    
    // For edge2 (n3 -> n1): n3 is source, n1 is destination
    queryStructure.addEdgeToNodeOutEdges("n3", "e2")  // n3 has outgoing edge e2
    queryStructure.addEdgeToNodeInEdges("n1", "e2")   // n1 has incoming edge e2
    
    queryStructure
  }
} 