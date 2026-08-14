package com.query

import dnf.BooleanExpression
import com.properties.Condition

class WhereClause {
    // Attributes for the WhereClause structure
    private var expression : Option[BooleanExpression] = None
    private var conditions : Map[String, Condition]    = Map.empty[String, Condition]

    // =============================== CONSTRUCTORS ================================
    def this(expression: BooleanExpression, conditions: Map[String, Condition]) = {
        this()
        this.expression = Some(expression)
        this.conditions = conditions
    }

    // =============================== GETTERS AND SETTERS ================================
    def getExpression: Option[BooleanExpression] = expression
    def getConditions: Map[String, Condition] = conditions

    def setExpression(expression: Option[BooleanExpression]): Unit = {
        this.expression = expression
    }

    def setConditions(conditions: Map[String, Condition]): Unit = {
        this.conditions = conditions
    }

    // =============================== OTHER METHODS ================================
    def addCondition(variable: String, condition: Condition): Unit = {
        this.conditions = this.conditions + (variable -> condition)
    }

    def removeCondition(variable: String): Unit = {
        this.conditions = this.conditions - variable
    }
}