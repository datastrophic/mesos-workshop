name := """mesos-workshop"""

version := "1.0"

scalaVersion := "2.11.7"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
   val akkaVersion       = "2.4.2"
   val scalaTestVersion  = "2.2.5"
   Seq(
      "com.typesafe.akka" %% "akka-actor"                           % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"             		        % akkaVersion,
      "com.typesafe.akka" %% "akka-http-experimental"               % akkaVersion,
      "com.typesafe.akka" %% "akka-http-spray-json-experimental"    % akkaVersion,
      "com.typesafe.akka" %% "akka-http-testkit"       		        % akkaVersion,
      "com.datastax.cassandra" % "cassandra-driver-core"            % "3.0.0",
      "com.github.scopt"  %% "scopt"                                % "3.3.0",
      "org.scalatest"     %% "scalatest"                            % scalaTestVersion % "test",
      "org.scalatest"     %% "scalatest"                            % scalaTestVersion % "test"
   )
}

assemblyJarName in assembly := "mesos-workshop.jar"

assemblyMergeStrategy in assembly := {
   case m if m.toLowerCase.matches("meta-inf.*\\.sf$") => MergeStrategy.discard
   case PathList("META-INF", xs @ _*)                  => MergeStrategy.discard
   case PathList("reference.conf")                     => MergeStrategy.concat
   case _                                              => MergeStrategy.first
}