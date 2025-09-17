/**
 * Truth Table Generator and Analyzer
 * 
 * This file provides utilities for generating truth tables, validating boolean
 * expressions, and verifying that DNF transformations preserve the original
 * semantics of expressions.
*/

package dnf

/**
 * Represents a single row in a truth table
 * @param assignment Variable assignments for this row
 * @param result The boolean result for this assignment
 */
case class TruthTableRow(assignment: Map[String, Boolean], result: Boolean) {
  override def toString: String = {
    val vars = assignment.toSeq.sortBy(_._1).map {
      case (name, value) => s"$name=${if (value) "T" else "F"}"
    }.mkString(", ")
    s"$vars => ${if (result) "T" else "F"}"
  }
}

/**
 * Represents a complete truth table for a boolean expression
 * @param variables The variables in the expression (in sorted order)
 * @param rows All possible truth assignments and their results
 * 
 * @example
 * {{{
 * // Truth table for expression "a AND b"
 * val table = TruthTable(
 *   variables = List("a", "b"),
 *   rows = List(
 *     TruthTableRow(Map("a" -> false, "b" -> false), false),
 *     TruthTableRow(Map("a" -> false, "b" -> true), false),
 *     TruthTableRow(Map("a" -> true, "b" -> false), false),
 *     TruthTableRow(Map("a" -> true, "b" -> true), true)
 *   )
 * )
 * 
 * println(table.prettyPrint)
 * // Output:
 * // a | b | Result
 * // -------------
 * // F | F | F
 * // F | T | F
 * // T | F | F
 * // T | T | T
 * }}}
 */
case class TruthTable(variables: List[String], rows: List[TruthTableRow]) {
  
  /**
   * Returns the number of variables
   */
  def numVariables: Int = variables.length
  
  /**
   * Returns the number of satisfying assignments (rows where result is true)
   */
  def numSatisfyingAssignments: Int = rows.count(_.result)
  
  /**
   * Checks if the expression is a tautology (always true)
   */
  def isTautology: Boolean = rows.forall(_.result)
  
  /**
   * Checks if the expression is a contradiction (always false)
   */
  def isContradiction: Boolean = rows.forall(!_.result)
  
  /**
   * Checks if the expression is satisfiable (has at least one true assignment)
   */
  def isSatisfiable: Boolean = rows.exists(_.result)
  
  /**
   * Returns all satisfying assignments
   */
  def satisfyingAssignments: List[Map[String, Boolean]] = 
    rows.filter(_.result).map(_.assignment)
  
  /**
   * Returns all falsifying assignments
   */
  def falsifyingAssignments: List[Map[String, Boolean]] = 
    rows.filterNot(_.result).map(_.assignment)
  
  /**
   * Pretty prints the truth table
   */
  def prettyPrint: String = {
    if (rows.isEmpty) return "Empty truth table"
    
    val header = variables.mkString(" | ") + " | Result"
    val separator = "-" * header.length
    val rowStrings = rows.map { row =>
      val values = variables.map(v => if (row.assignment(v)) "T" else "F")
      val result = if (row.result) "T" else "F"
      (values :+ result).mkString(" | ")
    }
    
    (header :: separator :: rowStrings).mkString("\n")
  }
  
  /**
   * Compares this truth table with another for semantic equivalence
   */
  def isEquivalentTo(other: TruthTable): Boolean = {
    if (variables.toSet != other.variables.toSet) return false
    
    // Create maps for quick lookup
    val thisResults = rows.map(row => row.assignment -> row.result).toMap
    val otherResults = other.rows.map(row => row.assignment -> row.result).toMap
    
    thisResults == otherResults
  }
}

/**
 * Truth table generator and analyzer
 */
class TruthTableGenerator {
  
