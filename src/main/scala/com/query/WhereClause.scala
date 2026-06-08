package com.query

import dnf.BooleanExpression
import com.properties.Property 

class WhereClause {
    // Attributes for the WhereClause structure
    private var expression : Option[BooleanExpression]          = None
    private var conditions : Map[String, (String, Property[_])] = Map.empty[String, (String, Property[_])]

    // =============================== CONSTRUCTORS ================================
    def this(expression: BooleanExpression, conditions: Map[String, (String, Property[_])]) = {
        this()
        this.expression = Some(expression)
        this.conditions = conditions
    }

    // =============================== GETTERS AND SETTERS ================================
    def getExpression: Option[BooleanExpression] = expression
    def getConditions: Map[String, (String, Property[_])] = conditions

    def setExpression(expression: Option[BooleanExpression]): Unit = {
        this.expression = expression
    }
    
    def setConditions(conditions: Map[String, (String, Property[_])]): Unit = {
        this.conditions = conditions
    }

    // =============================== OTHER METHODS ================================
    def addCondition(variable: String, nodeOrEdgeName: String, condition: Property[_]): Unit = {
        this.conditions = this.conditions + (variable -> (nodeOrEdgeName, condition))
    }

    def removeCondition(variable: String): Unit = {
        this.conditions = this.conditions - variable
    }
}