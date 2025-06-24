

object NetworkLoading {

    // Environment
    // Keys
    val SPK_HD_EP   = "spark.hadoop.fs.s3a.endpoint"
    val SPK_HD_AK   = "spark.hadoop.fs.s3a.access.key"
    val SPK_HD_SK   = "spark.hadoop.fs.s3a.secret.key"
    val SPK_HD_PS   = "spark.hadoop.fs.s3a.path.style.access"
    val SPK_HD_IMPL = "spark.hadoop.fs.s3a.impl"
    val SPK_HD_SSL  = "spark.hadoop.fs.s3a.connection.ssl.enabled"
      
    
    def main(args: Array[String]): Unit = {
       val spark = SparkSession.builder()
      .appName("Network Loading")
      .config(SPK_HD_EP,   "http://minio:9001")
      .config(SPK_HD_AK,   sys.env("AWS_ACCESS_KEY_ID"))
      .config(SPK_HD_SK,   sys.env("AWS_SECRET_ACCESS_KEY"))
      .config(SPK_HD_PS,   "true")
      .config(SPK_HD_IMPL, "org.apache.hadoop.fs.s3a.S3AFileSystem")
      .config(SPK_HD_SSL,  "false")
      .getOrCreate();
       
       // Examples of reading files from S3/MinIO buckets
       // Read JSON file from bucket
       val jsonDF = spark.read
         .json("s3a://terrorismnetworkfile/nodes_static_props_1.json")
       
       // Example: Show the data
       jsonDF.show(10)
       jsonDF.printSchema()
       
       spark.stop()
    }
}