  /**
   * Generates a truth table for a boolean expression
   * @param expr The boolean expression
   * @return The complete truth table
   * 
   * @example
   * {{{
   * val expr = Conjunction(Variable("a"), Variable("b"))
   * val table = generateTruthTable(expr)
   * // Returns truth table with rows:
   * // a=F, b=F => F
   * // a=F, b=T => F  
   * // a=T, b=F => F
   * // a=T, b=T => T
   * }}}
  */
  def generateTruthTable(expr: BooleanExpression): TruthTable = {
    val variables   = expr.variables.toList.sorted
    val assignments = generateAllAssignments(variables)
    
    val rows = assignments.map { assignment =>
      val result = try {
        expr.evaluate(assignment)
      } catch {
        case _: Exception => false // Handle any evaluation errors gracefully
      }
      TruthTableRow(assignment, result)
    }
    
    TruthTable(variables, rows)
  }
  
  /**
   * Generates all possible boolean assignments for a list of variables
   * @param variables List of variable names
   * @return List of all possible assignments
   * 
   * @example
   * {{{
   * generateAllAssignments(List("a", "b"))
   * // Returns:
   * // List(
   * //   Map("a" -> false, "b" -> false),
   * //   Map("a" -> false, "b" -> true),
   * //   Map("a" -> true, "b" -> false),
   * //   Map("a" -> true, "b" -> true)
   * // )
   * }}}
  */
  private def generateAllAssignments(variables: List[String]): List[Map[String, Boolean]] = {
    if (variables.isEmpty) {
      List(Map.empty[String, Boolean])
    } else {
      val head = variables.head
      val tail = variables.tail
      val tailAssignments = generateAllAssignments(tail)
      
      for {
        tailAssignment <- tailAssignments
        headValue <- List(false, true)
      } yield tailAssignment + (head -> headValue)
    }
  }
  
  /**
   * Verifies that two expressions are semantically equivalent
   * @param expr1 First expression
   * @param expr2 Second expression
   * @return True if they have the same truth table
   */
  def areEquivalent(expr1: BooleanExpression, expr2: BooleanExpression): Boolean = {
    val table1 = generateTruthTable(expr1)
    val table2 = generateTruthTable(expr2)
    table1.isEquivalentTo(table2)
  }
  
  /**
   * Verifies that a DNF transformation preserves semantics
   * @param original The original expression
   * @param dnf      The DNF expression
   * @return True if they are equivalent
   */
  def verifyDNFTransformation(original: BooleanExpression, dnf: BooleanExpression): Boolean = {
    areEquivalent(original, dnf)
  }
  
  /**
   * Finds a satisfying assignment for an expression, if one exists
   * @param expr The boolean expression
   * @return Some assignment if satisfiable, None if unsatisfiable
   */
  def findSatisfyingAssignment(expr: BooleanExpression): Option[Map[String, Boolean]] = {
    val table = generateTruthTable(expr)
    table.satisfyingAssignments.headOption
  }
  
  /**
   * Counts the number of satisfying assignments
   * @param expr The boolean expression
   * @return Number of satisfying assignments
   */
  def countSatisfyingAssignments(expr: BooleanExpression): Int = {
    generateTruthTable(expr).numSatisfyingAssignments
  }
  
    /**
   * Generates a minimal DNF expression from a truth table
   * This creates a DNF directly from the satisfying assignments
   * @param table The truth table
   * @return A DNF expression representing the same function
   * 
   * @example
   * {{{
   * // Given a truth table for variables ["a", "b"] with satisfying rows:
   * // a=F, b=T => T
   * // a=T, b=F => T
   * // a=T, b=T => T
   * val table = TruthTable(List("a", "b"), List(
   *   TruthTableRow(Map("a" -> false, "b" -> false), false),
   *   TruthTableRow(Map("a" -> false, "b" -> true), true),
   *   TruthTableRow(Map("a" -> true, "b" -> false), true),
   *   TruthTableRow(Map("a" -> true, "b" -> true), true)
   * ))
   * val dnf = truthTableToDNF(table)
   * // Returns: (!a ∧ b) ∨ (a ∧ !b) ∨ (a ∧ b)
   * }}}
   */
  def truthTableToDNF(table: TruthTable): BooleanExpression = {
    val satisfyingRows = table.rows.filter(_.result)
    
    if (satisfyingRows.isEmpty) {
      Constant(false)
    } 
    else if (satisfyingRows.length == table.rows.length) {
      Constant(true)
    } 
    else {
      val clauses = satisfyingRows.map { row =>
        val literals = table.variables.map { variable =>
          if (row.assignment(variable))
            Variable(variable)
          else {
            Negation(Variable(variable))
          }
        }
        
        // Create conjunction of literals
        literals.reduceLeft(Conjunction)
      }
      
      // Create disjunction of clauses
      clauses.reduceLeft(Disjunction)
    }
  }
  
