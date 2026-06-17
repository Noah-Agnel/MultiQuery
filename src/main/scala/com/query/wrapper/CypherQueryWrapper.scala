/**
 * Cypher Query Wrapper
 *
 * Takes a Neo4j-style Query and returns a Query Structure Object
 *
 */

package com.query.wrapper

import org.opencypher.v9_0.parser.CypherParser
import org.opencypher.v9_0.ast.{Match, Where, Return, Query, SingleQuery}
import org.opencypher.v9_0.expressions._
import org.opencypher.v9_0.util.CypherException
import dnf.BooleanExpressionParser
import com.query.{WhereClause, QueryStructure}
import com.properties._
import org.opencypher.v9_0.util.OpenCypherExceptionFactory

object CypherQueryWrapper {

    def convert(cypherQuery: String): Either[String, QueryStructure] = {
        
        // OpenCypher front-end parses the full query into AST
        val statement = new CypherParser().parse(cypherQuery, OpenCypherExceptionFactory(None))

        statement match {
            case Query(_, SingleQuery(clauses)) =>

                val matchClause  = clauses.collectFirst { case m: Match  => m }
                val whereClause   = clauses.collectFirst { case w: Where => w }
                    .orElse(matchClause.flatMap(_.where))
                val returnClause  = clauses.collectFirst { case r: Return => r }

                val queryStructure = new QueryStructure()
                
                // MATCH
                matchClause match {
                    case Some(m) => MatchClauseConverter.convert(m, queryStructure)
                    case None    => return Left("No MATCH clause found")
                }

                // WHERE (optional)
                whereClause match {
                    case Some(w) =>
                        WhereClauseConverter.convert(w.expression) match {
                            case Right(wc) => queryStructure.setWhereClause(wc)
                            case Left(err) => return Left(s"WHERE clause error: $err")
                        }
                    case None => // WHERE is optional, if none given do nothing
                }

                // RETURN
                returnClause match {
                    case Some(r) => queryStructure.setReturnClause(ReturnClauseConverter.convert(r, queryStructure))
                    case None    => return Left("No RETURN clause found")
                }
                return Right(queryStructure)
        }
    }
}