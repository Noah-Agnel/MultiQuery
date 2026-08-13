package com.sparkmultigraph

import  org.apache.spark.sql.SparkSession
import  org.apache.spark.sql.types._
import  org.apache.hadoop.fs.{FileSystem, Path}
import  scala.collection.mutable
import  org.apache.spark.sql.Dataset
import  org.apache.spark.sql.Column
import  org.apache.spark.sql.Row
import  org.apache.spark.sql.DataFrame
import  org.apache.spark.sql.functions._
import  org.apache.spark.sql.expressions.Window


object TablesPopulationHandler {
    // ========================================================================================================================
    // CONSTANTS
    // ========================================================================================================================
    val SRID: String = "source_id"
    val NDID: String = "node_id"
    val TAID: String = "target_id"
    val SNID: String = "source_node_id"
    val TNID: String = "target_node_id"
    val ELID: String = "edge_id"
    val LABL: String = "labels"
    val SRLB: String = "src_labels"
    val TALB: String = "dst_labels"
    val ELTP: String = "edge_type"
    val CRAT: String = "created_at"

    val MIND: String = "min_node"
    val MAND: String = "max_node"
    val MILB: String = "min_labels"
    val MALB: String = "max_labels"
    val ISAC: String = "is_active"
    val EOIL: String = "edge_out_id_list"
    val EOLS: String = "edge_out_list"
    val NMID: String = "nodes_match_id"
    val PAID: String = "pair_id"
    val E12T: String = "edge_1_2_types"
    val E21T: String = "edge_2_1_types"
    val EBIT: String = "edge_bi_types"
    val PRNM: String = "property_name"
    val PRVL: String = "property_value"
    val SNPI: String = "snproperty_id"
    val SEPI: String = "seproperty_id"
    val DNPI: String = "dnproperty_id"
    val DEPI: String = "deproperty_id"

    val STVL: String = "string_value"
    val NMLV: String = "numeric_value"
    val DTLV: String = "datetime_value"
    val STVS: String = "string_values"
    val NLVS: String = "numeric_values"
    val DTVS: String = "datetime_values"

    
    // ========================================================================================================================
    // FIELD MAPPING CREATION
    // ========================================================================================================================
    /**
     * Creates a mapping of field names to their corresponding Spark types.
     *
     * @param dset  the Dataset[Row] to create the mapping from
     * @param cname the name of the column to create the mapping from
     *
     * @return an Array[(String, String)] where the first element is the field name and the second element is the Spark type
    **/
    def fieldMappingCreation(
        dset  :Dataset[Row], 
        cname :String
    ): Array[(String, String)] = 
    {
        val staticPropsSchema  = dset.schema(cname).dataType.asInstanceOf[StructType]
        return staticPropsSchema.fields.map { field =>
            val sparkType      = field.dataType match {
                case StringType                  => "STRING"
                case DoubleType                  => "DOUBLE"
                case LongType                    => "DOUBLE"
                case IntegerType                 => "DOUBLE"
                case BooleanType                 => "STRING"
                case TimestampType               => "TIMESTAMP"
                case DateType                    => "TIMESTAMP"
                case ArrayType(StringType,    _) => "ARRAY_STRING"
                case ArrayType(DoubleType,    _) => "ARRAY_DOUBLE"
                case ArrayType(LongType,      _) => "ARRAY_DOUBLE"
                case ArrayType(IntegerType,   _) => "ARRAY_DOUBLE"
                case ArrayType(TimestampType, _) => "ARRAY_TIMESTAMP"
                case ArrayType(DateType,      _) => "ARRAY_TIMESTAMP"
                case other => other.toString 
            }
            (field.name, sparkType)
        }
    }


    // ========================================================================================================================
    // NEW COLUMNS CONFIGURATION
    // ========================================================================================================================
    /**
     * Adds new columns to a Dataset[Row] with specified values.
     *
     * @param dset the Dataset[Row] to add the new columns to
     * @param str_val the value for the string column
     * @param num_val the value for the numeric column
     * @param dat_val the value for the datetime column
     * @param astr_val the value for the array of string column
     * @param anum_val the value for the array of numeric column
     * @param adat_val the value for the array of datetime column
     * @return the Dataset[Row] with the new columns
    **/
    def newColumnsConfiguration(
        dset     : Dataset[Row],
        str_val  : Column = lit(null),
        num_val  : Column = lit(null), 
        dat_val  : Column = lit(null), 
        astr_val : Column = lit(null),
        anum_val : Column = lit(null),
        adat_val : Column = lit(null)
    ):Dataset[Row] = {
        return dset
            .withColumn(STVL, str_val.cast(StringType))
            .withColumn(NMLV, num_val.cast(DoubleType))
            .withColumn(DTLV, dat_val.cast(TimestampType))
            .withColumn(STVS, astr_val.cast(ArrayType(StringType)))
            .withColumn(NLVS, anum_val.cast(ArrayType(DoubleType)))
            .withColumn(DTVS, adat_val.cast(ArrayType(TimestampType)))
    }


