# Building

We have several main classes in the same project. Here's how to use them:


<br><br >


# 1. Build the JAR file once:

```
sbt assembly
```

<br><br>

# 2. Submit different main classes to Spark:

# Create Tables

```
spark-submit \
  --class com.sparkmultigraph.CreateIcebergTables \
  --master spark://spark-iceberg:7077 \
  --driver-memory 12g \
  --executor-memory 12g \
  --executor-cores 3 \
  --total-executor-cores 6 \
  target/scala-2.12/create-iceberg-tables.jar \
  my_database_name
```


# List tables

```
spark-submit \
  --class com.sparkmultigraph.ListIcebergTables \
  --master spark://spark-iceberg:7077 \
  --driver-memory 12g \
  --executor-memory 12g \
  --executor-cores 3 \
  --total-executor-cores 6 \
  target/scala-2.12/create-iceberg-tables.jar \
  my_database_name
```

# Drop tables:

```
spark-submit \
  --class com.sparkmultigraph.DropIcebergTables \
  --master spark://spark-iceberg:7077 \
  --driver-memory 12g \
  --executor-memory 12g \
  --executor-cores 3 \
  --total-executor-cores 6 \
  target/scala-2.12/create-iceberg-tables.jar \
  my_database_name
```

