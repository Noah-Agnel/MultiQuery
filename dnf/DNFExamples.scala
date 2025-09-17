/**
 * DNF Transformation Examples and Demonstrations
 * 
 * This file provides comprehensive examples of how to use the DNF transformation
 * algorithm with various types of boolean expressions, from simple to very complex.
 * It includes test cases, benchmarks, and educational examples.
 */

package dnf

/**
 * Collection of example boolean expressions and their transformations
 */
object DNFExamples {
  
  // Import convenience methods for building expressions
  import BooleanExpression._
  
  /**
   * Simple examples for educational purposes
   */
  object SimpleExamples {
    
    // Basic operations
    val simpleConjunction = and(variable("A"), variable("B"))
    val simpleDisjunction = or(variable("A"), variable("B"))
    val simpleNegation = not(variable("A"))
    
    // Additional conjunction examples
    val basicConjunction2 = and(variable("A"), variable("B"))
    val chainedConjunction = and(and(variable("A"), variable("B")), variable("C"))
    
    // Additional disjunction examples
    val basicDisjunction2 = or(variable("A"), variable("B"))
    val complexDisjunction = or(and(variable("A"), variable("B")), or(variable("C"), variable("D")))
    
    // Mixed operations
    val mixedExpression = or(
      and(variable("A"), not(variable("B"))),
      and(not(variable("A")), variable("B"))
    )
    
    /**
     * Demonstrates the simple examples
     */
    def demonstrateSimpleExamples(): Unit = {
      println("=== Simple Examples ===")
      
      val examples = List(
        ("Simple Conjunction", simpleConjunction),
        ("Simple Disjunction", simpleDisjunction),
        ("Simple Negation", simpleNegation),
        ("Basic Conjunction 2", basicConjunction2),
        ("Chained Conjunction", chainedConjunction),
        ("Basic Disjunction 2", basicDisjunction2),
        ("Complex Disjunction", complexDisjunction),
        ("Mixed Expression", mixedExpression)
      )
      
      examples.foreach { case (name, expr) =>
        println(s"\n$name:")
        println(s"  Original: $expr")
        val dnf = DNFTransformer(expr)
        println(s"  DNF:      $dnf")
        println(s"  Already DNF: ${DNFTransformer.isDNF(expr)}")
        println(s"  Verification: ${TruthTableGenerator.verifyDNF(expr, dnf)}")
      }
    }
  }
  
  /**
   * Complex examples that stress-test the algorithm
   */
  object ComplexExamples {
    
    // De Morgan's laws examples
    val deMorgan1 = not(and(variable("A"), variable("B")))
    val deMorgan2 = not(or(variable("A"), variable("B")))
    
    // Complex nested conjunctions
    val nestedConjunctions = and(
      and(variable("A"), variable("B")),
      and(variable("C"), or(variable("D"), variable("E")))
    )
    
    // Multiple conjunctions
    val multipleConjunctions = and(
      and(variable("A"), variable("B")),
      and(variable("C"), variable("D"))
    )
    
    // Deeply nested expression
    val deeplyNested = not(
      or(
        and(
          and(variable("A"), variable("B")),
          not(or(variable("C"), variable("D")))
        ),
        and(
          or(variable("E"), not(variable("F"))),
          and(not(variable("G")), variable("H"))
        )
      )
    )
    
    // Expression with many variables
    val manyVariables = or(
      and(variable("A"), and(variable("B"), and(variable("C"), variable("D")))),
      and(variable("E"), and(variable("F"), and(variable("G"), variable("H"))))
    )
    
    // Tautology example
    val tautologyExample = or(variable("A"), not(variable("A")))
    
    // Contradiction example
    val contradictionExample = and(variable("A"), not(variable("A")))
    
    /**
     * Demonstrates complex examples with detailed analysis
     */
    def demonstrateComplexExamples(): Unit = {
      println("\n=== Complex Examples ===")
      
      val examples = List(
        ("De Morgan's Law 1", deMorgan1),
        ("De Morgan's Law 2", deMorgan2),
        ("Nested Conjunctions", nestedConjunctions),
        ("Multiple Conjunctions", multipleConjunctions),
        ("Deeply Nested", deeplyNested),
        ("Many Variables", manyVariables),
        ("Tautology", tautologyExample),
        ("Contradiction", contradictionExample)
      )
      
      examples.foreach { case (name, expr) =>
        println(s"\n$name:")
        println(s"  Original: $expr")
        
        val analysis = TruthTableGenerator.analyze(expr)
        println(s"  Variables: ${analysis.truthTable.variables.mkString(", ")}")
        println(s"  DNF: ${analysis.dnfForm}")
        println(s"  Properties: ${if (analysis.isTautology) "Tautology" else if (analysis.isContradiction) "Contradiction" else "Contingent"}")
        println(s"  Satisfying assignments: ${analysis.satisfyingAssignments}/${analysis.totalAssignments}")
      }
    }
  }
  
