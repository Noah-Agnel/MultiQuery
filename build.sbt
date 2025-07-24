name              := "SparkMultiGraphMatching"
version           := "1.0.0"
scalaVersion      := "2.12.15"

val sparkVersion   = "3.3.0"
val icebergVersion = "1.3.1"


libraryDependencies ++= Seq(
  "org.apache.spark"       %% "spark-core"   % sparkVersion % "provided",
  "org.apache.spark"       %% "spark-sql"    % sparkVersion % "provided",
  "org.apache.iceberg"     %% "iceberg-spark-runtime-3.3" % icebergVersion,
  "org.apache.iceberg"     % "iceberg-core"  % icebergVersion,
  "org.apache.iceberg"     % "iceberg-aws"   % icebergVersion,
  "software.amazon.awssdk" % "s3"            % "2.17.52",
  "org.apache.hadoop"      % "hadoop-aws"    % "3.3.2",
  "org.opencypher"         % "util-9.0"      % "9.0.20210312",
  "org.apache.hadoop"      % "hadoop-common" % "3.3.2",
  "com.amazonaws"          % "aws-java-sdk-bundle" % "1.12.262"
)


Compile / unmanagedJars += file("libraries/util-9.0-9.0.20210312.jar")

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case PathList("reference.conf") => MergeStrategy.concat
  case _ => MergeStrategy.first
}

assembly / assemblyJarName := "create-iceberg-tables.jar" 