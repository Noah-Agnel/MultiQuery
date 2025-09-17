/**
 * Boolean Expression Parser
 * 
 * This file implements a recursive descent parser for boolean expressions.
 * It can parse expressions with variables, constants, and basic boolean operators
 * with proper precedence and associativity rules.
 * 
 * Supported syntax:
 * - Variables: any identifier (letters, digits, underscore)
 * - Constants: TRUE, FALSE, true, false, T, F
 * - Operators: !, ~, ¬ (NOT), &, ∧, AND (AND), |, ∨, OR (OR)
 * - Parentheses for grouping
*/

package dnf

import scala.util.parsing.combinator._
import scala.util.{Try, Failure}

/**
 * Parser for boolean expressions using Scala's parser combinators
 */
class BooleanExpressionParser extends RegexParsers {
  
  // Override whitespace handling to allow for flexible spacing
  override val whiteSpace = """(\s|//.*|(?m)/\*(\*(?!/)|[^*])*\*/)+""".r
  
  /**
   * Main parsing method
   * @param input String representation of the boolean expression
   * @return Either a parsed BooleanExpression or an error message
   */
  def parse(input: String): Either[String, BooleanExpression] = {
    parseAll(expression, input) match {
      case Success(expr, _)  => Right(expr)
      case failure => Left(s"Parse error: $failure")
    }
  }
  
  // Grammar rules with proper precedence (lowest to highest)
  
  /**
   * Top-level expression parser
   * Handles disjunction (lowest precedence)
  */
  def expression: Parser[BooleanExpression] = disjunction
  
  /**
   * Disjunction expressions (A | B, A ∨ B, A OR B)
   * Left associative
  */
  def disjunction: Parser[BooleanExpression] = 
    conjunction ~ rep(disjunctionOp ~ conjunction) ^^ {
      case head ~ tail => tail.foldLeft(head) {
        case (left, op ~ right) => Disjunction(left, right)
      }
    }
  
  /**
   * Conjunction expressions (A & B, A ∧ B, A AND B)
   * Left associative
   */
  def conjunction: Parser[BooleanExpression] = 
    negation ~ rep(conjunctionOp ~ negation) ^^ {
      case head ~ tail => tail.foldLeft(head) {
        case (left, op ~ right) => Conjunction(left, right)
      }
    }
  
  /**
   * Negation expressions (¬A, !A, ~A, NOT A)
   * Right associative (highest precedence)
   */
  def negation: Parser[BooleanExpression] = 
    rep(negationOp) ~ atom ^^ {
      case ops ~ expr => ops.foldRight(expr)((op, e) => Negation(e))
    }
  
  /**
   * Atomic expressions (variables, constants, parenthesized expressions)
   */
  def atom: Parser[BooleanExpression] = 
    variable | constant | parenthesized
  
  /**
   * Parenthesized expressions
   */
  def parenthesized: Parser[BooleanExpression] = 
    "(" ~> expression <~ ")"
  
  /**
   * Variable parser
   * Accepts any identifier starting with a letter or underscore
   */
  def variable: Parser[Variable] = 
    """[a-zA-Z_][a-zA-Z0-9_]*""".r ^^ { name => Variable(name) }
  
  /**
   * Constant parser
   * Accepts various representations of true/false
   */
  def constant: Parser[Constant] = 
    ("TRUE"  | "true"  | "T" | "1") ^^ { _ => Constant(true)  } |
    ("FALSE" | "false" | "F" | "0") ^^ { _ => Constant(false) }
  
  // Operator parsers
  
  def disjunctionOp: Parser[String] = 
    "|" | "∨" | "OR" | "or"
  
  def conjunctionOp: Parser[String] = 
    "&" | "∧" | "AND" | "and"
  
  def negationOp: Parser[String] = 
    "!" | "~" | "¬" | "NOT" | "not"
}

/**
 * Enhanced parser with additional features
 */
class AdvancedBooleanExpressionParser extends BooleanExpressionParser {
  
  /**
   * Parse with detailed error information
   */
  def parseWithDetails(input: String): dnf.ParseResult = {
    parseAll(expression, input) match {
      case Success(expr, _) => 
        ParseResult.Success(expr)
      case failure => 
        ParseResult.Error(failure.toString, 1, 1, input)
    }
  }
  
  /**
   * Parse and immediately convert to DNF
   */
  def parseToDNF(input: String): Either[String, BooleanExpression] = {
    parse(input) match {
      case Right(expr) => Right(DNFTransformer(expr))
      case Left(error) => Left(error)
    }
  }
  
  /**
   * Parse with variable validation
   * Ensures all variables are in the allowed set
   */
  def parseWithValidation(input: String, allowedVariables: Set[String]): Either[String, BooleanExpression] = {
    parse(input) match {
      case Right(expr) => 
        val usedVars = expr.variables
        val invalidVars = usedVars -- allowedVariables
        if (invalidVars.nonEmpty) {
          Left(s"Unknown variables: ${invalidVars.mkString(", ")}")
        } else {
          Right(expr)
        }
      case Left(error) => Left(error)
    }
  }
}

/**
 * Result of parsing with detailed information
 */
sealed trait ParseResult
object ParseResult {
  case class Success(expression: BooleanExpression) extends ParseResult
  case class Error(message: String, line: Int, column: Int, input: String) extends ParseResult {
    def prettyPrint: String = {
      val lines     = input.split('\n')
      val errorLine = if (line > 0 && line <= lines.length) lines(line - 1) else ""
      val pointer   = " " * (column - 1) + "^"
      
      s"""Parse error at line $line, column $column: $message
         |$errorLine
         |$pointer""".stripMargin
    }
  }
}

/**
 * Companion object with convenience methods
 */
object BooleanExpressionParser {
  private val defaultParser  = new BooleanExpressionParser()
  private val advancedParser = new AdvancedBooleanExpressionParser()
  
  /**
   * Parse a boolean expression from string
   */
  def parse(input: String): Either[String, BooleanExpression] = {
    defaultParser.parse(input)
  }
  
  /**
   * Parse with detailed error information
   */
  def parseWithDetails(input: String): dnf.ParseResult = {
    advancedParser.parseWithDetails(input)
  }
  
  /**
   * Parse and convert to DNF in one step
   */
  def parseToDNF(input: String): Either[String, BooleanExpression] = {
    advancedParser.parseToDNF(input)
  }
  
  /**
   * Parse with variable validation
   */
  def parseWithValidation(input: String, allowedVariables: Set[String]): Either[String, BooleanExpression] = {
    advancedParser.parseWithValidation(input, allowedVariables)
  }
  
  /**
   * Try to parse, returning None on failure
   */
  def tryParse(input: String): Option[BooleanExpression] = {
    parse(input).toOption
  }
  
  /**
   * Parse multiple expressions from a list of strings
   */
  def parseMultiple(inputs: List[String]): List[Either[String, BooleanExpression]] = {
    inputs.map(parse)
  }
} 