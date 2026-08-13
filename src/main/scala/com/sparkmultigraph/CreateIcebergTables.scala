package com.sparkmultigraph
import  org.apache.spark.sql.SparkSession
import  org.apache.spark.sql.functions._
import  org.apache.spark.sql.DataFrame
import  com.sparkconfiguration.SparkHandler._


object CreateIcebergTables {
  // Main function
  def main(args: Array[String]): Unit = {
    
    // Get database name from command line arguments
    val dbName = if (args.length > 0) args(0) else "graph_db"
    
    println(s"Creating Iceberg tables in database: $dbName")
    
    // Create Spark Session with Iceberg support
    val spark = spConfig("Iceberg Tables Creation")

    try {
      // Create database if it doesn't exist
      spark.sql(s"CREATE DATABASE IF NOT EXISTS $dbName")
      spark.sql(s"USE $dbName")
      
      println(s"Successfully created/switched to database: $dbName")

      // Create the tables
      createNodeLabelPairTable(spark, dbName)
      createNodePairMatchesTable(spark, dbName)
      createMatchEdgesTable(spark, dbName)
      createNodeStaticPropsTable(spark, dbName)
      createNodeDynamicPropsTable(spark, dbName)
      createEdgeStaticPropsTable(spark, dbName)
      createEdgeDynamicPropsTable(spark, dbName)
      createMetadataNodeLabelsTable(spark, dbName)
      createMetadataEdgeTypesTable(spark, dbName)

      // Verify tables were created
      println(s"Successfully created tables in $dbName:")
      spark.sql(s"SHOW TABLES IN $dbName").show()
      
    } catch {
      case e: Exception =>
        println(s"Error creating tables: ${e.getMessage}")
        e.printStackTrace()
    } finally {
      spark.stop()
    }
  }
  
  private def createNodeLabelPairTable(spark: SparkSession, dbName: String): Unit = {
    println("Creating node_label_pair table...")
    spark.sql(
      s"""
        CREATE TABLE IF NOT EXISTS $dbName.node_label_pair (
          pair_id           BIGINT NOT NULL,          
          src_labels        ARRAY<STRING>,
          dst_labels        ARRAY<STRING>,
          edge_1_2_types    MAP<STRING, INT>,
          edge_2_1_types    MAP<STRING, INT>,
          edge_bi_types     MAP<STRING, INT>,
          created_at        TIMESTAMP NOT NULL
        ) USING ICEBERG
        PARTITIONED BY (
          bucket(16, pair_id)
        )
    """)
  }
  
  private def createNodePairMatchesTable(spark: SparkSession, dbName: String): Unit = {
    println("Creating node_pair_matches table...")
    spark.sql(
      s"""
        CREATE TABLE IF NOT EXISTS $dbName.node_pair_matches (
          nodes_match_id    STRING,
          pair_id           BIGINT NOT NULL,
          source_node_id    STRING,
          target_node_id    STRING,
          created_at        TIMESTAMP NOT NULL
        ) USING ICEBERG
        PARTITIONED BY (
          bucket(16, pair_id)
        )
      """)
  }
  
  private def createMatchEdgesTable(spark: SparkSession, dbName: String): Unit = {
    println("Creating match_edges table...")
    spark.sql(
      s"""
        CREATE TABLE IF NOT EXISTS $dbName.edges_matches (
          edge_id           STRING,
          pair_id           BIGINT NOT NULL,
          nodes_match_id    STRING,
          edge_type         STRING,
          created_at        TIMESTAMP NOT NULL
        ) USING ICEBERG
        PARTITIONED BY (
          bucket(16, pair_id)
        )
      """)
  }
  
