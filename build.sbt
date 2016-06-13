name := """mesos-workshop"""

version := "1.0"

scalaVersion := "2.11.7"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

libraryDependencies ++= {
   val akkaVersion       = "2.4.2"
   val scalaTestVersion  = "2.2.5"
   val mesosVersion      = "0.28.1"

   Seq(
      "org.apache.mesos"        % "mesos"                                % mesosVersion,
      "com.datastax.cassandra"  % "cassandra-driver-core"                % "3.0.0",
      "com.github.scopt"       %% "scopt"                                % "3.3.0",
      "com.typesafe.akka"      %% "akka-actor"                           % akkaVersion,
      "com.typesafe.akka"      %% "akka-stream"             		       % akkaVersion,
      "com.typesafe.akka"      %% "akka-http-experimental"               % akkaVersion,
      "com.typesafe.akka"      %% "akka-http-spray-json-experimental"    % akkaVersion,
      "ch.qos.logback"          % "logback-classic"                      % "1.1.2",
      "com.typesafe.akka"      %% "akka-http-testkit"       		       % akkaVersion,
      "org.scalatest"          %% "scalatest"                            % scalaTestVersion % "test",
      "org.scalatest"          %% "scalatest"                            % scalaTestVersion % "test"
   )
}

assemblyJarName in assembly := "mesos-workshop.jar"

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = true)

assemblyMergeStrategy in assembly := {
   case m if m.toLowerCase.matches("meta-inf.*\\.sf$") => MergeStrategy.discard
   case PathList("META-INF", xs @ _*)                  => MergeStrategy.discard
   case PathList("reference.conf")                     => MergeStrategy.concat
   case x                                              => MergeStrategy.defaultMergeStrategy(x)
}


val copyTask = TaskKey[Unit]("distribute", "Copying fatjar to proper directories for being picked up by Docker")

copyTask <<= assembly map { (asm) =>
   val local = asm.getPath
   val microserviceContext = "images/microservice/akka-microservice.jar"
   val executorContext = "executor/throttle-framework.jar"

   println(s"Copying: $local -> $microserviceContext")
   Seq("cp", local, microserviceContext) !!

   println(s"Copying: $local -> $executorContext")
   Seq("cp", local, executorContext) !!
}