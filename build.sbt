name := """mesos-workshop"""

version := "1.0"

scalaVersion := "2.11.7"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
   val akkaV       = "2.4.2"
   val akkaStreamV = "2.0.1"
   val scalaTestV  = "2.2.5"
   Seq(
      "com.typesafe.akka" %% "akka-actor"                           % akkaV,
      "com.typesafe.akka" %% "akka-stream-experimental"             % akkaStreamV,
      "com.typesafe.akka" %% "akka-http-core-experimental"          % akkaStreamV,
      "com.typesafe.akka" %% "akka-http-experimental"               % akkaStreamV,
      "com.typesafe.akka" %% "akka-http-spray-json-experimental"    % akkaStreamV,
      "com.datastax.cassandra" % "cassandra-driver-core"            % "3.0.0",
      "com.github.scopt"  %% "scopt"                                % "3.3.0",
      "com.typesafe.akka" %% "akka-http-testkit-experimental"       % akkaStreamV,
      "org.scalatest"     %% "scalatest"                            % scalaTestV % "test"
   )
}

assemblyJarName in assembly := "mesos-workshop.jar"