    // ========================================================================================================================
    // NODES AND EDGES LABELS AND IDS CONFIGURATION
    // ========================================================================================================================
    /**
     * Generation of the tables that contains nodes and edges labels and ids
     *
     * @param nodesLabels the Dataset[Row] containing node labels and ids
     * @param edgesDS     the Dataset[Row] containing edge type and ids
     
     * @return the Dataset[Row] with configured labels and IDs
   **/
    def nodesAndEdgesLabelsAndIdsConfiguration(
        nodesLabels:Dataset[Row],
        edgesDS    :Dataset[Row]
    ):Dataset[Row] =
    {
        var nodesEdgesMatrix = edgesDS
           .join(nodesLabels.alias("src"), col(SRID) === col("src.node_id"))
           .join(nodesLabels.alias("tgt"), col(TAID) === col("tgt.node_id"))
           .select(
               col(SRID),
               col(TAID),
               col(ELID),
               col("src.labels").alias(SRLB),
               col("tgt.labels").alias(TALB),
               col(ELTP)
           )
           .withColumn(NMID, concat(col(SRID), lit("_"), col(TAID)))
           // WE GROUP BY THE TRUE (SOURCE, TARGET) DIRECTION FROM THE EDGE DATA
           // ITSELF, RATHER THAN CANONICALISING BY NODE ID, SO THAT EDGE
           // DIRECTIONALITY IS PRESERVED FOR DIRECTED QUERY MATCHING.
           .groupBy(NMID, SRID, TAID, SRLB, TALB)
           .agg(
                collect_list(col(ELTP)).alias(EOLS),
                collect_list(col(ELID)).alias(EOIL)
            )
            .withColumnRenamed(SRID, MIND)
            .withColumnRenamed(TAID, MAND)
            .withColumnRenamed(SRLB, MILB)
            .withColumnRenamed(TALB, MALB)
            .withColumn(CRAT, current_timestamp())

        // WE ARE ADDING THE ID BASED ON THE NODES AND EDGES TYPE
        val windowSpec = Window.partitionBy(MILB, MALB).orderBy(EOLS)
        nodesEdgesMatrix = nodesEdgesMatrix.withColumn(PAID, dense_rank().over(windowSpec))

        return nodesEdgesMatrix
    }


    // ========================================================================================================================
    // NODE LABEL PAIR CPOPULATION
    // ========================================================================================================================
    /**
     * Generation of the tables that contains only nodes and edges labels. The labels of each node
     * are within an array, while the edges type are within a map.
     *
     * @param nodesEdgesMatrix the Dataset[Row] containing nodes and edges labels and ids
     * @return the Dataset[Row] with configured labels. it is out new bitmatrix configuration.
    **/
    def nodeLabelPairPopulation(
        nodesEdgesMatrix:Dataset[Row]
    ):Dataset[Row] = 
    {
        val arrayToFrequencyMap = udf((arr: Seq[String]) => {
            if (arr == null || arr.isEmpty) Map.empty[String, Int]
            else                            arr.groupBy(identity).mapValues(_.size)
        })

        // Direction is now preserved at the row level: MILB/MALB are the true
        // src/dst labels for this pair_id, and EOLS holds the src->dst edge
        // types. The reverse direction (if it exists) is its own row with
        // MILB/MALB swapped, so edge_2_1_types is no longer populated here.
        val node_label_pair = nodesEdgesMatrix
            .select(PAID, MILB, MALB, EOLS, CRAT)
            .dropDuplicates()
            .withColumn(E12T, arrayToFrequencyMap(col(EOLS)))
            .withColumn(E21T, lit(null).cast(MapType(StringType, IntegerType)))
            .withColumn(EBIT, lit(null))
            .select(
                col(PAID), 
                col(MILB).as(SRLB),
                col(MALB).as(TALB),
                col(E12T),
                col(E21T),
                col(EBIT),
                col(CRAT)
            )
            
        return node_label_pair
    }


