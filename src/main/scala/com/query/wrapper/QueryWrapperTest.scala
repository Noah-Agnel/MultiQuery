package com.query.wrapper

import com.query.QueryStructure

object QueryWrapperTest extends App {

    def testQuery(description: String, query: String): Unit = {
        println("\n" + "=" * 60)
        println(s"TEST: $description")
        println(s"QUERY: $query")
        println("=" * 60)

        CypherQueryWrapper.convert(query) match {
            case Right(qs) =>
                println("--- QUERY STRUCTURE ---")
                println(qs.toString)
            case Left(err) =>
                println(s"ERROR: $err")
        }
    }

    // Test 1 - simple integer condition
    testQuery(
        "Simple integer condition",
        "MATCH (n:Person) WHERE n.age > 25 RETURN n.age"
    )
    
    // Test 2 - simple string equality
    testQuery(
        "Simple string condition",
        "MATCH (n:Person) WHERE n.city = 'Paris' RETURN n"
    )

    // Test 3 - AND condition
    testQuery(
        "AND condition",
        "MATCH (n:Person) WHERE n.age > 25 AND n.city = 'Paris' RETURN n"
    )

    // Test 4 - OR condition
    testQuery(
        "OR condition",
        "MATCH (n:Person) WHERE n.city = 'Paris' OR n.city = 'Lyon' RETURN n"
    )

    // Test 5 - AND with nested OR
    testQuery(
        "AND with nested OR - requires DNF",
        "MATCH (n:Person) WHERE n.age > 25 AND (n.city = 'Paris' OR n.city = 'Lyon') RETURN n"
    )

    // Test 6 - conditions on multiple nodes
    testQuery(
        "Conditions on multiple nodes",
        "MATCH (n:Person)-[r:KNOWS]->(m:Person) WHERE n.age > 25 AND m.city = 'Paris' RETURN n"
    )

    // Test 7 - complex WHERE
    testQuery(
        "Complex WHERE",
        """MATCH (n:Person)-[r:KNOWS]->(m:Person)
           WHERE n.age > 25 AND (m.city = 'Paris' OR m.city = 'Lyon') AND n.name = 'John'
           RETURN n"""
    )

    // Test 8 - no WHERE clause
    testQuery(
        "No WHERE clause",
        "MATCH (n:Person)-[r:KNOWS]->(m:Person) RETURN n"
    )
    
    println("\n" + "=" * 60)
    println("ALL TESTS COMPLETE")
    println("=" * 60)
}