package com.query

import dnf.{Variable, DNFTransformer}
import com.properties.LiteralCondition

/**
 * Classifies a WHERE clause's atomic conditions by when they can safely be evaluated.
 *
 * @param hoistable   single-variable literal conditions that are present as a positive
 *                    conjunct in every top-level DNF disjunct, grouped by variable name.
 *                    These are safe to apply as a pre-join filter on that variable's own
 *                    candidate set: since they're required regardless of which disjunct
 *                    ends up satisfying the WHERE clause, no row that fails one could
 *                    ever be part of a valid match.
 * @param whereClause the original clause. Hoisting is purely an optimization for pruning
 *                    candidates earlier — it never changes what's needed for correctness,
 *                    so the whole expression must still be evaluated per matched row.
 */
case class WherePredicatePlan(
  hoistable  : Map[String, Seq[LiteralCondition]],
  whereClause: WhereClause
)

object WherePredicatePlanner {

  def classify(whereClause: WhereClause): WherePredicatePlan = {
    val hoistable = whereClause.getExpression match {
      case None => Map.empty[String, Seq[LiteralCondition]]
      case Some(expr) =>
        val clauses = new DNFTransformer().dnfToClauses(expr)

        val positiveLettersPerClause: List[Set[String]] =
          clauses.map(_.collect { case Variable(name) => name }.toSet)

        val hoistableLetters: Set[String] =
          if (positiveLettersPerClause.isEmpty) Set.empty[String]
          else positiveLettersPerClause.reduce(_ intersect _)

        val hoistableConditions: Seq[LiteralCondition] =
          hoistableLetters.toSeq.flatMap { letter =>
            whereClause.getConditions.get(letter).collect { case lc: LiteralCondition => lc }
          }

        hoistableConditions.groupBy(_.variable)
    }

    WherePredicatePlan(hoistable, whereClause)
  }
}
