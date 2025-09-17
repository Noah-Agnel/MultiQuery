/**
 * Main Application for DNF Transformation Algorithm
 * 
 * This is the main entry point that demonstrates the capabilities of the
 * DNF transformation algorithm. It provides an interactive interface
 * and runs comprehensive examples.
 */

package dnf

/**
 * Main application object
 */
object Main extends App {
  
  println("=============================================================")
  println("          Boolean Expression to DNF Transformation          ")
  println("                    Comprehensive Solution                  ")
  println("=============================================================")
  
  // Run the comprehensive examples
  DNFExamples.runAllExamples()
  
  println("\n=== Interactive Demo ===")
  interactiveDemo()
  
  /**
   * Interactive demonstration of the DNF algorithm
   */
  def interactiveDemo(): Unit = {
    // Define some test expressions
    val testExpressions = List(
      "A & B",
      "A | B",
      "!(A & B)",
      "(A & B) | (C | D)"
    )
    
    println("\nTesting various boolean expressions:")
    testExpressions.foreach { expr =>
      println(s"\n" + "="*60)
      testSingleExpression(expr)
    }
    
    // Test parser with different syntax variations
    println(s"\n" + "="*60)
    println("Testing different syntax variations:")
    testSyntaxVariations()
    
    // Performance demonstration
    println(s"\n" + "="*60)
    println("Performance demonstration:")
    performanceDemo()
  }
  
  /**
   * Tests a single expression with full analysis
   */
  def testSingleExpression(exprString: String): Unit = {
    println(s"Expression: $exprString")
    
    BooleanExpressionParser.parseWithDetails(exprString) match {
      case ParseResult.Success(expr) =>
        val transformer = new DNFTransformer()
        val dnf         = transformer.toDNF(expr)
        
        println(s"Original:    $expr")
        println(s"DNF:         $dnf")
        println(s"Already DNF: ${transformer.isDNF(expr)}")
        
        // Generate truth table analysis
        val analysis = TruthTableGenerator.analyze(expr)
        println(s"Variables:   ${analysis.truthTable.variables.mkString(", ")}")
        println(s"Satisfying:  ${analysis.satisfyingAssignments}/${analysis.totalAssignments} assignments")
        
        if (analysis.isTautology)
          println("Type:       Tautology (always true)")
        else if (analysis.isContradiction) 
          println("Type:       Contradiction (always false)")
        else
          println("Type:       Contingent (sometimes true, sometimes false)")
        
        // Verify transformation correctness
        val isCorrect = TruthTableGenerator.verifyDNF(expr, dnf)
        println(s"Correct:    $isCorrect")
        
        // Show truth table for small expressions
        if (analysis.truthTable.variables.length <= 3) {
          println("\nTruth Table:")
          println(analysis.truthTable.prettyPrint)
        }
        
      case ParseResult.Error(message, line, column, input) =>
        println(s"Parse Error: $message at line $line, column $column")
    }
  }
  
  /**
   * Tests different syntax variations
   */
  def testSyntaxVariations(): Unit = {
    val variations = List(
      ("Standard symbols", "A & B | C"),
      ("Unicode symbols", "A ∧ B ∨ C"), 
      ("Word operators", "A AND B OR C"),
      ("Mixed syntax", "A & (B or C)"),
      ("Negation variations", "!A & ~B & ¬C")
    )
    
    variations.foreach { case (description, expr) =>
      println(s"\n$description: $expr")
      BooleanExpressionParser.parse(expr) match {
        case Right(parsed) =>
          val dnf = DNFTransformer(parsed)
          println(s"  Parsed as: $parsed")
          println(s"  DNF:       $dnf")
        case Left(error) =>
          println(s"  Error: $error")
      }
    }
  }
  
  /**
   * Demonstrates performance characteristics
   */
  def performanceDemo(): Unit = {
    println("Testing performance with increasingly complex expressions...")
    
    // Generate complex expressions of different sizes
    val sizes = List(4, 6, 8, 10)
    
    sizes.foreach { size =>
      println(s"\nTesting with $size variables:")
      
      // Create a complex expression: (A1 & A2) & (A2 & A3) & ... & (An-1 & An)
      val vars = (1 to size).map(i => BooleanExpression.variable(s"A$i"))
      val conjunctions = vars.zip(vars.tail).map { case (a, b) => 
        BooleanExpression.and(a, b) 
      }
      val complexExpr = conjunctions.reduceLeft(BooleanExpression.and)
      
      val startTime = System.nanoTime()
      val dnf = DNFTransformer(complexExpr)
      val endTime = System.nanoTime()
      val duration = (endTime - startTime) / 1e6
      
      println(f"  Original length: ${complexExpr.toString.length}%d characters")
      println(f"  DNF length:      ${dnf.toString.length}%d characters")
      println(f"  Transform time:  ${duration}%.2f ms")
      
      // Analyze the result
      val truthTable = TruthTableGenerator(complexExpr)
      println(f"  Truth table:     ${truthTable.rows.length}%d rows, ${truthTable.numSatisfyingAssignments}%d satisfying")
    }
  }
  