    // ========================================================================================================================
    // NODE PAIR MATCHES POPULATION
    // ========================================================================================================================
    /**
     * Generation of the tables that contains the id of the nodes of each pair, the pair id, 
     * and the node matching id.
     *
     * @param nodesEdgesMatrix the Dataset[Row] containing nodes and edges labels and ids
     * @return the Dataset[Row] with configured labels. it is out new bitmatrix configuration. 
    **/
    def nodePairMatchesPopulation(
        nodesEdgesMatrix:Dataset[Row]
    ):Dataset[Row] = 
    {
        val node_pair_matches = nodesEdgesMatrix
            .select(
                col(NMID),
                col(PAID), 
                col(MIND).alias(SNID), 
                col(MAND).alias(TNID), 
                col(CRAT)
            )
            .dropDuplicates()

        return node_pair_matches
    }


    // ========================================================================================================================
    // MATCH EDGES POPULATION
    // ========================================================================================================================
    def matchEdgesInOrOutPopulation(
        nodesEdgesMatrix:Dataset[Row],
        colNameId       :String,
        colNameType     :String
    ):Dataset[Row] = 
    {
        val edges_matches_x = nodesEdgesMatrix
            .filter(size(col(colNameType)) > 0)
            .select(
                col(NMID),
                col(PAID),
                explode(arrays_zip(col(colNameType), col(colNameId))).alias("zipped"),
                col(CRAT)
            )
            .select(
                col("zipped." + colNameId).as(ELID),
                col(PAID),
                col(NMID),
                col("zipped." + colNameType).as(ELTP),
                col(CRAT)
            )

        return edges_matches_x
    }

    /**
     * Generation of the tables that contains the id of the edges of each pair, the pair id, 
     * and the node matching id.
     *
     * @param nodesEdgesMatrix the Dataset[Row] containing nodes and edges labels and ids
     * @return the Dataset[Row] with configured labels. it is out new bitmatrix configuration.
    **/
    def matchEdgesPopulation(
        nodesEdgesMatrix:Dataset[Row]
    ):Dataset[Row] =
    {
        // Every edge now belongs to exactly one (true) direction group, so a
        // single explode over EOLS/EOIL captures every edge exactly once.
        return matchEdgesInOrOutPopulation(nodesEdgesMatrix, EOIL, EOLS)
    }


    // ========================================================================================================================
    // STATIC NODE PROPERTIES DATAFRAME CREATION
    // ========================================================================================================================
    def componentOfStaticNodePropsDSCreation(dset:Dataset[Row], fieldMappings: Array[(String, String)]): Dataset[Row] = {
        val fieldDfs   = fieldMappings.map { case (fieldName, fieldType) => {
            var baseDF = dset.select(
                col(NDID),
                col(LABL),
                col(ISAC),
                lit(fieldName                   ).as(PRNM),
                col(s"static_props.${fieldName}").as(PRVL)
            ).filter(col(PRVL).isNotNull)
        
            fieldType match {
                case "STRING"          => baseDF = newColumnsConfiguration(baseDF, str_val  = col(PRVL))
                case "DOUBLE"          => baseDF = newColumnsConfiguration(baseDF, num_val  = col(PRVL))
                case "TIMESTAMP"       => baseDF = newColumnsConfiguration(baseDF, dat_val  = col(PRVL)) 
                case "ARRAY_STRING"    => baseDF = newColumnsConfiguration(baseDF, astr_val = col(PRVL))
                case "ARRAY_DOUBLE"    => baseDF = newColumnsConfiguration(baseDF, anum_val = col(PRVL))
                case "ARRAY_TIMESTAMP" => baseDF = newColumnsConfiguration(baseDF, adat_val = col(PRVL))
                case _                 => baseDF = newColumnsConfiguration(baseDF, str_val  = col(PRVL))
            }
        
            baseDF.select(
                col(NDID),
                col(ISAC),
                col(PRNM),
                col(STVL),
                col(NMLV),
                col(DTLV),
                col(STVS),
                col(NLVS),
                col(DTVS)
            )}
        }
        return fieldDfs.reduce(_ union _).orderBy(asc(NDID))
    }


