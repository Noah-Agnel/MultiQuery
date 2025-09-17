/**
 * Abstract Syntax Tree for Boolean Expressions
 * 
 * This file defines the core structure for representing boolean expressions
 * as an abstract syntax tree. It supports variables, constants, and all
 * standard boolean operations including negation, conjunction, disjunction,
 * implication, and biconditional.
 */

package dnf

/**
 * Abstract base class for all boolean expressions
 */
sealed abstract class BooleanExpression {
  /**
   * Evaluates the expression given a variable assignment
   * @param assignment Map from variable names to boolean values
   * @return The boolean result of evaluation
  */
  def evaluate(assignment: Map[String, Boolean]): Boolean
  
  /**
   * Returns all variables present in this expression
   * @return Set of variable names
  */
  def variables: Set[String]
  
  /**
   * Returns a string representation of the expression
   * @return String representation
  */
  override def toString: String
}

/**
 * Represents a boolean variable (propositional variable)
 * @param name The name of the variable
 * @example Variable("p") creates a propositional variable named "p"
*/
case class Variable(name: String) extends BooleanExpression {
  def evaluate(assignment: Map[String, Boolean]): Boolean = 
    assignment.getOrElse(name, throw new IllegalArgumentException(s"Variable $name not found in assignment"))
  
  def variables: Set[String] = Set(name)
  
  override def toString: String = name
}

/**
 * Represents a boolean constant (true or false)
 * @param value The constant boolean value
*/
case class Constant(value: Boolean) extends BooleanExpression {
  def evaluate(assignment: Map[String, Boolean]): Boolean = value
  
  def variables: Set[String] = Set.empty
  
  override def toString: String = value.toString.toUpperCase
}

/**
 * Represents logical negation (NOT operation)
 * @param operand The expression to negate
 * @example Negation(Variable("p")) creates ¬p (NOT p)
*/
case class Negation(operand: BooleanExpression) extends BooleanExpression {
  def evaluate(assignment: Map[String, Boolean]): Boolean = !operand.evaluate(assignment)
  
  def variables: Set[String] = operand.variables
  
  override def toString: String = s"¬($operand)"
}

/**
 * Represents logical conjunction (AND operation)
 * @param left  Left operand
 * @param right Right operand
*/
case class Conjunction(left: BooleanExpression, right: BooleanExpression) extends BooleanExpression {
  def evaluate(assignment: Map[String, Boolean]): Boolean = 
    left.evaluate(assignment) && right.evaluate(assignment)
  
  def variables: Set[String] = left.variables ++ right.variables
  
  override def toString: String = s"($left ∧ $right)"
}

/**
 * Represents logical disjunction (OR operation)
 * @param left  Left operand
 * @param right Right operand
*/
case class Disjunction(left: BooleanExpression, right: BooleanExpression) extends BooleanExpression {
  def evaluate(assignment: Map[String, Boolean]): Boolean = 
    left.evaluate(assignment) || right.evaluate(assignment)
  
  def variables: Set[String] = left.variables ++ right.variables
  
  override def toString: String = s"($left ∨ $right)"
}

/**
 * Companion object providing utility methods for boolean expressions
 */
object BooleanExpression {
  /**
   * Creates a variable with the given name
   */
  def variable(name: String)  : 
      Variable     = Variable(name)
  
  /**
   * Creates a constant with the given value
   */
  def constant(value: Boolean): 
      Constant     = Constant(value)
  
  /**
   * Creates a negation of the given expression
   */
  def not(expr: BooleanExpression): 
      Negation = Negation(expr)
  
  /**
   * Creates a conjunction of two expressions
   */
  def and(left: BooleanExpression, right: BooleanExpression): 
      Conjunction = Conjunction(left, right)
  
  /**
   * Creates a disjunction of two expressions
   */
  def or(left: BooleanExpression, right: BooleanExpression): Disjunction = 
    Disjunction(left, right)
  
  // Convenience constants
  val TRUE: Constant = Constant(true)
  val FALSE: Constant = Constant(false)
} 