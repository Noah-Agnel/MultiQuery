package com.sparkconfiguration
import  org.apache.spark.sql.SparkSession


object SparkHandler {
    // ========================================================================================================================
    // CONSTANTS
    // ========================================================================================================================
    // Local warehouse path for the Iceberg Hadoop catalog. Adjust to wherever
    // you want table data actually written on disk.
    val LOCAL_WAREHOUSE: String = "file:///Users/noah/Desktop/MultiQuery/spark-warehouse"

    val SPK_MASTER      :String  = "local[*]"
    val ICEBERG_CATALOG :String  = "local"

    val SPK_SQL_EXT     :String  = "spark.sql.extensions"
    val SPK_DEF_CAT     :String  = "spark.sql.defaultCatalog"
    val SPK_CAT_IMPL    :String  = s"spark.sql.catalog.$ICEBERG_CATALOG"
    val SPK_CAT_TYPE    :String  = s"spark.sql.catalog.$ICEBERG_CATALOG.type"
    val SPK_CAT_WHOUSE  :String  = s"spark.sql.catalog.$ICEBERG_CATALOG.warehouse"

    // S3A filesystem config, used to read the raw node/edge JSON files from MinIO.
    // sbt run executes on the host, outside the docker network, so the endpoint
    // must be localhost rather than the "minio" service hostname.
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
            // NOTE: spark.driver.memory can't be set here -- in local[*] mode the driver IS
            // this already-running JVM, so its heap is fixed at process launch and Spark
            // ignores this config. The actual heap size is set via -Xmx in build.sbt's
            // `Compile / run / javaOptions` (effective because `run / fork := true`).
            .config("spark.driver.maxResultSize", "2g")
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

        // Suppress noisy Spark log output globally
        spark.sparkContext.setLogLevel("ERROR")

        return spark
    }
}