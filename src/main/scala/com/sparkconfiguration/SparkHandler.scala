package com.sparkconfiguration
import  org.apache.spark.sql.SparkSession


object SparkHandler {
    // ========================================================================================================================
    // CONSTANTS
    // ========================================================================================================================
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
        .config(SPK_HD_EDP, "http://minio:9000")
        .config(SPK_HD_ACK, sys.env("AWS_ACCESS_KEY_ID"))
        .config(SPK_HD_SKY, sys.env("AWS_SECRET_ACCESS_KEY"))
        .config(SPK_HD_PSA, "true")
        .config(SPK_HD_IMP, "org.apache.hadoop.fs.s3a.S3AFileSystem")
        .config(SPK_HD_SSL, "false")
        .getOrCreate()

        return spark
    }
}