    /**
     * Populates the node static properties table.
     * 
     * @param  nodesDS the Dataset[Row] containing the nodes
     * @return the Dataset[Row] with the node static properties
    **/
    def nodeStaticPropsTablePopulation(
        nodesDS: mutable.Map[String, Dataset[Row]],
        dbName : String,
        spark  : SparkSession
    ): Unit = {
        nodesDS.filter{ case (_, ds) => ds.columns.contains("static_props") }.foreach{ case (ds_type, ds) => {
            val staticPropsSchema = ds.schema("static_props").dataType.asInstanceOf[StructType]
            val fieldMappings     = fieldMappingCreation(ds, "static_props")

            var fieldDFs = componentOfStaticNodePropsDSCreation(ds, fieldMappings)
            if (!fieldDFs.isEmpty){
                val maxIdOption = spark.sql(s"SELECT MAX($SNPI) as max_id FROM $dbName.node_static_props").head()
                val maxId       = Option(maxIdOption.getAs[Long]("max_id")).getOrElse(0L)
                val startId     = maxId + 1
                fieldDFs        = fieldDFs.withColumn("row_num", monotonically_increasing_id())
                fieldDFs        = fieldDFs
                    .withColumn(SNPI, (row_number().over(Window.orderBy("row_num")) + startId - 1).cast(LongType))
                    .withColumn(CRAT, current_timestamp())
                    .select(
                       col(SNPI),
                       col(NDID), 
                       col(PRNM),
                       col(STVL),
                       col(NMLV),
                       col(DTLV),
                       col(STVS),
                       col(NLVS),
                       col(DTVS),
                       col(CRAT),
                       col(ISAC)
                    )

                // Saving the data into the table
                fieldDFs.write
                   .mode("append")
                   .insertInto(s"$dbName.node_static_props")

                println(s"Inserted ${fieldDFs.count()} rows into $dbName.node_static_props")  
            }  
        }}
    }

    // ========================================================================================================================
    // DYNAMIC NODE PROPERTIES DATAFRAME CREATION
    // ========================================================================================================================

    def componentOfDynamicNodePropsDSCreation(dset:Dataset[Row], fieldMappings: Array[(String, String)]): Dataset[Row] = {
        val fieldDfs   = fieldMappings.map { case (fieldName, fieldType) => {
            var baseDF = dset.select(
                col(NDID),
                col(LABL),
                col(ISAC),
                col("from"),
                col("to"),
                lit(fieldName                   ).as(PRNM),
                col(s"dynamic_props.${fieldName}").as(PRVL)
            ).filter(col(PRVL).isNotNull)
        
            fieldType match {
                case "STRING"          => baseDF = newColumnsConfiguration(baseDF, str_val  = col(PRVL))
                case "DOUBLE"          => baseDF = newColumnsConfiguration(baseDF, num_val  = col(PRVL))
                case "TIMESTAMP"       => baseDF = newColumnsConfiguration(baseDF, dat_val  = col(PRVL)) 
                case "ARRAY_STRING"    => baseDF = newColumnsConfiguration(baseDF, astr_val = col(PRVL))
                case "ARRAY_DOUBLE"    => baseDF = newColumnsConfiguration(baseDF, anum_val = col(PRVL))
                case "ARRAY_TIMESTAMP" => baseDF = newColumnsConfiguration(baseDF, adat_val = col(PRVL))
                case _                 => baseDF = newColumnsConfiguration(baseDF, str_val  = col(PRVL))
            }
        
            baseDF.select(
                col(NDID),
                col(ISAC),
                col(PRNM),
                col(STVL),
                col(NMLV),
                col(DTLV),
                col(STVS),
                col(NLVS),
                col(DTVS),
                col("from").cast(TimestampType),
                col("to").cast(TimestampType)
            )}
        }
        return fieldDfs.reduce(_ union _).orderBy(asc(NDID))
    }


    /**
     * Populates the node dynamic properties table.
     * 
     * @param  nodesDS the Dataset[Row] containing the nodes
     * @return the Dataset[Row] with the node dynamic properties
    **/
    def nodeDynamicPropsTablePopulation(
        nodesDS: mutable.Map[String, Dataset[Row]],
        dbName : String,
        spark  : SparkSession
    ): Unit = {
        nodesDS.filter{ case (_, ds) => ds.columns.contains("dynamic_props") }.foreach{ case (ds_type, ds) => {
            val staticPropsSchema = ds.schema("dynamic_props").dataType.asInstanceOf[StructType]
            val fieldMappings     = fieldMappingCreation(ds, "dynamic_props")

            var fieldDFs = componentOfDynamicNodePropsDSCreation(ds, fieldMappings)
            if (!fieldDFs.isEmpty){
                val maxIdOption = spark.sql(s"SELECT MAX($DNPI) as max_id FROM $dbName.node_dynamic_props").head()
                val maxId       = Option(maxIdOption.getAs[Long]("max_id")).getOrElse(0L)
                val startId     = maxId + 1
                fieldDFs        = fieldDFs.withColumn("row_num", monotonically_increasing_id())
                fieldDFs        = fieldDFs
                    .withColumn(DNPI, (row_number().over(Window.orderBy("row_num")) + startId - 1).cast(LongType))
                    .withColumn(CRAT, current_timestamp())
                    .select(
                       col(DNPI),
                       col(NDID), 
                       col(PRNM),
                       col(STVL),
                       col(NMLV),
                       col(DTLV),
                       col(STVS),
                       col(NLVS),
                       col(DTVS),
                       col("from"),
                       col("to"),
                       col(CRAT)
                    )

                // Saving the data into the table
                fieldDFs.write
                   .mode("append")
                   .insertInto(s"$dbName.node_dynamic_props")

                println(s"Inserted ${fieldDFs.count()} rows into $dbName.node_dynamic_props")  
            }  
        }}
    }


