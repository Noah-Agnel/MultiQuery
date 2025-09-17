name := "DNF-Transformer"

version := "1.0.0"

scalaVersion := "2.13.10"

// Dependencies
libraryDependencies ++= Seq(
  // Scala parser combinators for parsing boolean expressions
  "org.scala-lang.modules" %% "scala-parser-combinators" % "2.1.1",
  
  // ScalaTest for unit testing
  "org.scalatest" %% "scalatest" % "3.2.14" % Test,
  
  // ScalaCheck for property-based testing
  "org.scalatestplus" %% "scalacheck-1-15" % "3.2.11.0" % Test
)

// Compiler options
scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Xfatal-warnings",
  "-deprecation"
)

// Main class for running the application
Compile / mainClass := Some("dnf.Main")

// Assembly plugin settings for creating fat JARs
assembly / assemblyJarName := "dnf-transformer.jar"
assembly / mainClass := Some("dnf.Main")

// Test settings
Test / parallelExecution := false
Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oD")

// Console settings
console / initialCommands := """
  |import dnf._
  |import dnf.BooleanExpression._
  |
  |// Quick access to main components
  |val parser = BooleanExpressionParser
  |val transformer = new DNFTransformer()
  |val truthTable = TruthTableGenerator
  |
  |// Helper function for quick testing
  |def test(expr: String) = {
  |  parser.parse(expr) match {
  |    case Right(e) => 
  |      println(s"Original: $e")
  |      val dnf = transformer.toDNF(e)
  |      println(s"DNF:      $dnf")
  |      println(s"Correct:  ${truthTable.verifyDNF(e, dnf)}")
  |    case Left(error) => println(s"Error: $error")
  |  }
  |}
  |
  |println("DNF Transformer Console - Type 'test(\"A & B\")' to get started")
  |""".stripMargin 