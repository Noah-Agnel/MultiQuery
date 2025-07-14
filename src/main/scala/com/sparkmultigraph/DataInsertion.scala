import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types._
import org.apache.hadoop.fs.{FileSystem, Path}
import scala.collection.mutable
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.Column
import org.apache.spark.sql.Row
import org.apache.spark.sql.functions._
import org.apache.spark.sql.expressions.Window



object DataInsertionInTable {
    // ========================================================================================================================
    // CONSTANTS
    // ========================================================================================================================
    val SPK_HD_EP  :String   = "spark.hadoop.fs.s3a.endpoint"
    val SPK_HD_AK  :String   = "spark.hadoop.fs.s3a.access.key"
    val SPK_HD_SK  :String   = "spark.hadoop.fs.s3a.secret.key"
    val SPK_HD_PS  :String   = "spark.hadoop.fs.s3a.path.style.access"
    val SPK_HD_IMPL:String   = "spark.hadoop.fs.s3a.impl"
    val SPK_HD_SSL :String   = "spark.hadoop.fs.s3a.connection.ssl.enabled"


    // ========================================================================================================================
    // MAIN FUNCTION
    // ========================================================================================================================
    def main(args: Array[String]): Unit = {
        
        // Get database name from command line arguments
        // TODO manage it by args
        // val dbName = if (args.length > 0) args(0) else "graph_db"
        val dbName = "terrorism"

        // Logging
        println(s"Inserting data into database: $dbName")
    
        // Create Spark Session with Iceberg support
        val spark = SparkSession.builder()
           .appName("Network Loading")
           .config(SPK_HD_EP,   "http://minio:9000")
           .config(SPK_HD_AK,   sys.env("AWS_ACCESS_KEY_ID"))
           .config(SPK_HD_SK,   sys.env("AWS_SECRET_ACCESS_KEY"))
           .config(SPK_HD_PS,   "true")
           .config(SPK_HD_IMPL, "org.apache.hadoop.fs.s3a.S3AFileSystem")
           .config(SPK_HD_SSL,  "false")
           .getOrCreate();

        // Nodes and Edges dataframe creation
        val filesMap = pathsReadingfromMinio("terrorismnetworkfile")
        val nodesDF  = elementsDataframeCreation(filesMap("nodes" ))
        var edgesDF  = elementsDataframeCreation(filesMap("edges" ))  

        // Static nodes properties
        // TODO: I'm a test, so i need to be completed
        nodesDF("static").foreach{ case (ds_type, ds) => {
            val staticPropsSchema = ds.schema("static_props").dataType.asInstanceOf[StructType]
            val fieldMappings     = fieldMappingCreation(ds, "static_props")

            var fieldDFs = staticNodePropsDSCreation(ds, fieldMappings)
            if (!fieldDFs.isEmpty){
                println(ds_type)
                val maxIdOption = spark.sql("SELECT MAX(snproperty_id) as max_id FROM iceberg.terrorism.node_static_props").head()
                val maxId       = Option(row.get(0)).map(_.asInstanceOf[Long]).getOrElse(0L)
                val startId     = maxId + 1
                fieldDFs        = fieldDFs.withColumn("row_num", monotonically_increasing_id())
                fieldDFs        = fieldDFs
                    .withColumn("snproperty_id", (row_number().over(Window.orderBy("row_num")) + startId - 1).cast(LongType))
                    .withColumn("created_at", current_timestamp())
                    .select(
                       "snproperty_id",
                       "node_id", 
                       "property_name",
                       "string_value",
                       "numeric_value",
                       "datetime_value",
                       "string_values",
                       "numeric_values", 
                       "datetime_values",
                       "created_at",
                       "is_active"
                    )
                
                fieldDFs.write
                   .mode("append")
                   .insertInto("terrorism.node_static_props")

                println(s"Inserted ${fieldDFs.count()} rows into iceberg.terrorism.node_static_props")  
            }  
        }}
    }


    // ========================================================================================================================
    // 1. PATH READING FROM MINIO
    // ========================================================================================================================
    def pathsReadingfromMinio(bucketName:String): mutable.Map[String, mutable.Map[String, Array[String]]] = {
        // Filesystem configuration
        val conf = spark.sparkContext.hadoopConfiguration
        val path = new Path(s"s3a://${bucketName}/")
        val fs   = FileSystem.get(path.toUri, conf)

        // Files name reading
        val filesPath = fs.listStatus(path).map(path => path.getPath)

        // Map configuration
        val filesMap = mutable.Map.empty[String, mutable.Map[String, Array[String]]]
        filesMap    += (
            "nodes" -> mutable.Map.empty[String, Array[String]],
            "edges" -> mutable.Map.empty[String, Array[String]]
        )

        // Map population
        filesPath.foreach(filePath => {
            val filePathStr         = filePath.toString
            val fileNameComponents  = filePathStr.split("/").last.split("_")
            val partitionNumber     = fileNameComponents.last.split("\\.")(0)
            val key                 = fileNameComponents(0) + "_" + fileNameComponents(1) + "_" + partitionNumber    
            val elemType            = if(filePathStr.contains("node")) "nodes" else "edges"
                
            if (!filesMap(elemType).contains(key))
                filesMap(elemType) += (key -> Array.empty)
        
            filesMap(elemType).update(key, filesMap(elemType)(key):+filePathStr)    
        })

        return filesMap
    }

