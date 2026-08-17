/**
 * Disjunctive Normal Form (DNF) Transformer
 * 
 * This file implements the core algorithm for transforming any boolean expression
 * into Disjunctive Normal Form. DNF is a canonical form where the expression is
 * a disjunction of conjunctions of literals (variables or their negations).
 * 
 * The transformation process follows these steps:
 * 1. Push negations inward (De Morgan's laws)
 * 2. Distribute disjunctions over conjunctions
 * 3. Simplify the result
 */

package dnf

/**
 * Main class for transforming boolean expressions to DNF
 */
class DNFTransformer {
  /**
   * Transforms a boolean expression to Disjunctive Normal Form
   * @param expr The input boolean expression
   * @return The expression in DNF
   */
  def toDNF(expr: BooleanExpression): BooleanExpression = {
    val step1 = pushNegationsInward(expr)
    val step2 = distributeOrOverAnd(step1)
    val step3 = simplify(step2)
    step3
  }
  
  /**
   * Step 1: Push negations inward using De Morgan's laws
   * - ¬(A ∧ B) becomes ¬A ∨ ¬B
   * - ¬(A ∨ B) becomes ¬A ∧ ¬B
   * - ¬¬A becomes A
  */
  private def pushNegationsInward(expr: BooleanExpression): BooleanExpression = expr match {
    case Variable(name)  => Variable(name)
    case Constant(value) => Constant(value)
    case Conjunction(left, right) => 
      Conjunction(
        pushNegationsInward(left),
        pushNegationsInward(right)
      )
    case Disjunction(left, right) => 
      Disjunction(
        pushNegationsInward(left), 
        pushNegationsInward(right)
      )
    case Negation(operand) => pushNegationInward(operand)
  }
  
  /**
   * Helper method to push a single negation inward
  */
  private def pushNegationInward(expr: BooleanExpression): BooleanExpression = expr match {
    case Variable(name)    => Negation(Variable(name))
    case Constant(value)   => Constant(!value)
    case Negation(operand) => pushNegationsInward(operand) // Double negation elimination
    case Conjunction(left, right) => 
      // ¬(A ∧ B) = ¬A ∨ ¬B (De Morgan's law)
      Disjunction(
        pushNegationInward(left), 
        pushNegationInward(right)
      )
    case Disjunction(left, right) => 
      // ¬(A ∨ B) = ¬A ∧ ¬B (De Morgan's law)
      Conjunction(
        pushNegationInward(left), 
        pushNegationInward(right)
      )
  }
  
  /**
   * Step 2: Distribute conjunctions over disjunctions
   * - A ∧ (B ∨ C) becomes (A ∧ B) ∨ (A ∧ C)
   * This step converts the expression to DNF by pulling any OR nested inside an
   * AND up to the top, growing the top-level disjunction. A Disjunction's own
   * operands need no such distribution - once each side is itself recursively
   * converted, it's already a DNF sub-expression (a literal, a conjunction of
   * literals, or itself a disjunction of those), and DNF is closed under simply
   * disjoining two DNF expressions together.
  */
  private def distributeOrOverAnd(expr: BooleanExpression): BooleanExpression = expr match {
    case Variable(name)    => Variable(name)
    case Constant(value)   => Constant(value)
    case Negation(operand) => Negation(distributeOrOverAnd(operand))
    case Conjunction(left, right) =>
      distributeAndOverOr(distributeOrOverAnd(left), distributeOrOverAnd(right))
    case Disjunction(left, right) =>
      Disjunction(distributeOrOverAnd(left), distributeOrOverAnd(right))
  }

  /**
   * Helper method to distribute AND over OR
   * Handles the core distribution logic for DNF conversion
   */
  private def distributeAndOverOr(left: BooleanExpression, right: BooleanExpression): BooleanExpression = {
    (left, right) match {
      case (Disjunction(a, b), c) =>
        // (A ∨ B) ∧ C = (A ∧ C) ∨ (B ∧ C)
        Disjunction(
          distributeAndOverOr(a, c),
          distributeAndOverOr(b, c)
        )
      case (a, Disjunction(b, c)) =>
        // A ∧ (B ∨ C) = (A ∧ B) ∨ (A ∧ C)
        Disjunction(
          distributeAndOverOr(a, b),
          distributeAndOverOr(a, c)
        )
      case (a, b) => Conjunction(a, b)
    }
  }
  
