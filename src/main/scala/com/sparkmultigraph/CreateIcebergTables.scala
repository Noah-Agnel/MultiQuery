package com.sparkmultigraph
import  org.apache.spark.sql.SparkSession



object CreateIcebergTables {
  // Main function
  def main(args: Array[String]): Unit = {
    
    // Get database name from command line arguments
    val dbName = if (args.length > 0) args(0) else "graph_db"
    
    println(s"Creating Iceberg tables in database: $dbName")
    
    // Create Spark Session with Iceberg support
    val spark = SparkSession.builder()
      .appName("Iceberg Tables Creation")
      .getOrCreate()

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
    spark.sql(s"""
      CREATE TABLE IF NOT EXISTS $dbName.node_label_pair (
        pair_id           BIGINT,
        pair_hash         STRING,
        src_labels        ARRAY<INT>,
        dst_labels        ARRAY<INT>,
        edge_1_2_types    MAP<INT, INT>,
        edge_2_1_types    MAP<INT, INT>,
        edge_bi_types     MAP<INT, INT>,
        created_at        TIMESTAMP
      ) USING ICEBERG
      PARTITIONED BY (
        bucket(32, pair_id)
      )
    """)
  }
  
  private def createNodePairMatchesTable(spark: SparkSession, dbName: String): Unit = {
    println("Creating node_pair_matches table...")
    spark.sql(s"""
      CREATE TABLE IF NOT EXISTS $dbName.node_pair_matches (
        match_id          BIGINT,
        pair_id           BIGINT,
        source_node_id    BIGINT,
        target_node_id    BIGINT,
        created_at        TIMESTAMP
      ) USING ICEBERG
      PARTITIONED BY (
        bucket(32, pair_id)
      )
    """)
  }
  
  private def createMatchEdgesTable(spark: SparkSession, dbName: String): Unit = {
    println("Creating match_edges table...")
    spark.sql(s"""
      CREATE TABLE IF NOT EXISTS $dbName.match_edges (
        match_edge_id     BIGINT,
        match_id          BIGINT,
        edge_id           BIGINT,
        created_at        TIMESTAMP
      ) USING ICEBERG
      PARTITIONED BY (
        bucket(32, match_id)
      )
    """)
  }
  
  private def createNodeStaticPropsTable(spark: SparkSession, dbName: String): Unit = {
    println("Creating node_static_props table...")
    spark.sql(s"""
      CREATE TABLE IF NOT EXISTS $dbName.node_static_props (
        node_id        BIGINT,
        names_values   MAP<STRING, STRING>,
        created_at     TIMESTAMP,
        is_active      BOOLEAN
      ) USING ICEBERG
      PARTITIONED BY (
        bucket(32, node_id)
      )
    """)
  }
  
  private def createNodeDynamicPropsTable(spark: SparkSession, dbName: String): Unit = {
    println("Creating node_dynamic_props table...")
    spark.sql(s"""
      CREATE TABLE IF NOT EXISTS $dbName.node_dynamic_props (
        node_id        BIGINT,
        name           STRING,
        value          STRING,
        from           TIMESTAMP,
        to             TIMESTAMP,
        created_at     TIMESTAMP
      ) USING ICEBERG
      PARTITIONED BY (
        bucket(32, node_id),
        name
      )
    """)
  }
  
  private def createEdgeStaticPropsTable(spark: SparkSession, dbName: String): Unit = {
    println("Creating edge_static_props table...")
    spark.sql(s"""
      CREATE TABLE IF NOT EXISTS $dbName.edge_static_props (
        edge_id        BIGINT,
        names_values   MAP<STRING, STRING>,
        from           TIMESTAMP,
        to             TIMESTAMP,
        created_at     TIMESTAMP
      ) USING ICEBERG
      PARTITIONED BY (
        bucket(32, edge_id)
      )
    """)
  }
  
  private def createEdgeDynamicPropsTable(spark: SparkSession, dbName: String): Unit = {
    println("Creating edge_dynamic_props table...")
    spark.sql(s"""
      CREATE TABLE IF NOT EXISTS $dbName.edge_dynamic_props (
        edge_id        BIGINT,
        name           STRING,
        value          STRING,
        from           TIMESTAMP,
        to             TIMESTAMP
      ) USING ICEBERG
      PARTITIONED BY (
        bucket(32, edge_id),
        name
      )
    """)
  }
} 