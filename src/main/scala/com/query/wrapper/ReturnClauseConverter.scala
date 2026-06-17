/**
 * Return Clause Converter
 *
 * Takes a Return Expression AST from the openCypher front-end and converts it
 * into a ReturnClause object containing:
 *
 * The nodes and edges that the query must return
 * The properties of the nodes/edges that the query must return
 */

package com.query.wrapper

import org.opencypher.v9_0.ast.Return
import org.opencypher.v9_0.expressions._
import org.opencypher.v9_0.util.InputPosition
import com.query.{QueryStructure, QueryNode, QueryEdge, ReturnClause}

object ReturnClauseConverter {

    /**
     * Converts a Return AST clause into a ReturnClause object
     *
     * @param returnClause    the Return AST clause from openCypher front-end
     */
    def convert(returnClause: Return, qs: QueryStructure): ReturnClause = {
        val rc = new ReturnClause()

        returnClause.returnItems.items.foreach { item =>
            item.expression match {
                case Variable(name) =>
                    // check if it's a node or edge in the QueryStructure
                    qs.getNode(name).foreach(node => rc.addNode(name, node))
                    qs.getEdge(name).foreach(edge => rc.addEdge(name, edge))
                case Property(Variable(varName), PropertyKeyName(propName)) =>
                    val alias = item.alias.map(_.name).getOrElse(s"$varName.$propName")
                    rc.addProperty(alias, varName, propName)
                case _ => // ignore unsupported expressions for now
            }
        }

        rc
    }
}