  /**
   * Real-world examples that might appear in practical applications
   */
  object RealWorldExamples {
    
    // Access control logic
    val accessControl = or(
      and(variable("isAdmin"), variable("isAuthenticated")),
      and(
        and(variable("isOwner"), variable("isAuthenticated")),
        not(variable("isBlocked"))
      )
    )
    
    // Circuit logic
    val circuitLogic = and(
      or(variable("switch1"), variable("switch2")),
      and(variable("emergency"), not(variable("power")))
    )
    
    // Conditional workflow
    val workflowLogic = and(
      and(variable("dataValidated"), variable("userApproved")),
      or(variable("autoProcess"), variable("manualReview"))
    )
    
    // Game logic
    val gameLogic = or(
      and(variable("hasKey"), variable("doorUnlocked")),
      and(variable("hasPassword"), not(variable("alarmActive")))
    )
    
    // Business rules
    val businessRules = and(
      and(variable("isPremiumCustomer"), variable("freeShipping")),
      and(
        and(variable("orderValue"), variable("inStock")),
        variable("canPurchase")
      )
    )
    
    /**
     * Demonstrates real-world examples
     */
    def demonstrateRealWorldExamples(): Unit = {
      println("\n=== Real-World Examples ===")
      
      val examples = List(
        ("Access Control", accessControl),
        ("Circuit Logic", circuitLogic),
        ("Workflow Logic", workflowLogic),
        ("Game Logic", gameLogic),
        ("Business Rules", businessRules)
      )
      
      examples.foreach { case (name, expr) =>
        println(s"\n$name:")
        println(s"  Original: $expr")
        
        val dnf = DNFTransformer(expr)
        println(s"  DNF: $dnf")
        
        // Find a satisfying assignment
        TruthTableGenerator.apply(expr).satisfyingAssignments.headOption match {
          case Some(assignment) =>
            println(s"  Example satisfying assignment: ${assignment.map { case (k, v) => s"$k=$v" }.mkString(", ")}")
          case None =>
            println(s"  No satisfying assignments (contradiction)")
        }
      }
    }
  }
  
  /**
   * Performance testing with increasingly complex expressions
   */
  object PerformanceExamples {
    
    /**
     * Generates a chain of implications: A1 → (A2 → (A3 → ... → An))
     */
    def generateImplicationChain(n: Int): BooleanExpression = {
      if (n <= 0) {
        throw new IllegalArgumentException("n must be positive")
      } else if (n == 1) {
        variable(s"A1")
      } else {
        and(variable(s"A$n"), generateImplicationChain(n - 1))
      }
    }
    
    /**
     * Generates a nested conjunction: A1 ∧ (A2 ∧ (A3 ∧ ... ∧ An))
     */
    def generateNestedConjunction(n: Int): BooleanExpression = {
      if (n <= 0) {
        throw new IllegalArgumentException("n must be positive")
      } else if (n == 1) {
        variable(s"A1")
      } else {
        and(variable(s"A$n"), generateNestedConjunction(n - 1))
      }
    }
    
    /**
     * Generates alternating nested operations
     */
    def generateAlternatingNested(n: Int): BooleanExpression = {
      if (n <= 0) {
        throw new IllegalArgumentException("n must be positive")
      } else if (n == 1) {
        variable(s"A1")
      } else {
        if (n % 2 == 0) {
          and(variable(s"A$n"), generateAlternatingNested(n - 1))
        } else {
          or(variable(s"A$n"), generateAlternatingNested(n - 1))
        }
      }
    }
    