    // ========================================================================================================================
    // 2. ELEMENTS DATAFRAME CREATION
    // ========================================================================================================================
    def elementsDataframeCreation(
        elements: mutable.Map[String, Array[String]]
    ): mutable.Map[String, mutable.Map[String, Dataset[Row]]] = { 

        //     key_1            key_2         value
        // property_type -> element_type -> dataframe
        var elementsDF: mutable.Map[String, mutable.Map[String, Dataset[Row]]] = mutable.Map.empty
        
        elements.foreach {case (key, paths) => {
            paths.foreach(path => {
                println("Reading path: " + path)
                val pathFileName  = path.split("/").last
                val mainNameParts = pathFileName.split("_")
                
                // 1. WE ARE DEFINING THE element_type and property one
                val mapKey        = mainNameParts.slice(2, mainNameParts.length - 1).mkString("_")
                val propType      = if (path.contains("static")) "static" else "dynamic"

                // 2. WE ADD THE PROPERTY TYPE IF NOT EXIST
                if (!elementsDF.contains(propType))
                    elementsDF += (propType -> mutable.Map.empty)
                
                // 3. WE ADD THE NODE TYPE IF NOT EXIST
                if (!elementsDF(propType).contains(mapKey))
                    elementsDF(propType) += (mapKey -> null)
                    
                // 4. DATAFRAME READING FROM MINIO
                val elementDF = spark.read.option("multiline","true").json(path)
                
                // 5. IF IT IS THE FIRST DATAFRAME, WE ASSIGN
                if(elementsDF(propType)(mapKey) == null)
                    elementsDF(propType).update(
                        mapKey, 
                        elementDF
                    )
                    
                // 6. OTHERWISE WE APPEND IT 
                else
                    elementsDF(propType).update(
                        mapKey, 
                        elementsDF(propType)(mapKey)
                            .union(elementDF)
                            .dropDuplicates()
                    )
            })
        }}

        return elementsDF
    }

    // ========================================================================================================================
    // 3. FIELD MAPPING CREATION
    // ========================================================================================================================
    def fieldMappingCreation(dset:Dataset[Row], cname:String): Array[(String, String)] = {
        val staticPropsSchema = dset.schema(cname).dataType.asInstanceOf[StructType]
        return staticPropsSchema.fields.map { field =>
            val sparkType     = field.dataType match {
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
    // 4. NEW COLUMNS CONFIGURATION
    // ========================================================================================================================
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
    // 5. STATIC NODE PROPERTIES DATAFRAME CREATION
    // ========================================================================================================================
    def staticNodePropsDSCreation(dset:Dataset[Row], fieldMappings: Array[(String, String)]): Dataset[Row] = {
        val fieldDfs   = fieldMappings.map { case (fieldName, fieldType) => {
            var baseDF = dset.select(
                col("node_id"  ),
                col("labels"   ),
                col("is_active"),
                lit(fieldName                   ).as("property_name"),
                col(s"static_props.${fieldName}").as("property_value")
            ).filter(col("property_value").isNotNull)
        
            fieldType match {
                case "STRING"          => baseDF = newColumnsConfiguration(baseDF, str_val  = col("property_value"))
                case "DOUBLE"          => baseDF = newColumnsConfiguration(baseDF, num_val  = col("property_value"))
                case "TIMESTAMP"       => baseDF = newColumnsConfiguration(baseDF, dat_val  = col("property_value")) 
                case "ARRAY_STRING"    => baseDF = newColumnsConfiguration(baseDF, astr_val = col("property_value"))
                case "ARRAY_DOUBLE"    => baseDF = newColumnsConfiguration(baseDF, anum_val = col("property_value"))
                case "ARRAY_TIMESTAMP" => baseDF = newColumnsConfiguration(baseDF, adat_val = col("property_value"))
                case _                 => baseDF = newColumnsConfiguration(baseDF, str_val  = col("property_value"))
            }
        
            baseDF.select(
            col("node_id"        ),
            col("is_active"      ),
            col("property_name"  ),
            col("string_value"   ),
            col("numeric_value"  ),
            col("datetime_value" ),
            col("string_values"  ),
            col("numeric_values" ),
            col("datetime_values")
            )
        }}

        return fieldDfs.reduce(_ union _).orderBy(asc("node_id"))
    }
}