  /**
   * Analyzes an expression and provides detailed information
   * @param expr The boolean expression
   * @return Analysis results
   */
  def analyze(expr: BooleanExpression): ExpressionAnalysis = {
    val table        = generateTruthTable(expr)
    val isDNF        = DNFTransformer.isDNF(expr)
    val dnfForm      = if (isDNF) expr else DNFTransformer(expr)
    val dnfFromTable = truthTableToDNF(table)
    
    ExpressionAnalysis(
      originalExpression    = expr,
      truthTable            = table,
      isAlreadyDNF          = isDNF,
      dnfForm               = dnfForm,
      dnfFromTruthTable     = dnfFromTable,
      isTautology           = table.isTautology,
      isContradiction       = table.isContradiction,
      isSatisfiable         = table.isSatisfiable,
      satisfyingAssignments = table.satisfyingAssignments.length,
      totalAssignments      = table.rows.length
    )
  }
}

/**
 * Detailed analysis of a boolean expression
 */
case class ExpressionAnalysis(
  originalExpression: BooleanExpression,
  truthTable: TruthTable,
  isAlreadyDNF: Boolean,
  dnfForm: BooleanExpression,
  dnfFromTruthTable: BooleanExpression,
  isTautology: Boolean,
  isContradiction: Boolean,
  isSatisfiable: Boolean,
  satisfyingAssignments: Int,
  totalAssignments: Int
) {
  
  /**
   * Returns the ratio of satisfying assignments
   */
  def satisfactionRatio: Double = satisfyingAssignments.toDouble / totalAssignments
  
  /**
   * Pretty prints the analysis
   */
  def prettyPrint: String = {
    s"""Expression Analysis
       |==================
       |Original: $originalExpression
       |Variables: ${truthTable.variables.mkString(", ")}
       |
       |Properties:
       |  - Already in DNF: $isAlreadyDNF
       |  - Tautology: $isTautology
       |  - Contradiction: $isContradiction
       |  - Satisfiable: $isSatisfiable
       |  - Satisfying assignments: $satisfyingAssignments / $totalAssignments (${(satisfactionRatio * 100).round}%)
       |
       |DNF Form: $dnfForm
       |
       |Truth Table:
       |${truthTable.prettyPrint}""".stripMargin
  }
}

/**
 * Companion object with convenience methods
 */
object TruthTableGenerator {
  private val defaultGenerator = new TruthTableGenerator()
  
  /**
   * Generates a truth table for an expression
   */
  def apply(expr: BooleanExpression): TruthTable = {
    defaultGenerator.generateTruthTable(expr)
  }
  
  /**
   * Verifies that two expressions are equivalent
   */
  def areEquivalent(expr1: BooleanExpression, expr2: BooleanExpression): Boolean = {
    defaultGenerator.areEquivalent(expr1, expr2)
  }
  
  /**
   * Verifies a DNF transformation
   */
  def verifyDNF(original: BooleanExpression, dnf: BooleanExpression): Boolean = {
    defaultGenerator.verifyDNFTransformation(original, dnf)
  }
  
  /**
   * Analyzes an expression
   */
  def analyze(expr: BooleanExpression): ExpressionAnalysis = {
    defaultGenerator.analyze(expr)
  }
} 