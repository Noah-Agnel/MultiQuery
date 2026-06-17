/**
 * Where Clause Converter
 *
 * Takes a WHERE Expression AST from the openCypher front-end and converts it
 * into a WhereClause object containing:
 * - a BooleanExpression in DNF (the logic)
 * - a Map of variable letters to real property conditions
 */

package com.query.wrapper

import org.opencypher.v9_0.expressions._
import dnf.{BooleanExpressionParser, DNFTransformer}
import com.query.WhereClause
import com.properties.{Property => Prop, IntegerProp, StringProp, DoubleProp, StringArrayProp, Operator}

object WhereClauseConverter {

    /**
     * Converts a WHERE Expression AST into a WhereClause
     *
     * @param whereExpr the openCypher AST expression from the WHERE clause
     * @return Either a WhereClause on success or an error message on failure
     */
    def convert(whereExpr: Expression): Either[String, WhereClause] = {
        try {
            var counter    = 0
            var conditions = Map.empty[String, (String, Prop[_])]

            /**
             * Recursively walks the AST and builds an abstract logic string
             * e.g. "A & (B | C)"
             * while storing real conditions in the conditions map
             */
            def buildAbstractLogic(expr: Expression): String = expr match {

                // AND
                case And(lhs, rhs) =>
                    s"(${buildAbstractLogic(lhs)} & ${buildAbstractLogic(rhs)})"

                // OR
                case Or(lhs, rhs) =>
                    s"(${buildAbstractLogic(lhs)} | ${buildAbstractLogic(rhs)})"

                // NOT
                case Not(inner) =>
                    s"!(${buildAbstractLogic(inner)})"

                // XOR → (A | B) & !(A & B)
                case Xor(lhs, rhs) =>
                    val l = buildAbstractLogic(lhs)
                    val r = buildAbstractLogic(rhs)
                    s"(($l | $r) & !($l & $r))"

                // atomic condition → assign letter and store
                case atomic =>
                    val letter = ('A' + counter).toChar.toString
                    counter += 1
                    conditions = conditions + (letter -> extractCondition(atomic))
                    letter
            }

            // =============================== STEP 1: EXTRACT ATOMIC CONDITIONS ================================

            /**
             * Converts an atomic openCypher expression into a (nodeOrEdgeName, Property) tuple
             */
            def extractCondition(expr: Expression): (String, Prop[_]) = expr match {
                
                // ========== INTEGER CONDITIONS ==========
                // n.age = 25
                case Equals(Property(Variable(node), PropertyKeyName(prop)), SignedDecimalIntegerLiteral(value)) =>
                    (node, IntegerProp(prop, Operator.Equal, value.toInt))

                // n.age <> 25
                case NotEquals(Property(Variable(node), PropertyKeyName(prop)), SignedDecimalIntegerLiteral(value)) =>
                    (node, IntegerProp(prop, Operator.NotEqual, value.toInt))

                // n.age > 25
                case GreaterThan(Property(Variable(node), PropertyKeyName(prop)), SignedDecimalIntegerLiteral(value)) =>
                    (node, IntegerProp(prop, Operator.GreaterThan, value.toInt))

                // n.age >= 25
                case GreaterThanOrEqual(Property(Variable(node), PropertyKeyName(prop)), SignedDecimalIntegerLiteral(value)) =>
                    (node, IntegerProp(prop, Operator.GreaterThanOrEqual, value.toInt))

                // n.age < 25
                case LessThan(Property(Variable(node), PropertyKeyName(prop)), SignedDecimalIntegerLiteral(value)) =>
                    (node, IntegerProp(prop, Operator.LessThan, value.toInt))

                // n.age <= 25
                case LessThanOrEqual(Property(Variable(node), PropertyKeyName(prop)), SignedDecimalIntegerLiteral(value)) =>
                    (node, IntegerProp(prop, Operator.LessThanOrEqual, value.toInt))

                // n.age IS NULL
                case IsNull(Property(Variable(node), PropertyKeyName(prop))) =>
                    (node, IntegerProp(prop, Operator.IsNull, 0))

                // n.age IS NOT NULL
                case IsNotNull(Property(Variable(node), PropertyKeyName(prop))) =>
                    (node, IntegerProp(prop, Operator.IsNotNull, 0))

                // ========== DOUBLE CONDITIONS ==========
                // n.age = 25.0
                case Equals(Property(Variable(node), PropertyKeyName(prop)), DecimalDoubleLiteral(value)) =>
                    (node, DoubleProp(prop, Operator.Equal, value.toDouble))

                // n.age <> 25.0
                case NotEquals(Property(Variable(node), PropertyKeyName(prop)), DecimalDoubleLiteral(value)) =>
                    (node, DoubleProp(prop, Operator.NotEqual, value.toDouble))

                // n.age > 25
                case GreaterThan(Property(Variable(node), PropertyKeyName(prop)), DecimalDoubleLiteral(value)) =>
                    (node, DoubleProp(prop, Operator.GreaterThan, value.toDouble))

                // n.age >= 25
                case GreaterThanOrEqual(Property(Variable(node), PropertyKeyName(prop)), DecimalDoubleLiteral(value)) =>
                    (node, DoubleProp(prop, Operator.GreaterThanOrEqual, value.toDouble))

                // n.age < 25
                case LessThan(Property(Variable(node), PropertyKeyName(prop)), DecimalDoubleLiteral(value)) =>
                    (node, DoubleProp(prop, Operator.LessThan, value.toDouble))

                // n.age <= 25
                case LessThanOrEqual(Property(Variable(node), PropertyKeyName(prop)), DecimalDoubleLiteral(value)) =>
                    (node, DoubleProp(prop, Operator.LessThanOrEqual, value.toDouble))

                // ========== STRING CONDITIONS ==========
                // n.name = 'Paris'
                case Equals(Property(Variable(node), PropertyKeyName(prop)), StringLiteral(value)) =>
                    (node, StringProp.equal(prop, value))

                // n.name <> 'Paris'
                case NotEquals(Property(Variable(node), PropertyKeyName(prop)), StringLiteral(value)) =>
                    (node, StringProp.notEqual(prop, value))

                // n.name STARTS WITH 'Jo'
                case StartsWith(Property(Variable(node), PropertyKeyName(prop)), StringLiteral(value)) =>
                    (node, StringProp.startsWith(prop, value))

                // n.name ENDS WITH 'hn'
                case EndsWith(Property(Variable(node), PropertyKeyName(prop)), StringLiteral(value)) =>
                    (node, StringProp.endsWith(prop, value))

                // n.name CONTAINS 'oh'
                case Contains(Property(Variable(node), PropertyKeyName(prop)), StringLiteral(value)) =>
                    (node, StringProp.contains(prop, value))

                // n.city IN ['Paris', 'Lyon']
                case In(Property(Variable(node), PropertyKeyName(prop)), ListLiteral(values)) =>
                    val stringValues = values.collect { case StringLiteral(v) => v }.toArray
                    (node, StringArrayProp.In(prop, stringValues))

                // n.name < 'Paris'
                case LessThan(Property(Variable(node), PropertyKeyName(prop)), StringLiteral(value)) =>
                    (node, StringProp.lessThan(prop, value))

                // n.name <= 'Paris'
                case LessThanOrEqual(Property(Variable(node), PropertyKeyName(prop)), StringLiteral(value)) =>
                    (node, StringProp.lessThanOrEqual(prop, value))

                // n.name > 'Paris'
                case GreaterThan(Property(Variable(node), PropertyKeyName(prop)), StringLiteral(value)) =>
                    (node, StringProp.greaterThan(prop, value))

                // n.name >= 'Paris'
                case GreaterThanOrEqual(Property(Variable(node), PropertyKeyName(prop)), StringLiteral(value)) =>
                    (node, StringProp.greaterThanOrEqual(prop, value))

                case other =>
                    throw new IllegalArgumentException(s"Unsupported WHERE condition: $other")
            }

            // =============================== STEP 2: BUILD ABSTRACT LOGIC STRING ================================

            val abstractLogic = buildAbstractLogic(whereExpr)

            // =============================== STEP 3: PARSE TO BOOLEAN EXPRESSION ================================

            val booleanExpr = BooleanExpressionParser.parse(abstractLogic) match {
                case Right(expr) => expr
                case Left(error) => return Left(s"Failed to parse logic: $error")
            }

            // =============================== STEP 4: CONVERT TO DNF ================================

            val dnfExpr = DNFTransformer(booleanExpr)

            // =============================== STEP 5: BUILD WhereClause ================================

            Right(new WhereClause(dnfExpr, conditions))

        } catch {
            case e: IllegalArgumentException => Left(s"Unsupported condition: ${e.getMessage}")
            case e: Exception                => Left(s"Unexpected error: ${e.getMessage}")
        }
    }
}