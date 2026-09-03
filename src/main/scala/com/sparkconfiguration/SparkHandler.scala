package com.sparkconfiguration
import  org.apache.spark.sql.SparkSession


object SparkHandler {
    // ========================================================================================================================
    // CONSTANTS
    // ========================================================================================================================
    // Local warehouse path for the Iceberg Hadoop catalog.
    val LOCAL_WAREHOUSE: String = "file:///Users/noah/Desktop/MultiQuery/spark-warehouse"

    val EVENT_LOG_DIR: String = "file:///Users/noah/Desktop/MultiQuery/spark-event-logs"

    val SPK_MASTER      :String  = "local[*]"
    val ICEBERG_CATALOG :String  = "local"

    val SPK_SQL_EXT     :String  = "spark.sql.extensions"
    val SPK_DEF_CAT     :String  = "spark.sql.defaultCatalog"
    val SPK_CAT_IMPL    :String  = s"spark.sql.catalog.$ICEBERG_CATALOG"
    val SPK_CAT_TYPE    :String  = s"spark.sql.catalog.$ICEBERG_CATALOG.type"
    val SPK_CAT_WHOUSE  :String  = s"spark.sql.catalog.$ICEBERG_CATALOG.warehouse"

    // S3A filesystem config, used to read the raw node/edge JSON files from MinIO.
    val SPK_HD_EDP  :String  = "spark.hadoop.fs.s3a.endpoint"
    val SPK_HD_ACK  :String  = "spark.hadoop.fs.s3a.access.key"
    val SPK_HD_SKY  :String  = "spark.hadoop.fs.s3a.secret.key"
    val SPK_HD_PSA  :String  = "spark.hadoop.fs.s3a.path.style.access"
    val SPK_HD_IMP  :String  = "spark.hadoop.fs.s3a.impl"
    val SPK_HD_SSL  :String  = "spark.hadoop.fs.s3a.connection.ssl.enabled"


    // ========================================================================================================================
    // SPARK CONFIGURATION
    // ========================================================================================================================
    def spConfig(appName: String): SparkSession = {
        val spark = SparkSession.builder()
            .appName(appName)
            .master(SPK_MASTER)
            .config("spark.driver.maxResultSize", "2g")
            .config("spark.eventLog.enabled", "true")
            .config("spark.eventLog.dir",     EVENT_LOG_DIR)
            .config(SPK_SQL_EXT,    "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions")
            .config(SPK_CAT_IMPL,   "org.apache.iceberg.spark.SparkCatalog")
            .config(SPK_CAT_TYPE,   "hadoop")
            .config(SPK_CAT_WHOUSE, LOCAL_WAREHOUSE)
            .config(SPK_DEF_CAT,    ICEBERG_CATALOG)
            .config(SPK_HD_EDP,     "http://localhost:9000")
            .config(SPK_HD_ACK,     sys.env("AWS_ACCESS_KEY_ID"))
            .config(SPK_HD_SKY,     sys.env("AWS_SECRET_ACCESS_KEY"))
            .config(SPK_HD_PSA,     "true")
            .config(SPK_HD_IMP,     "org.apache.hadoop.fs.s3a.S3AFileSystem")
            .config(SPK_HD_SSL,     "false")
            .getOrCreate()

        return spark
    }
}