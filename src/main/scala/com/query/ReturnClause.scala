package com.query

class ReturnClause {
    // Attributes for the ReturnClause structure
    private var _nodes         : Map[String, QueryNode]     = Map.empty[String, QueryNode]
    private var _edges         : Map[String, QueryEdge]     = Map.empty[String, QueryEdge]

    // =============================== CONSTRUCTORS ================================
    def this(nodes: Map[String, QueryNode], edges: Map[String, QueryEdge]) = {
        this()
        this._nodes = nodes
        this._edges = edges
    }

    // =============================== GETTERS AND SETTERS ================================
    def getNodes: Map[String, QueryNode] = _nodes
    def getEdges: Map[String, QueryEdge] = _edges

    def setNodes(nodes: Map[String, QueryNode]): Unit = {
        this._nodes = nodes
    }
    
    def setEdges(edges: Map[String, QueryEdge]): Unit = {
        this._edges = edges
    }

    // =============================== OTHER METHODS ================================
    def addNode(nodeName: String, node: QueryNode): Unit = {
        this._nodes = this._nodes + (nodeName -> node)
    }

    def removeNode(nodeName: String): Unit = {
        this._nodes = this._nodes - nodeName
    }

    def addEdge(edgeName: String, edge: QueryEdge): Unit = {
        this._edges = this._edges + (edgeName -> edge)
    }

    def removeEdge(edgeName: String): Unit = {
        this._edges = this._edges - edgeName
    }

    // =============================== TO STRING ================================

    override def toString: String = {
        val sb = new StringBuilder()
        sb.append("nodes={")
        _nodes.foreach { case (name, _) => sb.append(name).append(" ") }
        sb.append("}\n")
        sb.append("edges={")
        _edges.foreach { case (name, _) => sb.append(name).append(" ") }
        sb.append("}")
        sb.toString
    }
}