  /**
   * Step 3: Simplify the expression
   * - Remove redundant terms
   * - Apply absorption laws
   * - Handle constants
   */
  private def simplify(expr: BooleanExpression): BooleanExpression = {
    val simplified = simplifyOnce(expr)
    if (simplified == expr) simplified else simplify(simplified)
  }
  
  /**
   * Performs one pass of simplification
   */
  private def simplifyOnce(expr: BooleanExpression): BooleanExpression = expr match {
    case Variable(name)    => Variable(name)
    case Constant(value)   => Constant(value)
    case Negation(operand) => 
      simplifyOnce(operand) match {
        case Constant(value) => Constant(!value)
        case simplified      => Negation(simplified)
      }
    case Conjunction(left, right) => 
      (simplifyOnce(left), simplifyOnce(right)) match {
        case (Constant(true), r)        => r
        case (l, Constant(true))        => l
        case (Constant(false), _)       => Constant(false)
        case (_, Constant(false))       => Constant(false)
        case (l, r) if l == r           => l // A ∧ A = A (idempotent)
        case (l, Negation(r)) if l == r => Constant(false) // A ∧ ¬A = FALSE
        case (Negation(l), r) if l == r => Constant(false) // ¬A ∧ A = FALSE
        case (l, r)                     => Conjunction(l, r)
      }
    case Disjunction(left, right) => 
      (simplifyOnce(left), simplifyOnce(right)) match {
        case (Constant(false), r)       => r
        case (l, Constant(false))       => l
        case (Constant(true), _)        => Constant(true)
        case (_, Constant(true))        => Constant(true)
        case (l, r) if l == r           => l // A ∨ A = A (idempotent)
        case (l, Negation(r)) if l == r => Constant(true) // A ∨ ¬A = TRUE
        case (Negation(l), r) if l == r => Constant(true) // ¬A ∨ A = TRUE
        case (l, r)                     => Disjunction(l, r)
      }
    case _ => throw new IllegalStateException("Unexpected expression type in simplification")
  }
  
  /**
   * Checks if an expression is already in DNF
   * DNF is a disjunction of conjunctions of literals. A disjunction chain can be
   * nested in any shape -- left-linear, right-linear, or balanced, depending on
   * how the parser/transformer built it -- so this recurses into both sides of
   * every Disjunction rather than assuming one side is always a single clause.
   */
  def isDNF(expr: BooleanExpression): Boolean = {
    def isLiteral(e: BooleanExpression): Boolean = e match {
      case Variable(_)           => true
      case Negation(Variable(_)) => true
      case Constant(_)           => true
      case _                     => false
    }

    def isConjunctionOfLiterals(e: BooleanExpression): Boolean = e match {
      case literal if isLiteral(literal) => true
      case Conjunction(left, right) =>
        isConjunctionOfLiterals(left) && isConjunctionOfLiterals(right)
      case _ => false
    }

    def isClauseOrDisjunctionOfClauses(e: BooleanExpression): Boolean = e match {
      case clause if isConjunctionOfLiterals(clause) => true
      case Disjunction(left, right) =>
        isClauseOrDisjunctionOfClauses(left) && isClauseOrDisjunctionOfClauses(right)
      case _ => false
    }

    isClauseOrDisjunctionOfClauses(expr)
  }

  /**
   * Converts a DNF expression to a list of clauses (conjunctions)
   * Each clause is represented as a list of literals. Recurses into both sides
   * of every Disjunction (see isDNF) so any nesting shape is handled, not just
   * a right-linear chain.
   */
  def dnfToClauses(expr: BooleanExpression): List[List[BooleanExpression]] = {
    require(isDNF(expr), "Expression must be in DNF")

    def extractLiterals(conjunction: BooleanExpression): List[BooleanExpression] = {
      conjunction match {
        case Variable(_) | Negation(Variable(_)) | Constant(_) => List(conjunction)
        case Conjunction(left, right) => extractLiterals(left) ++ extractLiterals(right)
        case _ => throw new IllegalArgumentException("Not a valid conjunction of literals")
      }
    }

    expr match {
      case Disjunction(left, right) => dnfToClauses(left) ++ dnfToClauses(right)
      case clause                   => List(extractLiterals(clause))
    }
  }
}

/**
 * Companion object providing convenience methods
 */
object DNFTransformer {
  /**
   * Transforms an expression to DNF using the default transformer
   */
  def apply(expr: BooleanExpression): BooleanExpression = {
    new DNFTransformer().toDNF(expr)
  }
  
  /**
   * Checks if an expression is in DNF
   */
  def isDNF(expr: BooleanExpression): Boolean = {
    new DNFTransformer().isDNF(expr)
  }
} 