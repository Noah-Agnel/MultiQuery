package com.sparkmultigraph
import  org.apache.spark.sql.SparkSession



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

// Values
val AWS_AKID   = sys.env("AWS_ACCESS_KEY_ID")
val AWS_SAKY   = sys.env("AWS_SECRET_ACCESS_KEY")
val CAT_URI    = "http://iceberg-rest:8181"
val CAT_EPT    = "http://minio:9000"
val CAT_WHS    = "s3://warehouse"


object ListIcebergTables {
  def main(args: Array[String]): Unit = {
    
    // Get database name from command line arguments
    val dbName = if (args.length > 0) args(0) else "graph_db"
    
    println(s"Listing Iceberg tables in database: $dbName")
    
    // Create Spark Session with Iceberg support
    val spark = SparkSession.builder()
      .appName("Iceberg Tables Creation")
      .config(SPK_EXTENS, "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions")
      .config(SPK_CATALG, "org.apache.iceberg.spark.SparkSessionCatalog")
      .config(SPK_CAT_TP, "rest")
      .config(SPK_CAT_UR, CAT_URI )
      .config(SPK_CAT_EP, CAT_EPT )
      .config(SPK_CAT_AK, AWS_AKID)
      .config(SPK_CAT_SK, AWS_SAKY)
      .config(SPK_CAT_WR, CAT_WHS)
      .getOrCreate()


    try {
      // Show all databases
      println("Available databases:")
      spark.sql("SHOW DATABASES").show()
      
      // Check if specified database exists
      val databases = spark.sql("SHOW DATABASES").collect()
      val dbExists  = databases.exists(_.getString(0) == dbName)
      
      if (!dbExists) {
        println(s"Database $dbName does not exist!")
        return
      }
      
      // Show tables in the specified database
      println(s"\nTables in database '$dbName':")
      spark.sql(s"SHOW TABLES IN $dbName").show()
      
      // Get detailed information about each table
      val tables = spark.sql(s"SHOW TABLES IN $dbName").collect()
      
      if (tables.nonEmpty) {
        println(s"\nDetailed information for tables in '$dbName':")
        tables.foreach { row =>
          val tableName = row.getString(1)
          println(s"\n=== Table: $tableName ===")
          
          try {
            // Show table schema
            println("Schema:")
            spark.sql(s"DESCRIBE $dbName.$tableName").show()
            
            // Show table properties (if supported)
            println("Table Properties:")
            spark.sql(s"SHOW TBLPROPERTIES $dbName.$tableName").show()
            
            // Show row count
            val count = spark.sql(s"SELECT COUNT(*) as row_count FROM $dbName.$tableName").collect()
            println(s"Row count: ${count(0).getLong(0)}")
            
          } catch {
            case e: Exception =>
              println(s"Error getting details for table $tableName: ${e.getMessage}")
          }
        }
      } else {
        println(s"No tables found in database '$dbName'")
      }
      
    } catch {
      case e: Exception =>
        println(s"Error listing tables: ${e.getMessage}")
        e.printStackTrace()
    } finally {
      spark.stop()
    }
  }
} 