    // ========================================================================================================================
    // STATIC EDGE PROPERTIES DATAFRAME CREATION
    // ========================================================================================================================
    def componentOfStaticEdgePropsDSCreation(dset:Dataset[Row], fieldMappings: Array[(String, String)]): Dataset[Row] = {
        val fieldDfs   = fieldMappings.map { case (fieldName, fieldType) => {
            var baseDF = dset.select(
                col(ELID),
                lit(fieldName                   ).as(PRNM),
                col(s"static_props.${fieldName}").as(PRVL)
            ).filter(col(PRVL).isNotNull)

            fieldType match {
                case "STRING"          => baseDF = newColumnsConfiguration(baseDF, str_val  = col(PRVL))
                case "DOUBLE"          => baseDF = newColumnsConfiguration(baseDF, num_val  = col(PRVL))
                case "TIMESTAMP"       => baseDF = newColumnsConfiguration(baseDF, dat_val  = col(PRVL)) 
                case "ARRAY_STRING"    => baseDF = newColumnsConfiguration(baseDF, astr_val = col(PRVL))
                case "ARRAY_DOUBLE"    => baseDF = newColumnsConfiguration(baseDF, anum_val = col(PRVL))
                case "ARRAY_TIMESTAMP" => baseDF = newColumnsConfiguration(baseDF, adat_val = col(PRVL))
                case _                 => baseDF = newColumnsConfiguration(baseDF, str_val  = col(PRVL))
            }

            baseDF.select(
                col(ELID),
                col(PRNM),
                col(STVL),
                col(NMLV),
                col(DTLV),
                col(STVS),
                col(NLVS),
                col(DTVS)
            )}
        }

        return fieldDfs.reduce(_ union _).orderBy(asc(ELID))
    }

    def edgeStaticPropsTablePopulation(
        edgesDS : mutable.Map[String, Dataset[Row]],
        dbName  : String,
        spark   : SparkSession
    ): Unit = {
        edgesDS.filter{ case (_, ds) => ds.columns.contains("static_props") }.foreach{ case (ds_type, ds) => {
            val staticPropsSchema = ds.schema("static_props").dataType.asInstanceOf[StructType]
            val fieldMappings     = fieldMappingCreation(ds, "static_props")

            var fieldDFs = componentOfStaticEdgePropsDSCreation(ds, fieldMappings)
            if (!fieldDFs.isEmpty){
                val maxIdOption = spark.sql(s"SELECT MAX($SEPI) as max_id FROM $dbName.edge_static_props").head()   
                val maxId       = Option(maxIdOption.getAs[Long]("max_id")).getOrElse(0L)
                val startId     = maxId + 1
                fieldDFs        = fieldDFs.withColumn("row_num", monotonically_increasing_id())
                fieldDFs        = fieldDFs
                    .withColumn(SEPI, (row_number().over(Window.orderBy("row_num")) + startId - 1).cast(LongType))
                    .withColumn(CRAT, current_timestamp())      
                    .select(
                        col(SEPI),
                        col(ELID),
                        col(PRNM),
                        col(STVL),
                        col(NMLV),
                        col(DTLV),
                        col(STVS),
                        col(NLVS),
                        col(DTVS),
                        col(CRAT)
                    )

                // Saving the data into the table
                fieldDFs.write
                   .mode("append")
                   .insertInto(s"$dbName.edge_static_props")

                println(s"Inserted ${fieldDFs.count()} rows into $dbName.edge_static_props")  
            }  
        }}
    }

