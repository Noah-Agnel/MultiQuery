package com.sparkmultigraph
import  org.apache.spark.sql.SparkSession
import  com.sparkconfiguration.SparkHandler


object ListIcebergTables { 
  // Main function
  def main(args: Array[String]): Unit = {
    
    // Get database name from command line arguments
    val dbName = if (args.length > 0) args(0) else "graph_db"
    
    println(s"Listing Iceberg tables in database: $dbName")
    
    // Create Spark Session with Iceberg support
    val spark = SparkHandler.spConfig("List Iceberg Tables")


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
            
          } 
          catch {
            case e: Exception =>
                println(s"Error getting details for table $tableName: ${e.getMessage}")
          }
        }
      } 
      else {
         println(s"No tables found in database '$dbName'")
      }
      
    } 
    catch {
       case e: Exception =>
         println(s"Error listing tables: ${e.getMessage}")
         e.printStackTrace()
    } 
    finally {
      spark.stop()
    }
  }
} 