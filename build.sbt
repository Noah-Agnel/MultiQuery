name              := "SparkMultiGraphMatching"
version           := "1.0.0"
scalaVersion      := "2.12.15"

val sparkVersion   = "3.3.0"
val icebergVersion = "1.3.1"


libraryDependencies ++= Seq(
  "org.apache.spark"       %% "spark-core"      % sparkVersion % "provided",
  "org.apache.spark"       %% "spark-sql"       % sparkVersion % "provided",
  "org.parboiled"          %% "parboiled-scala" % "1.3.1",
  "org.parboiled"          %  "parboiled-core"  % "1.3.1",
  "org.apache.iceberg"     %% "iceberg-spark-runtime-3.3" % icebergVersion,
  "org.apache.iceberg"     % "iceberg-core"     % icebergVersion,
  "org.apache.iceberg"     % "iceberg-aws"      % icebergVersion,
  "software.amazon.awssdk" % "s3"               % "2.17.52",
  "org.apache.hadoop"      % "hadoop-aws"       % "3.3.2",
  "org.apache.hadoop"      % "hadoop-common"    % "3.3.2",
  "com.amazonaws"          % "aws-java-sdk-bundle" % "1.12.262",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2"
)


Compile / unmanagedJars ++= Seq(
  file("libraries/util-9.0-9.0.20210312.jar"),
  file("libraries/ast-9.0-9.0.20210312.jar"),
  file("libraries/expressions-9.0-9.0.20210312.jar"),
  file("libraries/parser-9.0-9.0.20210312.jar")
)
Compile / unmanagedSourceDirectories += baseDirectory.value / "dnf"

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case PathList("reference.conf") => MergeStrategy.concat
  case _ => MergeStrategy.first
}

assembly / assemblyJarName := "create-iceberg-tables.jar" 