    /**
     * Performance test with timing
     */
    def performanceTest(): Unit = {
      println("\n=== Performance Testing ===")
      
      val testSizes = List(5, 8, 10, 12)
      
      testSizes.foreach { size =>
        println(s"\nTesting with $size variables:")
        
        // Test implication chain
        val implChain = generateImplicationChain(size)
        val start1 = System.nanoTime()
        val dnf1 = DNFTransformer(implChain)
        val time1 = (System.nanoTime() - start1) / 1e6
        println(f"  Implication chain: ${time1}%.2f ms")
        
        // Test nested conjunction
        val nestedConj = generateNestedConjunction(size)
        val start2 = System.nanoTime()
        val dnf2 = DNFTransformer(nestedConj)
        val time2 = (System.nanoTime() - start2) / 1e6
        println(f"  Nested conjunction: ${time2}%.2f ms")
        
        // Test alternating operations
        val alternating = generateAlternatingNested(size)
        val start3 = System.nanoTime()
        val dnf3 = DNFTransformer(alternating)
        val time3 = (System.nanoTime() - start3) / 1e6
        println(f"  Alternating operations: ${time3}%.2f ms")
      }
    }
  }
  
  /**
   * Educational step-by-step transformation examples
   */
  object StepByStepExamples {
    
    /**
     * Shows step-by-step transformation of a complex expression
     */
    def demonstrateStepByStep(): Unit = {
      println("\n=== Step-by-Step Transformation ===")
      
      // Complex expression with all types of operations
      val expr = and(
        and(variable("A"), not(variable("B"))),
        or(variable("C"), or(variable("D"), variable("E")))
      )
      
      println(s"Original expression: $expr")
      println("\nStep-by-step transformation:")
      
      val transformer = new DNFTransformer()
      
      // Show simple step-by-step transformation
      println(s"1. Original: $expr")
      
      val dnfResult = transformer.toDNF(expr)
      println(s"2. Final DNF: $dnfResult")
      
      println(s"\nIs in DNF: ${transformer.isDNF(dnfResult)}")
      
      // Verify correctness
      val isCorrect = TruthTableGenerator.verifyDNF(expr, dnfResult)
      println(s"Transformation correct: $isCorrect")
    }
  }
  
  /**
   * Runs all example demonstrations
   */
  def runAllExamples(): Unit = {
    println("DNF Transformation Algorithm - Comprehensive Examples")
    println("====================================================")
    
    SimpleExamples.demonstrateSimpleExamples()
    ComplexExamples.demonstrateComplexExamples()
    RealWorldExamples.demonstrateRealWorldExamples()
    StepByStepExamples.demonstrateStepByStep()
    PerformanceExamples.performanceTest()
    
    println("\n=== Summary ===")
    println("All examples completed successfully!")
    println("The DNF transformation algorithm can handle:")
    println("- Simple boolean operations (AND, OR, NOT)")
            println("- Complex operations (AND, OR, NOT)")
    println("- Deeply nested expressions")
    println("- Real-world logical scenarios")
    println("- Performance-intensive transformations")
  }
}

/**
 * Interactive testing utilities
 */
object DNFTester {
  
  /**
   * Tests a string expression
   */
  def testExpression(exprString: String): Unit = {
    println(s"\nTesting: $exprString")
    
    BooleanExpressionParser.parse(exprString) match {
      case Right(expr) =>
        val analysis = TruthTableGenerator.analyze(expr)
        println(analysis.prettyPrint)
      case Left(error) =>
        println(s"Parse error: $error")
    }
  }
  
  /**
   * Compares two expressions for equivalence
   */
  def compareExpressions(expr1String: String, expr2String: String): Unit = {
    println(s"\nComparing:")
    println(s"  Expression 1: $expr1String")
    println(s"  Expression 2: $expr2String")
    
    val result = for {
      expr1 <- BooleanExpressionParser.parse(expr1String)
      expr2 <- BooleanExpressionParser.parse(expr2String)
    } yield {
      val equivalent = TruthTableGenerator.areEquivalent(expr1, expr2)
      println(s"  Equivalent: $equivalent")
      
      if (!equivalent) {
        println("  Truth tables:")
        val table1 = TruthTableGenerator(expr1)
        val table2 = TruthTableGenerator(expr2)
        println(s"    Expression 1:\n${table1.prettyPrint}")
        println(s"    Expression 2:\n${table2.prettyPrint}")
      }
    }
    
    result match {
      case Left(error) => println(s"Error: $error")
      case Right(_) => // Success, output already printed
    }
  }
  
  /**
   * Batch test multiple expressions
   */
  def batchTest(expressions: List[String]): Unit = {
    println("\n=== Batch Testing ===")
    expressions.zipWithIndex.foreach { case (expr, index) =>
      println(s"\nTest ${index + 1}:")
      testExpression(expr)
    }
  }
} 