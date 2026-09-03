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
      propertyTypes: Map[String, PropertyType],
      candidateIds : Option[DataFrame] = None
  ): DataFrame = load(spark, s"$dbName.node_static_props", "node_id", propertyTypes, candidateIds)

  def loadEdgeProperties(
      spark        : SparkSession,
      dbName       : String,
      propertyTypes: Map[String, PropertyType],
      candidateIds : Option[DataFrame] = None
  ): DataFrame = load(spark, s"$dbName.edge_static_props", "edge_id", propertyTypes, candidateIds)

  /**
   * Loads properties whose type isn't known statically (e.g. cross-variable conditions,
   * which carry no declared type - see VariableCondition). Produces two columns per
   * requested property ("<name>__string" and "<name>__numeric") instead of one, so the
   * caller can pick whichever side actually has a value.
   */
  def loadNodePropertiesUntyped(
      spark        : SparkSession,
      dbName       : String,
      propertyNames: Set[String],
      candidateIds : Option[DataFrame] = None
  ): DataFrame = loadUntyped(spark, s"$dbName.node_static_props", "node_id", propertyNames, candidateIds)

  def loadEdgePropertiesUntyped(
      spark        : SparkSession,
      dbName       : String,
      propertyNames: Set[String],
      candidateIds : Option[DataFrame] = None
  ): DataFrame = loadUntyped(spark, s"$dbName.edge_static_props", "edge_id", propertyNames, candidateIds)

  /**
   * Restricts `propsDF` to rows whose id is in `candidateIds`, via a broadcast join, before
   * any grouping/pivoting happens. When the caller already knows which ids it cares about
   * (e.g. the small set of node ids bound by a multijoin), this turns an aggregation over the
   * whole property table into one over just the relevant rows, and lets the later join onto
   * those ids be a broadcast join instead of a shuffle.
   */
  private def restrictToCandidates(propsDF: DataFrame, idColumn: String, candidateIds: Option[DataFrame]): DataFrame =
    candidateIds match {
      case Some(ids) => propsDF.join(broadcast(ids.withColumnRenamed("id", idColumn)), Seq(idColumn))
      case None       => propsDF
    }

  private def loadUntyped(
      spark        : SparkSession,
      table        : String,
      idColumn     : String,
      propertyNames: Set[String],
      candidateIds : Option[DataFrame]
  ): DataFrame = {
    require(propertyNames.nonEmpty, "Must request at least one property")

    val propsDF = restrictToCandidates(
      spark.table(table).filter(col("property_name").isin(propertyNames.toSeq: _*)),
      idColumn, candidateIds
    )

    if (propertyNames.size == 1) {
      val propName = propertyNames.head
      propsDF.select(
        col(idColumn),
        col("string_value").as(s"${propName}__string"),
        col("numeric_value").as(s"${propName}__numeric")
      )
    } else {
      val pivotedColumns = propertyNames.toSeq.flatMap { propName =>
        Seq(
          first(when(col("property_name") === propName, col("string_value")), ignoreNulls = true).as(s"${propName}__string"),
          first(when(col("property_name") === propName, col("numeric_value")), ignoreNulls = true).as(s"${propName}__numeric")
        )
      }
      propsDF.groupBy(col(idColumn)).agg(pivotedColumns.head, pivotedColumns.tail: _*)
    }
  }

  private def load(
      spark        : SparkSession,
      table        : String,
      idColumn     : String,
      propertyTypes: Map[String, PropertyType],
      candidateIds : Option[DataFrame]
  ): DataFrame = {
    require(propertyTypes.nonEmpty, "Must request at least one property")

    val propsDF = restrictToCandidates(
      spark.table(table).filter(col("property_name").isin(propertyTypes.keys.toSeq: _*)),
      idColumn, candidateIds
    )
    if (propertyTypes.size == 1) {
      val (propName, propType) = propertyTypes.head
      propsDF.select(
        col(idColumn),
        col(valueColumnFor(propType)).as(propName),
        col(arrayValueColumnFor(propType)).as(s"${propName}__array")
      )
    } else {
      val pivotedColumns = propertyTypes.flatMap { case (propName, propType) =>
        val valueColumn        = valueColumnFor(propType)
        val arrayFallbackColumn = arrayValueColumnFor(propType)
        Seq(
          first(when(col("property_name") === propName, col(valueColumn)), ignoreNulls = true).as(propName),
          first(when(col("property_name") === propName, col(arrayFallbackColumn)), ignoreNulls = true).as(s"${propName}__array")
        )
      }.toSeq
      propsDF.groupBy(col(idColumn)).agg(pivotedColumns.head, pivotedColumns.tail: _*)
    }
  }

  private def valueColumnFor(propType: PropertyType): String = propType match {
    case PropertyType.StringType       => "string_value"
    case PropertyType.IntegerType      => "numeric_value"
    case PropertyType.DoubleType       => "numeric_value"
    case PropertyType.StringArrayType  => "string_values"
    case PropertyType.IntegerArrayType => "numeric_values"
    case PropertyType.DoubleArrayType  => "numeric_values"
  }

  private def arrayValueColumnFor(propType: PropertyType): String = propType match {
    case PropertyType.StringType       => "string_values"
    case PropertyType.IntegerType      => "numeric_values"
    case PropertyType.DoubleType       => "numeric_values"
    case PropertyType.StringArrayType  => "string_values"
    case PropertyType.IntegerArrayType => "numeric_values"
    case PropertyType.DoubleArrayType  => "numeric_values"
  }
}
