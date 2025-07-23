package com.sparkmultigraph
import  org.apache.spark.sql.SparkSession
import  org.apache.spark.sql.types._
import  org.apache.hadoop.fs.{FileSystem, Path}
import  scala.collection.mutable
import  org.apache.spark.sql.Dataset
import  org.apache.spark.sql.Column
import  org.apache.spark.sql.Row
import  org.apache.spark.sql.functions._
import  org.apache.spark.sql.expressions.Window
import  com.sparkconfiguration.SparkHandler._
import  com.sparkmultigraph.TablesPopulationHandler._
import  com.sparkmultigraph.ReaderWriterHandler._


object DataInsertionInTable {
    // ========================================================================================================================
    // MAIN FUNCTION
    // ========================================================================================================================
    def main(args: Array[String]): Unit = {
        if (args.length < 2){
            System.err.println("You need to provide two arguments: <database_name> <file_warehouse_name>")
            System.exit(1)
        }

        // Get database name from command line arguments
        val dbName             = args(0)
        val fileWarehouseName  = args(1)
        
        // Create Spark Session with Iceberg support
        val spark              = spConfig("Data Insertion in Table")

        // Nodes and Edges data reading
        val filesMap           = pathsReadingFromMinio(spark, fileWarehouseName)
        val nodesDF            = dataframesCreation(spark, filesMap("nodes"))
        val edgesDF            = dataframesCreation(spark, filesMap("edges"))     
        val staticEdges        = edgesDF("static")("edges_static_props").where(col("source_id") !== col("target_id"))
        val nodesLabels        = nodesDF("static")
            .values
            .map(_.select("node_id", "labels"))
            .reduce(_.union(_))

        // Matrix containing nodes and edges' labels and ids
        val nodesEdgesMatrix   = nodesAndEdgesLabelsAndIdsConfiguration(nodesLabels, staticEdges)

        // Node label pair table creation
        val nodeLabelPairTab   = nodeLabelPairPopulation(nodesEdgesMatrix)

        // Node pair matches table creation
        val nodePairMatchesTab = nodePairMatchesPopulation(nodesEdgesMatrix)

        // Edges matches table creation
        val edgesMatchesTab    = matchEdgesPopulation(nodesEdgesMatrix)

        // Saving tables data
        nodeLabelPairTab
          .write
          .mode("append")
          .insertInto(s"$dbName.node_label_pair")
        nodePairMatchesTab
          .write
          .mode("append")
          .insertInto(s"$dbName.node_pair_matches")
        edgesMatchesTab
          .write
          .mode("append")
          .insertInto(s"$dbName.edges_matches")
        
        nodeStaticPropsTablePopulation(nodesDF("static"), dbName, spark)
        edgeStaticPropsTablePopulation(edgesDF("static"), dbName, spark)

        // TODO continue dynamic props
    }
}