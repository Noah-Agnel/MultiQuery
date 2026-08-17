package com.sparkmultigraph

import  java.io.File
import  com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import  com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import  com.amazonaws.services.s3.AmazonS3ClientBuilder


// One-off utility to push a local directory of node/edge JSON files into a MinIO bucket, since
// this environment has no aws/mc CLI installed. Only used to stage data for the Spark jobs that
// read from s3a:// -- not part of the regular ingestion path.
object UploadToMinio {
    def main(args: Array[String]): Unit = {
        if (args.length < 2) {
            System.err.println("You need to provide two arguments: <local_dir> <bucket_name>")
            System.exit(1)
        }
        uploadDir(args(0), args(1))
    }

    def uploadDir(localDir: String, bucketName: String): Unit = {
        val credentials = new BasicAWSCredentials(
            sys.env("AWS_ACCESS_KEY_ID"),
            sys.env("AWS_SECRET_ACCESS_KEY")
        )
        val s3 = AmazonS3ClientBuilder.standard()
            .withEndpointConfiguration(new EndpointConfiguration("http://localhost:9000", "us-east-1"))
            .withCredentials(new AWSStaticCredentialsProvider(credentials))
            .withPathStyleAccessEnabled(true)
            .build()

        if (!s3.doesBucketExistV2(bucketName)) {
            s3.createBucket(bucketName)
            println(s"Created bucket $bucketName")
        }

        val files = new File(localDir).listFiles().filter(_.getName.endsWith(".json"))
        files.zipWithIndex.foreach { case (f, i) =>
            s3.putObject(bucketName, f.getName, f)
            println(s"Uploaded ${i + 1}/${files.length}: ${f.getName}")
        }

        println(s"Done. Uploaded ${files.length} files to $bucketName.")
    }
}
