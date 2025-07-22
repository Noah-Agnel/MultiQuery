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
    val ELIN: String = "edge_in"
    val ELOU: String = "edge_out"
    val EIIN: String = "edge_in_id"
    val EIOU: String = "edge_out_id"
    val ISMI: String = "is_source_min"
    val ISAC: String = "is_active"
    val EIIL: String = "edge_in_id_list"
    val EOIL: String = "edge_out_id_list"
    val EILS: String = "edge_in_list"
    val EOLS: String = "edge_out_list"
    val NMID: String = "nodes_match_id"
    val PAID: String = "pair_id"
    val E12T: String = "edge_1_2_types"
    val E21T: String = "edge_2_1_types"
    val EBIT: String = "edge_bi_types"
    val PRNM: String = "property_name"
    val PRVL: String = "property_value"
   
    
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
            .withColumn("string_value"    , str_val.cast(StringType))
            .withColumn("numeric_value"   , num_val.cast(DoubleType))
            .withColumn("datetime_value"  , dat_val.cast(TimestampType))
            .withColumn("string_values"   , astr_val.cast(ArrayType(StringType)))
            .withColumn("numeric_values"  , anum_val.cast(ArrayType(DoubleType)))
            .withColumn("datetime_values" , adat_val.cast(ArrayType(TimestampType)))
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
           .withColumn(ISMI, col(SRID) < col(TAID))
           // WE CONSIDER AS THE FIRST NODE THE ONE WITH THE MIN ID COMPARED 
           // TO THE SECOND ONE. IF THE MIN ID IS AS TARGET NODE THEN WE 
           // CONSIDER THE EDGES AS INCOMING, OTHERWISE AS OUTGOING.
           .select(
                when( col(ISMI), col(SRID)).otherwise(col(TAID)).alias(MIND),
                when( col(ISMI), col(TAID)).otherwise(col(SRID)).alias(MAND),
                when( col(ISMI), col(SRLB)).otherwise(col(TALB)).alias(MILB),
                when( col(ISMI), col(TALB)).otherwise(col(SRLB)).alias(MALB),
                when( col(ISMI), col(ELTP)).alias(ELIN),
                when( col(ISMI), col(ELID)).alias(EIIN),
                when(!col(ISMI), col(ELTP)).alias(ELOU),
                when(!col(ISMI), col(ELID)).alias(EIOU)
            )
            .withColumn(NMID, concat(col(MIND), lit("_"), col(MAND)))
            // WE GROIP IN ORDER TO CREATE ALL THE INCOMING AND OUTGOING EDGES
            // FOR EACH NODE PAIR.
            .groupBy(NMID, MIND, MAND, MILB, MALB)
            .agg(
                collect_list(when(col(ELIN).isNotNull, col(ELIN))).alias(EILS),
                collect_list(when(col(EIIN).isNotNull, col(EIIN))).alias(EIIL),
                collect_list(when(col(ELOU).isNotNull, col(ELOU))).alias(EOLS),
                collect_list(when(col(EIOU).isNotNull, col(EIOU))).alias(EOIL)
            )
            .withColumn(CRAT, current_timestamp())

        // WE ARE ADDING THE ID BASED ON THE NODES AND EDGES TYPE
        val windowSpec   = Window.orderBy(MILB, MALB, EILS, EOLS)
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

        // TODO YOU NEED TO MANAGE THE UNDIRECTED CASE
        val node_label_pair = nodesEdgesMatrix
            .select(PAID, MILB, MALB, EILS, EOLS, CRAT)
            .dropDuplicates()
            .withColumn(E12T, arrayToFrequencyMap(col(EILS)))
            .withColumn(E21T, arrayToFrequencyMap(col(EOLS)))
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
                explode(arrays_zip(col(colNameType), col(colNameId)))
                    .alias("zipped")
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
        val edges_matches_in  = matchEdgesInOrOutPopulation(nodesEdgesMatrix, EIIL, EILS)
        val edges_matches_out = matchEdgesInOrOutPopulation(nodesEdgesMatrix, EOIL, EOLS)

        return edges_matches_in.union(edges_matches_out)
    }


    // ========================================================================================================================
    // STATIC NODE PROPERTIES DATAFRAME CREATION
    // ========================================================================================================================
    def staticNodePropsDSCreation(dset:Dataset[Row], fieldMappings: Array[(String, String)]): Dataset[Row] = {
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
            col("string_value"   ),
            col("numeric_value"  ),
            col("datetime_value" ),
            col("string_values"  ),
            col("numeric_values" ),
            col("datetime_values")
            )
        }}

        return fieldDfs.reduce(_ union _).orderBy(asc(NDID))
    }
}   