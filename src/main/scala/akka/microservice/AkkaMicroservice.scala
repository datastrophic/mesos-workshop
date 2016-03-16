package akka.microservice

import java.text.SimpleDateFormat
import java.util.UUID

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives
import akka.stream.ActorMaterializer
import com.datastax.driver.core.Cluster

import scala.collection.JavaConversions._
import scala.util.{Failure, Success, Try}

class Service(val clusterContext: ClusterContext) extends Directives with Protocols {
   implicit val system = ActorSystem()
   implicit def executor = system.dispatcher
   implicit val materializer = ActorMaterializer()
   val logger = Logging(system, getClass)

   val format = new SimpleDateFormat("yyyy-dd-MM HH:mm:ss")

   def save(event: Event) = {
      Try {
         clusterContext.session.execute(s"""INSERT INTO ${clusterContext.keyspace}.${clusterContext.table} (id, campaign_id, event_type, value, time, internal_id)
            VALUES (${event.id.toString}, ${event.campaignId}, '${event.eventType}', ${event.value},
                    ${format.parse(event.timestamp).getTime}, ${UUID.randomUUID().toString});""".stripMargin)
      }
   }

   def getTotals(campaignId: UUID, eventType: String) = {
      Try {
         val rows = clusterContext.session.execute(s"""
                         SELECT value from ${clusterContext.keyspace}.${clusterContext.table}
                         WHERE campaign_id = ${campaignId.toString} AND event_type = '$eventType';""".stripMargin)

         rows.all().foldLeft(0L){(acc, row) => acc + row.getLong(0)}
      }
   }

   val routes = {
      logRequestResult("akka-http-microservice") {
         pathPrefix("event") {
            (post & entity(as[Event])) { event =>
               logger.info(s"Event received: $event")
               complete {
                  save(event) match {
                     case Success(_) =>
                        logger.info(s"Event stored: $event")
                        OK
                     case Failure(ex) =>
                        logger.error(s"Error during writing to storage", ex)
                        BadRequest -> ex.getMessage
                  }
               }
            }
         } ~
         pathPrefix("campaign" / JavaUUID / "totals"){ uuid =>
            (get & path(Segment)) { eventType =>
               logger.info(s"Counting total of '$eventType' events for campaign: $uuid")
               complete {
                   getTotals(uuid, eventType) match {
                     case Success(value) => TotalsResponse(uuid.toString, value)
                     case Failure(ex) =>
                        logger.error(s"Error during read from storage", ex)
                        BadRequest -> ex.getMessage
                  }
               }
            }
         }
      }
   }

   def start(port: Int) = {
      Http().bindAndHandle(routes, "127.0.0.1", port)
   }
}

object AkkaMicroservice {
   import SchemaHelper._

   def run(config: Config) = {
      val session = Cluster.builder()
                    .addContactPoint(config.cassandraHost)
                    .build()
                    .connect()

      val clusterContext = ClusterContext(session, config.keyspace, config.table)

      createSchema(clusterContext)

      val service = new Service(clusterContext)
      service.start(config.port)
   }

   def main(args: Array[String]): Unit = {
      val parser = new scopt.OptionParser[Config]("scopt") {
         head("scopt", "3.x")
         opt[Int]('p', "service-port") required() action { (x, c) => c.copy(port = x) } text ("service port to listen on")
         opt[String]('h', "cassandra-host") required() action { (x, c) => c.copy(cassandraHost = x) } text ("cassandra hostname")
         opt[String]('k', "keyspace") required() action { (x, c) => c.copy(keyspace = x) } text ("keyspace name")
         opt[String]('t', "table") required() action { (x, c) => c.copy(table = x) } text ("table name")
         help("help") text("prints this usage text")
      }

      parser.parse(args, Config()) map { config =>
         run(config)
      } getOrElse {
         println("Not all program arguments provided, can't continue")
      }
   }
}

object SchemaHelper {
   def createSchema(context: ClusterContext): Unit = {
      context.session.execute(s"CREATE KEYSPACE IF NOT EXISTS ${context.keyspace} WITH REPLICATION = {'class' : 'SimpleStrategy', 'replication_factor' : 1 };")

      context.session.execute(s"""
                    CREATE TABLE IF NOT EXISTS ${context.keyspace}.event (
                     id uuid,
                     campaign_id uuid,
                     event_type text,
                     value bigint,
                     time timestamp,
                     internal_id uuid,
                     PRIMARY KEY((campaign_id, event_type), id, time, internal_id)
                    );
                    """.stripMargin)
   }
}

case class Config(port: Int = 0, cassandraHost: String = "", keyspace: String = "", table: String = "")