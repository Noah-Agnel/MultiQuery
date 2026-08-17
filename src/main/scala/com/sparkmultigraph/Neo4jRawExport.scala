package com.sparkmultigraph

import  java.io.File
import  org.apache.spark.sql.{SparkSession, DataFrame}
import  org.apache.spark.sql.types.{ArrayType, StructType}
import  org.apache.spark.sql.functions._
import  scala.collection.mutable
import  com.sparkconfiguration.SparkHandler._
import  com.sparkmultigraph.ReaderWriterHandler._


// Exports the raw node/edge JSON files straight out of a MinIO bucket (e.g. warehouse-small) into
// CSVs for a manual Neo4j import, bypassing the Iceberg tables entirely. Unlike Neo4jCsvExport this
// keeps every node type (including Address) and every edge, unfiltered and unsampled, so the graph
// in Neo4j is a 1:1 mirror of what's in the bucket.
object Neo4jRawExport {
    def main(args: Array[String]): Unit = {
        if (args.length < 1) {
            System.err.println("You need to provide one argument: <minio_bucket_name> [output_dir]")
            System.exit(1)
        }
        val bucketName = args(0)
        val outDir     = if (args.length > 1) args(1) else "neo4j_raw_export"

        val spark = spConfig("Neo4j Raw Export")

        val filesMap = pathsReadingFromMinio(spark, bucketName)
        val nodesDF  = dataframesCreation(spark, filesMap("nodes"))
        val edgesDF  = dataframesCreation(spark, filesMap("edges"))

        // node_id is kept in its raw, unprefixed form here so it can be matched against the raw
        // source_id/target_id in the edge files; the type prefix is only applied when a table is
        // actually written out, after buildNodeLabels/buildEdges are done matching on it.
        val nodeTables = buildNodeTables(nodesDF)
        val nodeLabels = buildNodeLabels(nodeTables)

        nodeTables.foreach { case (nodeType, df) =>
            // node_id is prefixed with its type before becoming the neo4j-admin :ID column, so a
            // single global ID space works across every node file even though the underlying ICIJ
            // node_ids aren't actually unique across types (Officers/Intermediaries collide ~1139
            // times) -- see the comment on buildNodeLabels below.
            val prefixed = df
                .withColumn("node_id", concat(col("type"), lit("_"), col("node_id")))
                .withColumnRenamed("node_id", "node_id:ID")
                .withColumnRenamed("type", ":LABEL")
            val outPath  = s"$outDir/nodes/$nodeType.csv"
            val written  = writeSingleCsv(prepareForCsv(prefixed), outPath)
            println(s"Wrote $written $nodeType nodes to $outPath")
        }

        val edges = buildEdges(edgesDF, nodeLabels)
            .withColumnRenamed("source_id", ":START_ID")
            .withColumnRenamed("target_id", ":END_ID")
            .withColumnRenamed("edge_type", ":TYPE")
        val writtenEdges = writeSingleCsv(prepareForCsv(edges), s"$outDir/edges.csv")
        println(s"Wrote $writtenEdges edges to $outDir/edges.csv")

        spark.stop()
    }

    // ========================================================================================================================
    // STRUCT FLATTENING
    // ========================================================================================================================
    // static_props/dynamic_props arrive as JSON structs; this pulls every field up to a top-level
    // column, joining any array-valued field (e.g. country_codes) with ";" so it survives a CSV round trip.
    def flattenStruct(df: DataFrame, structCol: String): DataFrame = {
        if (!df.columns.contains(structCol)) return df

        val fields = df.schema(structCol).dataType.asInstanceOf[StructType].fields
        val exprs  = fields.map { f =>
            f.dataType match {
                case ArrayType(_, _) => concat_ws(";", col(s"$structCol.${f.name}")).as(f.name)
                case _                => col(s"$structCol.${f.name}").as(f.name)
            }
        }
        df.select(df.columns.filterNot(_ == structCol).map(col) ++ exprs: _*)
    }

    // Casts any remaining array column (namely "labels") to a ";"-joined string so Spark's CSV writer
    // doesn't choke on it.
    def prepareForCsv(df: DataFrame): DataFrame = {
        df.schema.fields.foldLeft(df) { (acc, field) =>
            field.dataType match {
                case ArrayType(_, _) => acc.withColumn(field.name, concat_ws(";", col(field.name)))
                case _                => acc
            }
        }
    }

    // ========================================================================================================================
    // NODES
    // ========================================================================================================================
    // One DataFrame per node type (Address/Entity/Intermediary/Officer/...), combining that type's
    // static properties with whatever dynamic properties are currently valid (from <= now <= to).
    def buildNodeTables(nodesDF: mutable.Map[String, mutable.Map[String, DataFrame]]): Map[String, DataFrame] = {
        val staticByType  = nodesDF.getOrElse("static", mutable.Map.empty)
        val dynamicByType = nodesDF.getOrElse("dynamic", mutable.Map.empty)

        staticByType.map { case (key, staticDS) =>
            val nodeType = staticDS.select(col("labels")(0)).limit(1).collect().headOption
                .map(_.getString(0)).getOrElse(key)

            val staticFlat = flattenStruct(staticDS, "static_props")
                .withColumn("type", lit(nodeType))
                .drop("labels")

            val matchingDynamic = dynamicByType.collectFirst {
                case (dKey, ds) if ds.columns.contains("node_id") && sameEntity(key, dKey) => ds
            }

            val enriched = matchingDynamic match {
                case Some(dynamicDS) =>
                    val currentDynamic = flattenStruct(dynamicDS, "dynamic_props")
                        .filter((col("to").isNull || col("to") > current_timestamp()) && col("from") <= current_timestamp())
                        .drop("labels", "is_active")
                        .withColumnRenamed("from", "dynamic_from")
                        .withColumnRenamed("to", "dynamic_to")
                        .dropDuplicates("node_id")
                    staticFlat.join(currentDynamic, Seq("node_id"), "left")
                case None =>
                    staticFlat
            }

            nodeType -> enriched
        }.toMap
    }

