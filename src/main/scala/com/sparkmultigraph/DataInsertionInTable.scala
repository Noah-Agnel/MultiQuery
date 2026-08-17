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
import  com.sparkmultigraph.bitmatrix.{BitMatrixConfig, BitMatrixPopulator}


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
        val staticEdges        = edgesDF("static")("edges_static_props")
          .where(col("source_id") !== col("target_id"))
          .select("edge_id", "source_id", "target_id", "edge_type")

        val staticNodesLabels        = nodesDF("static")
            .values
            .map(_.select("node_id", "labels"))
            .reduce(_.union(_))

        val dynamicEdges       = edgesDF("dynamic")("edges_dynamic_props")
          .where(col("source_id") !== col("target_id"))
          .select("edge_id", "source_id", "target_id", "edge_type")

        val dynamicNodesLabels = nodesDF("dynamic")
            .values
            .map(_.select("node_id", "labels"))
            .reduce(_.union(_))

        // Combining static and dynamic nodes/edges
        val allEdges = staticEdges.union(dynamicEdges)
            .dropDuplicates("edge_id")
        val allNodesLabels = staticNodesLabels.union(dynamicNodesLabels)
            .dropDuplicates("node_id")

        // Matrix containing nodes and edges' labels and ids. pair_id is produced by a dense_rank
        // that starts at 1 on every run, so it's offset by the table's current max here -- the same
        // pattern already used below for snproperty_id/edge_id etc. -- otherwise a second ingestion
        // run collides its pair_ids with the first run's, silently merging unrelated edge-type
        // signatures together in node_label_pair/node_pair_matches/edges_matches.
        var nodesEdgesMatrix    = nodesAndEdgesLabelsAndIdsConfiguration(allNodesLabels, allEdges)
        val maxPairIdOption     = spark.sql(s"SELECT MAX(pair_id) as max_id FROM $dbName.node_label_pair").head()
        val maxPairId           = Option(maxPairIdOption.getAs[Long]("max_id")).getOrElse(0L)
        nodesEdgesMatrix        = nodesEdgesMatrix.withColumn("pair_id", col("pair_id") + lit(maxPairId))

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
        
        // --- Populate Statuc property DataFrames ---
        nodeStaticPropsTablePopulation(nodesDF("static"), dbName, spark)
        edgeStaticPropsTablePopulation(edgesDF("static"), dbName, spark)

        // --- Populate Dynamic property DataFrames ---
        nodeDynamicPropsTablePopulation(nodesDF("dynamic"), dbName, spark)
        edgeDynamicPropsTablePopulation(edgesDF("dynamic"), dbName, spark)

        // --- Populate Metadata DataFrames ---
        val metaNodeLabelsTab = metadataNodeLabelsPopulation(allNodesLabels)
        val metaEdgeTypesTab  = metadataEdgeTypesPopulation(allEdges)

        // --- Save Metadata Data ---
        metaNodeLabelsTab
          .write
          .mode("overwrite") // Or "append" depending on your batch strategy
          .insertInto(s"$dbName.metadata_node_labels")

        metaEdgeTypesTab
          .write
          .mode("overwrite")
          .insertInto(s"$dbName.metadata_edge_types")

        // --- Populate Target Bit Matrix table (so queries read it instead of recomputing it) ---
        val bitMatrixConfig = BitMatrixConfig.loadFromMetadata(spark, dbName)
        BitMatrixPopulator.populateTargetBitMatrixTable(spark, dbName, bitMatrixConfig)
    }
}