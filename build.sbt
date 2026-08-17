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
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
  "org.scala-lang.modules" %% "scala-collection-compat" % "2.9.0"
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

Compile / run := Defaults.runTask(
  Compile / fullClasspath,
  Compile / run / mainClass,
  Compile / run / runner
).evaluated

Compile / runMain := Defaults.runMainTask(
  Compile / fullClasspath,
  Compile / run / runner
).evaluated

run / fork := true

Compile / run / javaOptions ++= Seq(
  // Driver heap for the forked run JVM -- Spark's local[*] mode runs entirely in this
  // process, so this (not spark.driver.memory) is what actually controls how much memory
  // is available during table population. 5g leaves headroom for the OS on an 8GB machine;
  // raise it if running on a bigger box.
  "-Xmx5g",
  "--add-opens=java.base/java.lang=ALL-UNNAMED",
  "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
  "--add-opens=java.base/java.io=ALL-UNNAMED",
  "--add-opens=java.base/java.net=ALL-UNNAMED",
  "--add-opens=java.base/java.nio=ALL-UNNAMED",
  "--add-opens=java.base/java.util=ALL-UNNAMED",
  "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
  "--add-opens=java.base/sun.security.action=ALL-UNNAMED",
  "--add-opens=java.base/sun.util.calendar=ALL-UNNAMED"
)

dependencyOverrides ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-databind"    % "2.13.4.2",
  "com.fasterxml.jackson.core" % "jackson-core"         % "2.13.4",
  "com.fasterxml.jackson.core" % "jackson-annotations"  % "2.13.4"
)