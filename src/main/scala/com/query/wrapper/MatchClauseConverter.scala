/**
 * Match Clause Converter
 *
 * Takes a Match AST from the openCypher front-end and populates
 * a QueryStructure with the corresponding QueryNodes and QueryEdges.
 *
 * Supported patterns:
 * - (n:Label)                        -- simple node
 * - (n:Label)-[r:TYPE]->(m:Label)    -- directed outgoing
 * - (n:Label)<-[r:TYPE]-(m:Label)    -- directed incoming
 * - (n:Label)-[r:TYPE]-(m:Label)     -- undirected
 */

package com.query.wrapper

import org.opencypher.v9_0.ast.Match
import org.opencypher.v9_0.expressions._
import org.opencypher.v9_0.util.InputPosition
import com.query.{QueryStructure, QueryNode, QueryEdge}

object MatchClauseConverter {

    /**
     * Converts a Match AST clause into QueryNodes and QueryEdges
     * and populates the given QueryStructure
     *
     * @param matchClause           the Match AST clause from openCypher front-end
     * @param qs                    the QueryStructure to populate
     */
    def convert(matchClause: Match, qs: QueryStructure): Unit = {
        // a Match clause contains a Pattern
        // a Pattern contains a Sequence of PatternParts
        // each PatternPart contains a PatternElement (node or relationship chain)
        matchClause.pattern.patternParts.foreach { part =>
            processPatternElement(part.element, qs)
        }
    }

    // =============================== PRIVATE METHODS ================================

    /**
     * Recursively processes a PatternElement which can be:
     * - a NodePattern: (n:Label)
     * - a RelationshipChain: (n:Label)-[r:TYPE]->(m:Label)
     */
    private def processPatternElement(element: PatternElement, qs: QueryStructure): Unit = {
        element match {

            // simple node pattern: (n:Label)
            case NodePattern(variable, labels, properties, _) =>
                processNode(variable, labels, properties, qs)

            // relationship chain: left -[rel]-> right
            case RelationshipChain(left, relationship, right) =>
                // recursively process the left side first
                // (left can itself be a RelationshipChain for longer paths)
                processPatternElement(left, qs)

                // process the right node
                val rightNodeName = processNode(
                    right.variable,
                    right.labels,
                    right.properties,
                    qs
                )

                // get the left node name
                val leftNodeName = extractNodeName(left)

                // process the relationship
                processRelationship(relationship, leftNodeName, rightNodeName, qs)
        }
    }

    /**
     * Processes a NodePattern and adds the QueryNode to the QueryStructure
     * Returns the node name for use in relationship processing
     */
    private def processNode(
        variable  : Option[LogicalVariable],
        labels    : Seq[LabelName],
        properties: Option[Expression],
        qs        : QueryStructure
    ): String = {
        val nodeName   = variable.map(_.name).getOrElse("_anon")
        val labelArray = labels.map(_.name).toArray

        // only add if not already in the structure
        // (node may appear multiple times in complex patterns)
        if (qs.getNode(nodeName).isEmpty) {
            val node = new QueryNode(nodeName, labelArray)
            qs.addNode(node)
        }

        nodeName
    }

    /**
     * Processes a RelationshipPattern and adds the QueryEdge to the QueryStructure
     * Also updates nodeInEdges, nodeOutEdges, nodeInOutEdges accordingly
     */
    private def processRelationship(
        rel         : RelationshipPattern,
        leftNodeName: String,
        rightNodeName: String,
        qs          : QueryStructure
    ): Unit = {
        val edgeName = rel.variable.map(_.name).getOrElse("_anonEdge")
        val edgeType = rel.types.headOption.map(_.name).getOrElse("")

        // determine src and dst based on direction
        // SemanticDirection.OUTGOING: left -> right  (left is src, right is dst)
        // SemanticDirection.INCOMING: left <- right  (right is src, left is dst)
        // SemanticDirection.BOTH:     left -- right  (undirected)
        rel.direction match {

            case SemanticDirection.OUTGOING =>
                // (leftNode)-[r]->(rightNode)
                val edge = new QueryEdge(edgeName, leftNodeName, rightNodeName, edgeType)
                qs.addEdge(edge)
                qs.addEdgeToNodeOutEdges(leftNodeName, edgeName)
                qs.addEdgeToNodeInEdges(rightNodeName, edgeName)

            case SemanticDirection.INCOMING =>
                // (leftNode)<-[r]-(rightNode)
                val edge = new QueryEdge(edgeName, rightNodeName, leftNodeName, edgeType)
                qs.addEdge(edge)
                qs.addEdgeToNodeOutEdges(rightNodeName, edgeName)
                qs.addEdgeToNodeInEdges(leftNodeName, edgeName)

            case SemanticDirection.BOTH =>
                // (leftNode)-[r]-(rightNode) undirected
                val edge = new QueryEdge(edgeName, leftNodeName, rightNodeName, edgeType)
                qs.addEdge(edge)
                qs.addEdgeToNodeInOutEdges(leftNodeName, edgeName)
                qs.addEdgeToNodeInOutEdges(rightNodeName, edgeName)
        }
    }

    /**
     * Extracts the node name from the leftmost element of a pattern
     * Used to get the src node name when processing a RelationshipChain
     */
    private def extractNodeName(element: PatternElement): String = element match {
        case NodePattern(variable, _, _, _) =>
            variable.map(_.name).getOrElse("_anon")
        case RelationshipChain(_, _, right) =>
            right.variable.map(_.name).getOrElse("_anon")
    }
}