    // Both keys are derived from the same "<...>_nodes_<partition>" filename pattern (e.g.
    // "entity_nodes_static_props" and "entity_nodes_dynamic_props"), so matching on the entity-type
    // prefix before "static"/"dynamic" pairs a type's static file with its dynamic one.
    def sameEntity(staticKey: String, dynamicKey: String): Boolean = {
        staticKey.split("static").head == dynamicKey.split("dynamic").head
    }

    // node_id -> type lookup used to label edge endpoints, built from the already-typed node tables.
    //
    // NOTE: raw ICIJ node_ids are not actually unique across types -- ~1,139 numeric ids are shared
    // between Officers and Intermediaries (e.g. plain "51122" exists as both), affecting ~53k of the
    // 1.5M edges. dropDuplicates("node_id") here silently keeps an arbitrary one of the colliding
    // rows, i.e. an arbitrary-but-deterministic type wins for those ids. This mirrors what the
    // Iceberg population path already does (DataInsertionInTable dedupes node labels the same way
    // before joining edges), so the resulting Neo4j graph stays consistent with what's in Iceberg --
    // but it means those ~53k edges' endpoint types are not reliably correct in either system.
    def buildNodeLabels(nodeTables: Map[String, DataFrame]): DataFrame = {
        nodeTables.values
            .map(_.select("node_id", "type"))
            .reduce(_.union(_))
            .dropDuplicates("node_id")
    }

    // ========================================================================================================================
    // EDGES
    // ========================================================================================================================
    // Every edge, static properties merged with whatever dynamic properties are currently valid, with
    // endpoints resolved to their node type and ids rewritten to match the prefixed node_id used above.
    def buildEdges(
        edgesDF    : mutable.Map[String, mutable.Map[String, DataFrame]],
        nodeLabels : DataFrame
    ): DataFrame = {
        val staticDS  = edgesDF("static").values.reduce(_.union(_))
        val staticFlat = flattenStruct(staticDS, "static_props")

        val enriched = edgesDF.get("dynamic").map(_.values.reduce(_.union(_))) match {
            case Some(dynamicDS) =>
                val currentDynamic = flattenStruct(dynamicDS, "dynamic_props")
                    .filter((col("to").isNull || col("to") > current_timestamp()) && col("from") <= current_timestamp())
                    .drop("source_id", "target_id", "edge_type")
                    .withColumnRenamed("from", "dynamic_from")
                    .withColumnRenamed("to", "dynamic_to")
                    .dropDuplicates("edge_id")
                staticFlat.join(currentDynamic, Seq("edge_id"), "left")
            case None =>
                staticFlat
        }

        val withEndpointTypes = enriched
            .join(nodeLabels.withColumnRenamed("node_id", "source_id").withColumnRenamed("type", "source_type"), Seq("source_id"), "left")
            .join(nodeLabels.withColumnRenamed("node_id", "target_id").withColumnRenamed("type", "target_type"), Seq("target_id"), "left")
            .withColumn("source_id", concat(coalesce(col("source_type"), lit("Unknown")), lit("_"), col("source_id")))
            .withColumn("target_id", concat(coalesce(col("target_type"), lit("Unknown")), lit("_"), col("target_id")))

        withEndpointTypes.select(
            Seq("edge_id", "source_id", "target_id", "source_type", "target_type", "edge_type").map(col) ++
            withEndpointTypes.columns.filterNot(Set("edge_id", "source_id", "target_id", "source_type", "target_type", "edge_type")).map(col): _*
        )
    }

    // ========================================================================================================================
    // SINGLE-FILE CSV WRITER
    // ========================================================================================================================
    // Spark's CSV writer always produces a directory of part-files; this coalesces to one partition
    // and renames Spark's generated part file to the exact path the caller asked for.
    def writeSingleCsv(df: DataFrame, outPath: String): Long = {
        val tmpDir = outPath + "_tmp"
        // Spark's CSV writer defaults to backslash-escaping embedded quotes ( \" ), which is not
        // RFC4180 and neo4j-admin's strict CSV parser rejects it. Forcing escape="\"" makes Spark
        // double embedded quotes ("") instead, which both RFC4180 and neo4j-admin expect.
        df.coalesce(1).write.mode("overwrite").option("header", "true").option("escape", "\"").csv(tmpDir)

        val tmpFolder = new File(tmpDir)
        val partFile  = tmpFolder.listFiles().find(_.getName.startsWith("part-"))
        val destFile  = new File(outPath)
        Option(destFile.getParentFile).foreach(_.mkdirs())
        partFile.foreach(f => java.nio.file.Files.move(f.toPath, destFile.toPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING))

        def deleteRecursively(f: File): Unit = {
            if (f.isDirectory) f.listFiles().foreach(deleteRecursively)
            f.delete()
        }
        deleteRecursively(tmpFolder)

        df.count()
    }
}
