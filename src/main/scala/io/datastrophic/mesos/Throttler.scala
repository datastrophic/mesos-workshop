package io.datastrophic.mesos

import com.datastax.driver.core.Session
import com.twitter.chill.{ScalaKryoInstantiator, EmptyScalaKryoInstantiator}
import io.datastrophic.common.CassandraUtil
import io.datastrophic.mesos.ThrottleExecutor._
import org.apache.mesos.MesosSchedulerDriver
import org.apache.mesos.Protos.FrameworkInfo
import org.slf4j.LoggerFactory

object Throttler {
   import SchemaBuilder._

   val logger = LoggerFactory.getLogger(getClass.getName)

   def run(config: Config): Unit ={
      val framework = FrameworkInfo.newBuilder
                         .setName("Throttler")
                         .setUser("")
                         .setRole("*")
                         .setCheckpoint(false)
                         .setFailoverTimeout(0.0d)
                         .build()

      ensureSchema(config)

      val scheduler = config.mode match {
         case "node-local" =>
            logger.info("Starting in node-local mode. Tasks will be executed in one task per node mode.")
            new NodeLocalScheduler(config)
         case _ =>
            logger.info("Starting in fine-grained mode. Scheduler will accept all the offers until parallelism threshold is not reached.")
            new FineGrainedScheduler(config)
      }

      val driver = new MesosSchedulerDriver(scheduler, framework, config.mesosURL)
      driver.run()
   }

   def main(args: Array[String]): Unit = {
      val parser = new scopt.OptionParser[Config]("scopt") {
         head("scopt", "3.x")
         opt[String]('m', "mesos-master") required() action { (x, c) => c.copy(mesosURL = x) } text ("mesos master")
         opt[String]('h', "cassandra-host") required() action { (x, c) => c.copy(cassandraHost = x) } text ("cassandra hostname")
         opt[String]('k', "keyspace") required() action { (x, c) => c.copy(keyspace = x) } text ("keyspace name")
         opt[Int]('t', "total-queries") required() action { (x, c) => c.copy(totalQueries = x) } text ("total amount of queries to execute")
         opt[Int]('f', "queries-per-task") required() action { (x, c) => c.copy(queriesPerTask = x) } text ("amount of queries to execute within single task")
         opt[String]('o', "mode") required() action { (x, c) => c.copy(mode = x) } text ("execution mode: [fine-grained|node-local]")
         opt[Int]('p', "parallelism") required() action { (x, c) => c.copy(parallelism = x) } text ("number of tasks run in parallel")
         help("help") text("prints this usage text")
      }

      parser.parse(args, Config()) map { config =>
         run(config)
      } getOrElse {
         println("Not all program arguments provided, can't continue")
      }
   }
}

protected case class Config(
   mesosURL: String = "",
   cassandraHost: String = "",
   keyspace: String = "",
   totalQueries: Int = 0,
   queriesPerTask: Int = 0,
   mode: String = "fine-grained",
   parallelism: Int = 1
)

object SchemaBuilder {
   def ensureSchema(config: Config): Unit = {
      val session = CassandraUtil.buildSession(config.cassandraHost)

      session.execute(s"CREATE KEYSPACE IF NOT EXISTS ${config.keyspace} WITH REPLICATION = {'class' : 'SimpleStrategy', 'replication_factor' : 1 };")

      session.execute(s"""
                    CREATE TABLE IF NOT EXISTS ${config.keyspace}.test (
                     pk int,
                     ck uuid,
                     rand uuid,
                     PRIMARY KEY(pk, ck)
                    );
                    """.stripMargin)
   }
}