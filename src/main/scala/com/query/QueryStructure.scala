package com.query


class QueryStructure {
    // Attributes for the query structure
    private var _nodes: Map[String, QueryNode] = Map.empty[String, QueryNode]
    private var _edges: Map[String, QueryEdge] = Map.empty[String, QueryEdge]

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
    def addNode(node: QueryNode): Unit = {
        this._nodes += (node.getName -> node)
    }

    def addEdge(edge: QueryEdge): Unit = {
        this._edges += (edge.getName -> edge)
    }
    
    def removeNode(node: QueryNode): Unit = {
        this._nodes -= node.getName
    }

    def removeEdge(edge: QueryEdge): Unit = {
        this._edges -= edge.getName
    }
    
    def getNode(name: String): Option[QueryNode] = {
        this._nodes.get(name)
    }

    def getEdge(name: String): Option[QueryEdge] = {
        this._edges.get(name)
    }

    def getNodeCount: Int = _nodes.size
    def getEdgeCount: Int = _edges.size

    // =============================== TO STRING ================================
    override def toString: String = {
        val sb = new StringBuilder()
        sb.append("QueryStructure(\n")
        
        // Add nodes
        sb.append("\tnodes={\n")
        _nodes.foreach { case (name, node) =>
            sb.append("\t\t").append(name).append(" -> ").append(node.toString.replace("\n", "\n\t\t")).append("\n")
        }
        sb.append("\t}\n")
        
        // Add edges
        sb.append("\tedges={\n")
        _edges.foreach { case (name, edge) =>
            sb.append("\t\t").append(name).append(" -> ").append(edge.toString.replace("\n", "\n\t\t")).append("\n")
        }
        sb.append("\t}\n")
        
        sb.append(")")
        sb.toString
    }
}