  private def createNodeStaticPropsTable(spark: SparkSession, dbName: String): Unit = {
    println("Creating node_static_props table...")
    spark.sql(
      s"""
        CREATE TABLE IF NOT EXISTS $dbName.node_static_props (
          snproperty_id   BIGINT NOT NULL,
          node_id         STRING,
          property_name   STRING NOT NULL,
          string_value    STRING,
          numeric_value   DOUBLE,
          datetime_value  TIMESTAMP,
          string_values   ARRAY<STRING>,
          numeric_values  ARRAY<DOUBLE>,
          datetime_values ARRAY<TIMESTAMP>,
          created_at      TIMESTAMP NOT NULL,
          is_active       BOOLEAN
        ) USING ICEBERG
        PARTITIONED BY (
          bucket(16, node_id)
        )
        """
    )
  }
  
  private def createNodeDynamicPropsTable(spark: SparkSession, dbName: String): Unit = {
    println("Creating node_dynamic_props table...")
    spark.sql(
      s"""
        CREATE TABLE IF NOT EXISTS $dbName.node_dynamic_props (
          dnproperty_id   BIGINT NOT NULL,
          node_id         STRING,
          property_name   STRING NOT NULL,
          string_value    STRING,
          numeric_value   DOUBLE,
          datetime_value  TIMESTAMP,
          string_values   ARRAY<STRING>,
          numeric_values  ARRAY<DOUBLE>,
          datetime_values ARRAY<TIMESTAMP>,
          from            TIMESTAMP,
          to              TIMESTAMP,
          created_at      TIMESTAMP NOT NULL
        ) USING ICEBERG
        PARTITIONED BY (
          bucket(16, node_id)
        )
      """
    )
  }
  
  private def createEdgeStaticPropsTable(spark: SparkSession, dbName: String): Unit = {
    println("Creating edge_static_props table...")
    spark.sql(
      s"""
        CREATE TABLE IF NOT EXISTS $dbName.edge_static_props (
          seproperty_id   BIGINT NOT NULL,
          edge_id         STRING,
          property_name   STRING NOT NULL,
          string_value    STRING,
          numeric_value   DOUBLE,
          datetime_value  TIMESTAMP,
          string_values   ARRAY<STRING>,
          numeric_values  ARRAY<DOUBLE>,
          datetime_values ARRAY<TIMESTAMP>,
          created_at      TIMESTAMP NOT NULL
        ) USING ICEBERG
        PARTITIONED BY (
          bucket(16, edge_id)
        )
      """)
  }
  
  private def createEdgeDynamicPropsTable(spark: SparkSession, dbName: String): Unit = {
    println("Creating edge_dynamic_props table...")
    spark.sql(
      s"""
        CREATE TABLE IF NOT EXISTS $dbName.edge_dynamic_props (
          deproperty_id   BIGINT NOT NULL,
          edge_id         STRING,
          property_name   STRING NOT NULL,
          string_value    STRING,
          numeric_value   DOUBLE,
          datetime_value  TIMESTAMP,
          string_values   ARRAY<STRING>,
          numeric_values  ARRAY<DOUBLE>,
          datetime_values ARRAY<TIMESTAMP>,
          from            TIMESTAMP,
          to              TIMESTAMP,
          created_at      TIMESTAMP NOT NULL
        ) USING ICEBERG
        PARTITIONED BY (
          bucket(16, edge_id)
        )
      """
    )
  }

  private def createMetadataNodeLabelsTable(spark: SparkSession, dbName: String): Unit = {
    println("Creating metadata_node_labels table...")
    spark.sql(
      s"""
        CREATE TABLE IF NOT EXISTS $dbName.metadata_node_labels (
          label_id    INT NOT NULL,
          label       STRING
        ) USING ICEBERG
      """)
  }

  private def createMetadataEdgeTypesTable(spark: SparkSession, dbName: String): Unit = {
    println("Creating metadata_edge_types table...")
    spark.sql(
      s"""
        CREATE TABLE IF NOT EXISTS $dbName.metadata_edge_types (
          type_id     INT NOT NULL,
          edge_type   STRING
        ) USING ICEBERG
      """)
  }
} 