  /**
   * Utility method to create separator lines
   */
  def separator(char: Char = '=', length: Int = 60): String = {
    char.toString * length
  }
}

/**
 * Command-line interface for the DNF transformer
 */
object CLIRunner {
  
  /**
   * Processes command line arguments and runs appropriate operations
   */
  def run(args: Array[String]): Unit = {
    if (args.isEmpty) {
      printUsage()
      return
    }
    
    args(0) match {
      case "--help" | "-h" => printUsage()
      case "--parse" | "-p" => 
        if (args.length > 1) parseExpression(args(1))
        else println("Error: No expression provided")
      case "--dnf" | "-d" => 
        if (args.length > 1) transformToDNF(args(1))
        else println("Error: No expression provided")
      case "--analyze" | "-a" => 
        if (args.length > 1) analyzeExpression(args(1))
        else println("Error: No expression provided")
      case "--compare" | "-c" => 
        if (args.length > 2) compareExpressions(args(1), args(2))
        else println("Error: Need two expressions to compare")
      case "--examples" | "-e" => DNFExamples.runAllExamples()
      case expr => transformToDNF(expr) // Default: treat as expression
    }
  }
  
  /**
   * Prints usage information
   */
  def printUsage(): Unit = {
    println("""
      |DNF Transformation Tool
      |Usage: scala Main [options] [expression]
      |
      |Options:
      |  --help, -h                  Show this help message
      |  --parse, -p <expr>          Parse and display expression
      |  --dnf, -d <expr>            Transform expression to DNF
      |  --analyze, -a <expr>        Full analysis of expression
      |  --compare, -c <expr1> <expr2> Compare two expressions
      |  --examples, -e              Run all examples
      |
      |Expression syntax:
      |  Variables:       A, B, C, x1, x2, etc.
      |  Constants:       TRUE, FALSE, true, false, T, F
      |  Negation:        !, ~, ¬, NOT
      |  Conjunction:     &, ∧, AND
      |  Disjunction:     |, ∨, OR

      |  Parentheses:     ( )
      |
      |Examples:
      |  scala Main "A & B"
      |  scala Main --dnf "A & (B | C)"
      |  scala Main --analyze "!(A & B) | (C & D)"
      |  scala Main --compare "A & B" "A | B"
      |""".stripMargin)
  }
  
  /**
   * Parses and displays an expression
   */
  def parseExpression(exprString: String): Unit = {
    BooleanExpressionParser.parseWithDetails(exprString) match {
      case ParseResult.Success(expr) =>
        println(s"Expression: $exprString")
        println(s"Parsed as:  $expr")
        println(s"Variables:  ${expr.variables.mkString(", ")}")
      case ParseResult.Error(message, line, column, input) =>
        println(s"Parse Error: $message")
        println(ParseResult.Error(message, line, column, input).prettyPrint)
    }
  }
  
  /**
   * Transforms an expression to DNF
   */
  def transformToDNF(exprString: String): Unit = {
    BooleanExpressionParser.parse(exprString) match {
      case Right(expr) =>
        println(s"Original: $expr")
        val dnf = DNFTransformer(expr)
        println(s"DNF:      $dnf")
        
        val isCorrect = TruthTableGenerator.verifyDNF(expr, dnf)
        println(s"Verified: $isCorrect")
        
      case Left(error) =>
        println(s"Parse Error: $error")
    }
  }
  
  /**
   * Provides full analysis of an expression
   */
  def analyzeExpression(exprString: String): Unit = {
    BooleanExpressionParser.parse(exprString) match {
      case Right(expr) =>
        val analysis = TruthTableGenerator.analyze(expr)
        println(analysis.prettyPrint)
        
      case Left(error) =>
        println(s"Parse Error: $error")
    }
  }
  
  /**
   * Compares two expressions for equivalence
   */
  def compareExpressions(expr1String: String, expr2String: String): Unit = {
    val result = for {
      expr1 <- BooleanExpressionParser.parse(expr1String)
      expr2 <- BooleanExpressionParser.parse(expr2String)
    } yield {
      println(s"Expression 1: $expr1")
      println(s"Expression 2: $expr2")
      
      val equivalent = TruthTableGenerator.areEquivalent(expr1, expr2)
      println(s"Equivalent:   $equivalent")
      
      if (!equivalent) {
        println("\nTruth tables differ:")
        val table1 = TruthTableGenerator(expr1)
        val table2 = TruthTableGenerator(expr2)
        println("Expression 1:")
        println(table1.prettyPrint)
        println("\nExpression 2:")
        println(table2.prettyPrint)
      }
    }
    
    result match {
      case Left(error) => println(s"Parse Error: $error")
      case Right(_) => // Success, output already printed
    }
  }
} 