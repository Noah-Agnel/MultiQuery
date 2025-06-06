// This script is used to create the tables in the iceberg database
// You need to set the spark.app.db.name in the spark configuration
// in order to create the tables in the correct database


// Import for Spark SQL functions
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._


// Environment
val SPK_EXTENS = "spark.sql.extensions"
val SPK_CATALG = "spark.sql.catalog.spark_catalog"
val SPK_CAT_TP = "spark.sql.catalog.spark_catalog.type"
val SPK_CAT_UR = "spark.sql.catalog.spark_catalog.uri"
val SPK_CAT_EP = "spark.sql.catalog.spark_catalog.s3.endpoint"
val SPK_CAT_AK = "spark.sql.catalog.spark_catalog.s3.access-key"
val SPK_CAT_SK = "spark.sql.catalog.spark_catalog.s3.secret-key"
val SPK_CAT_WR = "spark.sql.catalog.spark_catalog.warehouse"

val AWS_AKID   = sys.env("AWS_ACCESS_KEY_ID")
val AWS_SAKY   = sys.env("AWS_SECRET_ACCESS_KEY")
val CAT_URI    = "http://iceberg-rest:8181"
val CAT_EPT    = "http://minio:9000"
val CAT_WHS    = "s3://warehouse"


// Get database name from spark configuration
val dbName     = spark.conf.get("spark.app.db.name", "graph_db")


// Create Spark Session with Iceberg support
val spark = SparkSession.builder()
  .appName("Iceberg Tables Creation")
  .config(SPK_EXTENS, "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions")
  .config(SPK_CATALG, "org.apache.iceberg.spark.SparkSessionCatalog")
  .config(SPK_CAT_TP, "rest")
  .config(SPK_CAT_UR, CAT_URI )
  .config(SPK_CAT_EP, CAT_EPT )
  .config(SPK_CAT_AK, AWS_AKID)
  .config(SPK_CAT_SK, AWS_SAKY)
  .config(SPK_CAT_WR, CAT_WHS)
  .getOrCreate()


// Create database if it doesn't exist
spark.sql(s"CREATE DATABASE IF NOT EXISTS $dbName")
spark.sql(s"USE $dbName")


println(s"Creating tables in database: $dbName")


// Create the tables
// Node Label Pair Table
spark.sql(s"""
  CREATE TABLE IF NOT EXISTS $dbName.node_label_pair (
    pair_id           BIGINT,
    pair_hash         STRING,
    src_labels        ARRAY<INT>,
    dst_labels        ARRAY<INT>,
    edge_1_2_types    MAP<INT, INT>,
    edge_2_1_types    MAP<INT, INT>,
    edge_bi_types     MAP<INT, INT>,
    created_at        TIMESTAMP
  ) USING ICEBERG
  PARTITIONED BY (
    bucket(32, pair_id)
  )
""")


// Node Pair Matches Table
spark.sql(s"""
  CREATE TABLE IF NOT EXISTS $dbName.node_pair_matches (
    match_id          BIGINT,
    pair_id           BIGINT,
    source_node_id    BIGINT,
    target_node_id    BIGINT,
    created_at        TIMESTAMP
  ) USING ICEBERG
  PARTITIONED BY (
    bucket(32, pair_id)
  )
""")


// Match Edges Table
spark.sql(s"""
  CREATE TABLE IF NOT EXISTS $dbName.match_edges (
    match_edge_id     BIGINT,
    match_id          BIGINT,
    edge_id           BIGINT,
    created_at        TIMESTAMP
  ) USING ICEBERG
  PARTITIONED BY (
    bucket(32, match_id)
  )
""")


// Node Static Properties Table 
spark.sql(s"""
  CREATE TABLE IF NOT EXISTS $dbName.node_static_props (
    node_id           BIGINT,
    names_values      MAP<STRING, STRING>,
    created_at        TIMESTAMP,
    is_active         BOOLEAN,
    from              TIMESTAMP,
    to                TIMESTAMP
  ) USING ICEBERG
  PARTITIONED BY (
    bucket(32, node_id)
  )
""")


// Node Dynamic Properties Table
spark.sql(s"""
  CREATE TABLE IF NOT EXISTS $dbName.node_dynamic_props (
    node_id           BIGINT,
    name              STRING,
    value             STRING,
    from              TIMESTAMP,
    to                TIMESTAMP,
    created_at        TIMESTAMP
  ) USING ICEBERG
  PARTITIONED BY (
    week(from),
    bucket(32, node_id),
    name
  )
""")


// Edge Static Properties Table
spark.sql(s"""
  CREATE TABLE IF NOT EXISTS $dbName.edge_static_props (
    edge_id           BIGINT,
    names_values      MAP<STRING, STRING>,
    from              TIMESTAMP,
    to                TIMESTAMP,
    created_at        TIMESTAMP
  ) USING ICEBERG
  PARTITIONED BY (
    bucket(32, edge_id)
  )
""")


// Edge Dynamic Properties Table
spark.sql(s"""
  CREATE TABLE IF NOT EXISTS $dbName.edge_dynamic_props (
    edge_id           BIGINT,
    name              STRING,
    value             STRING,
    from              TIMESTAMP,
    to                TIMESTAMP,
    created_at        TIMESTAMP
  ) USING ICEBERG
  PARTITIONED BY (
    week(from),
    bucket(32, edge_id),
    name
  )
""")


// Verify tables were created
println(s"Created tables in $dbName:")
spark.sql(s"SHOW TABLES IN $dbName").show()