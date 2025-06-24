package com.sparkmultigraph
import  org.apache.spark.sql.SparkSession


object DropIcebergTables {

  // Environment
  // Keys
  val SPK_EXTENS = "spark.sql.extensions"
  val SPK_CATALG = "spark.sql.catalog.spark_catalog"
  val SPK_CAT_TP = "spark.sql.catalog.spark_catalog.type"
  val SPK_CAT_UR = "spark.sql.catalog.spark_catalog.uri"
  val SPK_CAT_EP = "spark.sql.catalog.spark_catalog.s3.endpoint"
  val SPK_CAT_AK = "spark.sql.catalog.spark_catalog.s3.access-key"
  val SPK_CAT_SK = "spark.sql.catalog.spark_catalog.s3.secret-key"
  val SPK_CAT_WR = "spark.sql.catalog.spark_catalog.warehouse"
  val SPK_WRH_DR = "spark.sql.warehouse.dir"

  // Values
  val AWS_AKID   = sys.env("AWS_ACCESS_KEY_ID")
  val AWS_SAKY   = sys.env("AWS_SECRET_ACCESS_KEY")
  val CAT_URI    = "http://iceberg-rest:8181"
  val CAT_EPT    = "http://minio:9000"
  val CAT_WHS    = "s3://warehouse"

  //Main function
  def main(args: Array[String]): Unit = {
    
    // Get database name from command line arguments
    val dbName = if (args.length > 0) args(0) else "graph_db"
    
    println(s"Dropping Iceberg tables in database: $dbName")
    
    // Create Spark Session with Iceberg support
    val spark = SparkSession.builder()
      .appName("Drop Iceberg Tables")
      .config(SPK_EXTENS, "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions")
      .config(SPK_CATALG, "org.apache.iceberg.spark.SparkSessionCatalog")
      .config(SPK_CAT_TP, "rest")
      .config(SPK_CAT_UR, CAT_URI )
      .config(SPK_CAT_EP, CAT_EPT )
      .config(SPK_CAT_AK, AWS_AKID)
      .config(SPK_CAT_SK, AWS_SAKY)
      .config(SPK_CAT_WR, CAT_WHS)
      .config(SPK_WRH_DR, "/home/iceberg/warehouse")
      // Additional S3 configurations for MinIO compatibility
      .config("spark.sql.catalog.spark_catalog.s3.path-style-access", "true")
      .config("spark.hadoop.fs.s3a.endpoint", CAT_EPT)
      .config("spark.hadoop.fs.s3a.access.key", AWS_AKID)
      .config("spark.hadoop.fs.s3a.secret.key", AWS_SAKY)
      .config("spark.hadoop.fs.s3a.path.style.access", "true")
      .config("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
      .getOrCreate()

    try {
      // Check if database exists
      val databases = spark.sql("SHOW DATABASES").collect()
      val dbExists = databases.exists(_.getString(0) == dbName)
      
      if (!dbExists) {
        println(s"Database $dbName does not exist!")
        return
      }
      
      spark.sql(s"USE $dbName")
      
      // List all tables in the database
      val tables = spark.sql(s"SHOW TABLES IN $dbName").collect()
      
      if (tables.isEmpty) {
        println(s"No tables found in database $dbName")
        return
      }
      
      println(s"Found ${tables.length} tables in $dbName:")
      tables.foreach(row => println(s"  - ${row.getString(1)}"))
      
      // Drop each table
      val tableNames = Array(
        "node_label_pair",
        "node_pair_matches", 
        "match_edges",
        "node_static_props",
        "node_dynamic_props",
        "edge_static_props",
        "edge_dynamic_props"
      )
      
      tableNames.foreach { tableName =>
        try {
          println(s"Dropping table $tableName...")
          spark.sql(s"DROP TABLE IF EXISTS $dbName.$tableName")
          println(s"Successfully dropped $tableName")
        } catch {
          case e: Exception =>
            println(s"Error dropping table $tableName: ${e.getMessage}")
        }
      }
      
      // Optionally drop the database if it's empty
      val remainingTables = spark.sql(s"SHOW TABLES IN $dbName").collect()
      if (remainingTables.isEmpty) {
        println(s"Dropping empty database $dbName...")
        spark.sql(s"DROP DATABASE $dbName")
        println(s"Successfully dropped database $dbName")
      }
      
    } catch {
      case e: Exception =>
        println(s"Error dropping tables: ${e.getMessage}")
        e.printStackTrace()
    } finally {
      spark.stop()
    }
  }
} 