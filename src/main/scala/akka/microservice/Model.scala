package akka.microservice

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.datastax.driver.core.Session
import spray.json.DefaultJsonProtocol

case class Event(id: String, campaignId: String, eventType: String, value: Long, timestamp: String)

case class TotalsResponse(campaignId: String, total: Long)

case class ClusterContext(session: Session, keyspace: String, table: String)

trait Protocols extends SprayJsonSupport with DefaultJsonProtocol {
   implicit val eventFormat = jsonFormat5(Event.apply)
   implicit val campaignResponseFormat = jsonFormat2(TotalsResponse.apply)
}