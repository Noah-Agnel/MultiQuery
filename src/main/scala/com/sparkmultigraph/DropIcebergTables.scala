package com.sparkmultigraph
import  org.apache.spark.sql.SparkSession
import  com.sparkconfiguration.SparkHandler._



object DropIcebergTables {
  //Main function
  def main(args: Array[String]): Unit = {
    
    // Get database name from command line arguments
    val dbName = if (args.length > 0) args(0) else "graph_db"
    
    println(s"Dropping Iceberg tables in database: $dbName")
    
    // Create Spark Session with Iceberg support
    val spark  = spConfig("Drop Iceberg Tables")

    try {
      // Check if database exists
      val databases = spark.sql("SHOW DATABASES").collect()
      val dbExists  = databases.exists(_.getString(0) == dbName)
      
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
        "edges_matches",
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
        } 
        catch {
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
      
    } 
    catch {
      case e: Exception =>
        println(s"Error dropping tables: ${e.getMessage}")
        e.printStackTrace()
    } 
    finally {
      spark.stop()
    }
  }
} 