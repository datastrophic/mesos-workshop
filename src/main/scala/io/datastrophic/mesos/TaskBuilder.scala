package io.datastrophic.mesos

import java.util.UUID

import com.google.protobuf.ByteString
import org.apache.mesos.Protos._
import org.apache.mesos.SchedulerDriver

trait TaskBuilder extends BinarySerDe {
   def config: Config

   def buildMesosTask(driver: SchedulerDriver, offer: Offer, totalQueries: Int) = {
      val task = new Task(
         config.cassandraHost,
         (1 to totalQueries).map(pk => s"INSERT INTO ${config.keyspace}.test (pk, ck, rand) VALUES($pk, $uuid, $uuid);").toList
      )

      val cpus = Resource.newBuilder
                 .setType(org.apache.mesos.Protos.Value.Type.SCALAR)
                 .setName("cpus")
                 .setScalar(org.apache.mesos.Protos.Value.Scalar.newBuilder.setValue(1.0))
                 .setRole("*")
                 .build

      TaskInfo.newBuilder()
            .setSlaveId(SlaveID.newBuilder().setValue(offer.getSlaveId.getValue).build())
            .setTaskId(TaskID.newBuilder().setValue(s"ThrottleTask_$uuid"))
            .setExecutor(buildExecutorInfo(driver))
            .setName(UUID.randomUUID().toString)
            .addResources(cpus)
            .setData(ByteString.copyFrom(serialize(task)))
            .build()
   }

   def buildExecutorInfo(d: SchedulerDriver): ExecutorInfo = {
      val scriptPath = System.getProperty("executor.path","/throttle/throttle-executor.sh")
      ExecutorInfo.newBuilder()
      .setCommand(CommandInfo.newBuilder().setValue("/bin/sh "+scriptPath))
      .setExecutorId(ExecutorID.newBuilder().setValue(s"Throttler_$uuid"))
      .build()
   }

   def uuid = UUID.randomUUID()
}

@SerialVersionUID(1458551286)
case class  Task[T]( val host: String, queries: List[String]) extends Serializable