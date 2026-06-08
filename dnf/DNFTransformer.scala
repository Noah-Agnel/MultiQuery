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
    val step2 = distributeAndOverOr(step1)
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
   * (A ∨ B) ∧ (A ∨ C) becomes A ∨ (B ∧ C)
   * This step converts the expression to DNF
  */
  private def distributeAndOverOr(expr: BooleanExpression): BooleanExpression = expr match {
    case Variable(name)    => Variable(name)
    case Constant(value)   => Constant(value)
    case Negation(operand) => Negation(distributeAndOverOr(operand))
    case Conjunction(left, right) => 
      val leftDist  = distributeAndOverOr(left)
      val rightDist = distributeAndOverOr(right)

      (leftDist, rightDist) match {
          // (A ∨ B) ∧ C = (A ∧ C) ∨ (B ∧ C)
          case (Disjunction(a, b), c) =>
              Disjunction(
                  distributeAndOverOr(Conjunction(a, c)),
                  distributeAndOverOr(Conjunction(b, c))
              )
          // A ∧ (B ∨ C) = (A ∧ B) ∨ (A ∧ C)
          case (a, Disjunction(b, c)) =>
              Disjunction(
                  distributeAndOverOr(Conjunction(a, b)),
                  distributeAndOverOr(Conjunction(a, c))
              )
          // no distribution needed
          case (a, b) => Conjunction(a, b)
        }

    case Disjunction(left, right) => 
      Disjunction(distributeAndOverOr(left), distributeAndOverOr(right))
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
   * DNF is a disjunction of conjunctions of literals
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
    
    expr match {
      case literal if isLiteral(literal) => true
      case conjunction if isConjunctionOfLiterals(conjunction) => true
      case Disjunction(left, right) => 
        isConjunctionOfLiterals(left) && isDNF(right)
      case _ => false
    }
  }
  
  /**
   * Converts a DNF expression to a list of clauses (conjunctions)
   * Each clause is represented as a list of literals
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
      case Variable(_) | Negation(Variable(_)) | Constant(_) => List(List(expr))
      case Conjunction(_, _) => List(extractLiterals(expr))
      case Negation(inner) => List(extractLiterals(expr))
      case Disjunction(left, right) => 
        extractLiterals(left) :: dnfToClauses(right)
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