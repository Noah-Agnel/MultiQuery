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
      // Drop each table
      val tableNames = Array(
        "node_label_pair",
        "node_pair_matches", 
        "edges_matches",
        "node_static_props",
        "node_dynamic_props",
        "edge_static_props",
        "edge_dynamic_props",
        "metadata_node_labels",
        "metadata_edge_types"
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