    // ========================================================================================================================
    // DYNAMIC EDGE PROPERTIES DATAFRAME CREATION
    // ========================================================================================================================

    def componentOfDynamicEdgePropsDSCreation(dset:Dataset[Row], fieldMappings: Array[(String, String)]): Dataset[Row] = {
        val fieldDfs   = fieldMappings.map { case (fieldName, fieldType) => {
            var baseDF = dset.select(
                col(ELID),
                col("from"),
                col("to"),
                lit(fieldName                   ).as(PRNM),
                col(s"dynamic_props.${fieldName}").as(PRVL)
            ).filter(col(PRVL).isNotNull)

            fieldType match {
                case "STRING"          => baseDF = newColumnsConfiguration(baseDF, str_val  = col(PRVL))
                case "DOUBLE"          => baseDF = newColumnsConfiguration(baseDF, num_val  = col(PRVL))
                case "TIMESTAMP"       => baseDF = newColumnsConfiguration(baseDF, dat_val  = col(PRVL)) 
                case "ARRAY_STRING"    => baseDF = newColumnsConfiguration(baseDF, astr_val = col(PRVL))
                case "ARRAY_DOUBLE"    => baseDF = newColumnsConfiguration(baseDF, anum_val = col(PRVL))
                case "ARRAY_TIMESTAMP" => baseDF = newColumnsConfiguration(baseDF, adat_val = col(PRVL))
                case _                 => baseDF = newColumnsConfiguration(baseDF, str_val  = col(PRVL))
            }

            baseDF.select(
                col(ELID),
                col(PRNM),
                col(STVL),
                col(NMLV),
                col(DTLV),
                col(STVS),
                col(NLVS),
                col(DTVS),
                col("from").cast(TimestampType),
                col("to").cast(TimestampType)
            )}
        }

        return fieldDfs.reduce(_ union _).orderBy(asc(ELID))
    }


    def edgeDynamicPropsTablePopulation(
        edgesDS : mutable.Map[String, Dataset[Row]],
        dbName  : String,
        spark   : SparkSession
    ): Unit = {
        edgesDS.filter{ case (_, ds) => ds.columns.contains("dynamic_props") }.foreach{ case (ds_type, ds) => {
            val staticPropsSchema = ds.schema("dynamic_props").dataType.asInstanceOf[StructType]
            val fieldMappings     = fieldMappingCreation(ds, "dynamic_props")

            var fieldDFs = componentOfDynamicEdgePropsDSCreation(ds, fieldMappings)
            if (!fieldDFs.isEmpty){
                val maxIdOption = spark.sql(s"SELECT MAX($DEPI) as max_id FROM $dbName.edge_dynamic_props").head()   
                val maxId       = Option(maxIdOption.getAs[Long]("max_id")).getOrElse(0L)
                val startId     = maxId + 1
                fieldDFs        = fieldDFs.withColumn("row_num", monotonically_increasing_id())
                fieldDFs        = fieldDFs
                    .withColumn(DEPI, (row_number().over(Window.orderBy("row_num")) + startId - 1).cast(LongType))
                    .withColumn(CRAT, current_timestamp())      
                    .select(
                        col(DEPI),
                        col(ELID),
                        col(PRNM),
                        col(STVL),
                        col(NMLV),
                        col(DTLV),
                        col(STVS),
                        col(NLVS),
                        col(DTVS),
                        col("from"),
                        col("to"),
                        col(CRAT)
                    )

                // Saving the data into the table
                fieldDFs.write
                   .mode("append")
                   .insertInto(s"$dbName.edge_dynamic_props")

                println(s"Inserted ${fieldDFs.count()} rows into $dbName.edge_dynamic_props")  
            }  
        }
        }
    }

    // ========================================================================================================================
    // METADATA TABLES POPULATION
    // ========================================================================================================================


    def metadataNodeLabelsPopulation(nodesLabels: DataFrame): DataFrame = {
    val windowSpec = Window.orderBy("label")
    
    nodesLabels
        .select(explode(col("labels")).as("label"))
        .distinct()
        .withColumn("label_id", (row_number().over(windowSpec) - 1).cast("int"))
        .select("label_id", "label")
    }

    def metadataEdgeTypesPopulation(staticEdges: DataFrame): DataFrame = {
    val windowSpec = Window.orderBy("edge_type")
    
    staticEdges
        .select(col("edge_type"))
        .distinct()
        .withColumn("type_id", (row_number().over(windowSpec) - 1).cast("int"))
        .select("type_id", "edge_type")
    }
}   