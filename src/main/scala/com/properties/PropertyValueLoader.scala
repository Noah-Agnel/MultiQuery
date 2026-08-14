package com.properties

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._

/**
 * Loads actual property values for WHERE-clause evaluation.
 *
 * Node/edge properties live in an EAV table (`node_static_props`/`edge_static_props`):
 * one row per (id, property_name), with the value in whichever typed column applies.
 * This pivots the properties a query actually needs into one column per property,
 * keyed by id, so they can be joined onto candidate ids for filtering.
 *
 * N.B. only static properties are read here. Dynamic (time-varying) properties have
 * `from`/`to` validity windows with no "as of" semantics defined anywhere in WhereClause
 * yet, so including them would mean silently picking an arbitrary value per id.
 */
object PropertyValueLoader {

  def loadNodeProperties(
      spark        : SparkSession,
      dbName       : String,
      propertyTypes: Map[String, PropertyType]
  ): DataFrame = load(spark, s"$dbName.node_static_props", "node_id", propertyTypes)

  def loadEdgeProperties(
      spark        : SparkSession,
      dbName       : String,
      propertyTypes: Map[String, PropertyType]
  ): DataFrame = load(spark, s"$dbName.edge_static_props", "edge_id", propertyTypes)

  /**
   * Loads properties whose type isn't known statically (e.g. cross-variable conditions,
   * which carry no declared type - see VariableCondition). Produces two columns per
   * requested property ("<name>__string" and "<name>__numeric") instead of one, so the
   * caller can pick whichever side actually has a value.
   */
  def loadNodePropertiesUntyped(
      spark        : SparkSession,
      dbName       : String,
      propertyNames: Set[String]
  ): DataFrame = loadUntyped(spark, s"$dbName.node_static_props", "node_id", propertyNames)

  def loadEdgePropertiesUntyped(
      spark        : SparkSession,
      dbName       : String,
      propertyNames: Set[String]
  ): DataFrame = loadUntyped(spark, s"$dbName.edge_static_props", "edge_id", propertyNames)

  private def loadUntyped(
      spark        : SparkSession,
      table        : String,
      idColumn     : String,
      propertyNames: Set[String]
  ): DataFrame = {
    require(propertyNames.nonEmpty, "Must request at least one property")

    val propsDF = spark.table(table)
      .filter(col("property_name").isin(propertyNames.toSeq: _*))

    val pivotedColumns = propertyNames.toSeq.flatMap { propName =>
      Seq(
        first(when(col("property_name") === propName, col("string_value")), ignoreNulls = true).as(s"${propName}__string"),
        first(when(col("property_name") === propName, col("numeric_value")), ignoreNulls = true).as(s"${propName}__numeric")
      )
    }

    propsDF.groupBy(col(idColumn)).agg(pivotedColumns.head, pivotedColumns.tail: _*)
  }

  private def load(
      spark        : SparkSession,
      table        : String,
      idColumn     : String,
      propertyTypes: Map[String, PropertyType]
  ): DataFrame = {
    require(propertyTypes.nonEmpty, "Must request at least one property")

    val propsDF = spark.table(table)
      .filter(col("property_name").isin(propertyTypes.keys.toSeq: _*))

    val pivotedColumns = propertyTypes.map { case (propName, propType) =>
      val valueColumn = valueColumnFor(propType)
      first(when(col("property_name") === propName, col(valueColumn)), ignoreNulls = true).as(propName)
    }.toSeq

    propsDF.groupBy(col(idColumn)).agg(pivotedColumns.head, pivotedColumns.tail: _*)
  }

  private def valueColumnFor(propType: PropertyType): String = propType match {
    case PropertyType.StringType       => "string_value"
    case PropertyType.IntegerType      => "numeric_value"
    case PropertyType.DoubleType       => "numeric_value"
    case PropertyType.StringArrayType  => "string_values"
    case PropertyType.IntegerArrayType => "numeric_values"
    case PropertyType.DoubleArrayType  => "numeric_values"
  }
}
