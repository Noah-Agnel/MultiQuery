package com.query


class QueryStructure {
    // Attributes for the query structure
    private var _nodes         : Map[String, QueryNode]     = Map.empty[String, QueryNode]
    private var _edges         : Map[String, QueryEdge]     = Map.empty[String, QueryEdge]
    private var _nodeInEdges   : Map[String, Array[String]] = Map.empty[String, Array[String]]
    private var _nodeOutEdges  : Map[String, Array[String]] = Map.empty[String, Array[String]]
    private var _nodeInOutEdges: Map[String, Array[String]] = Map.empty[String, Array[String]]
    private var _whereClause   : Option[WhereClause]        = None

    // =============================== CONSTRUCTORS ================================
    def this(nodes: Map[String, QueryNode], edges: Map[String, QueryEdge]) = {
        this()
        this._nodes = nodes
        this._edges = edges
    }
    
    def this(
        nodes         : Map[String, QueryNode],
        edges         : Map[String, QueryEdge],
        nodeInEdges   : Map[String, Array[String]],
        nodeOutEdges  : Map[String, Array[String]],
        nodeInOutEdges: Map[String, Array[String]]
    ) = {
        this(nodes, edges) 
        this._nodeInEdges    = nodeInEdges
        this._nodeOutEdges   = nodeOutEdges
        this._nodeInOutEdges = nodeInOutEdges
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
        this._nodes += (node.getNodeName -> node)
    }

    def addEdge(edge: QueryEdge): Unit = {
        this._edges += (edge.getEdgeName -> edge)
    }
    
    def removeNode(node: QueryNode): Unit = {
        this._nodes -= node.getNodeName
    }

    def removeEdge(edge: QueryEdge): Unit = {
        this._edges -= edge.getEdgeName
    }
    
    def getNode(name: String): Option[QueryNode] = {
        this._nodes.get(name)
    }

    def getEdge(name: String): Option[QueryEdge] = {
        this._edges.get(name)
    }

    def getNodeCount: Int = _nodes.size
    def getEdgeCount: Int = _edges.size

    def getNodeInEdges(name: String): Option[Array[String]] = {
        this._nodeInEdges.get(name)
    }
    
    def getNodeOutEdges(name: String): Option[Array[String]] = {
        this._nodeOutEdges.get(name)
    }

    def getNodeInOutEdges(name: String): Option[Array[String]] = {
        this._nodeInOutEdges.get(name)
    }

    def setNodeInEdges(name: String, edges: Array[String]): Unit = {
        this._nodeInEdges += (name -> edges)
    }
    
    def setNodeOutEdges(name: String, edges: Array[String]): Unit = {
        this._nodeOutEdges += (name -> edges)
    }

    def setNodeInOutEdges(name: String, edges: Array[String]): Unit = {
        this._nodeInOutEdges += (name -> edges)
    }

    def removeNodeInEdges(name: String): Unit = {
        this._nodeInEdges -= name
    }
    
    def removeNodeOutEdges(name: String): Unit = {
        this._nodeOutEdges -= name
    }

    def removeNodeInOutEdges(name: String): Unit = {
        this._nodeInOutEdges -= name
    }

    def addEdgeToNodeInEdges(name: String, edge: String): Unit = {
        this._nodeInEdges += (name -> (
            this._nodeInEdges.getOrElse(name, Array.empty[String]) :+ edge
        ).sorted)
    }
    
    def addEdgeToNodeOutEdges(name: String, edge: String): Unit = {
        this._nodeOutEdges += (name -> (
            this._nodeOutEdges.getOrElse(name, Array.empty[String]) :+ edge
        ).sorted)
    }

    def addEdgeToNodeInOutEdges(name: String, edge: String): Unit = {
        this._nodeInOutEdges += (name -> (
            this._nodeInOutEdges.getOrElse(name, Array.empty[String]) :+ edge
        ).sorted)
    }

    def removeEdgeFromNodeInEdges(name: String, edge: String): Unit = {
        this._nodeInEdges += (name -> (
            this._nodeInEdges.getOrElse(name, Array.empty[String]) :+ edge
        ).sorted)
    }

    def removeEdgeFromNodeOutEdges(name: String, edge: String): Unit = {
        this._nodeOutEdges += (name -> (
            this._nodeOutEdges.getOrElse(name, Array.empty[String]) :+ edge
        ).sorted)
    }

    def removeEdgeFromNodeInOutEdges(name: String, edge: String): Unit = {
        this._nodeInOutEdges += (name -> (
            this._nodeInOutEdges.getOrElse(name, Array.empty[String]) :+ edge
        ).sorted)
    }

    def setWhereClause(wc: WhereClause): Unit = {
        this._whereClause = Some(wc)
    }

    def getWhereClause: Option[WhereClause] = _whereClause

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
        
        sb.append(")\n")

        // Add where clause if present
        this.getWhereClause.foreach { wc =>
            sb.append("--- WHERE CLAUSE ---\n")
            sb.append(s"DNF Expression: ${wc.getExpression}\n")
            sb.append(s"Conditions:\n")
            wc.getConditions.foreach { case (letter, (node, prop)) =>
                sb.append(s"  $letter -> node='$node', condition=$prop\n")
            }
        }

        sb.toString
    }
}