package io.datastrophic.mesos.throttler

import java.util.UUID

import com.google.protobuf.ByteString
import io.datastrophic.mesos.TaskBuilder
import org.apache.mesos.Protos.Value.Scalar
import org.apache.mesos.Protos.Value.Type._
import org.apache.mesos.Protos._

trait CassandraTaskBuilder extends TaskBuilder {
   def config: Config

   def buildCassandraTask(offer: Offer, executorInfo: ExecutorInfo, totalQueries: Int) = {
      val task = new Task(
         config.cassandraHost,
         (1 to totalQueries).map(pk => s"INSERT INTO ${config.keyspace}.test (pk, ck, rand) VALUES($pk, $uuid, $uuid);").toList
      )

      val cpus = Resource.newBuilder
                 .setType(SCALAR)
                 .setName("cpus")
                 .setScalar(Scalar.newBuilder.setValue(1.0))
                 .setRole("*")
                 .build

      TaskInfo.newBuilder()
      .setSlaveId(SlaveID.newBuilder().setValue(offer.getSlaveId.getValue).build())
      .setTaskId(TaskID.newBuilder().setValue(s"ThrottleTask_$uuid"))
      .setExecutor(executorInfo)
      .setName(UUID.randomUUID().toString)
      .addResources(cpus)
      .setData(ByteString.copyFrom(serialize(task)))
      .build()
   }
}

@SerialVersionUID(1458551286)
case class  Task[T]( val host: String, queries: List[String]) extends Serializable
