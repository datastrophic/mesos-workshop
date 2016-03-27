package io.datastrophic.mesos

import java.util.UUID

import org.apache.mesos.Protos.Value.Scalar
import org.apache.mesos.Protos.Value.Type.SCALAR
import org.apache.mesos.Protos._
import org.apache.mesos.SchedulerDriver

trait TaskBuilder extends BinarySerDe {
   def buildDummyTask(offer: Offer, cpus: Double, memory: Int, executorInfo: ExecutorInfo, prefix: String) = {
      val cpuResource = Resource.newBuilder
                          .setType(SCALAR)
                          .setName("cpus")
                          .setScalar(Scalar.newBuilder.setValue(cpus))
                          .setRole("*")
                          .build

      val memResource = Resource.newBuilder
                          .setType(SCALAR)
                          .setName("mem")
                          .setScalar(Scalar.newBuilder.setValue(memory))
                          .setRole("*")
                          .build

      TaskInfo.newBuilder()
      .setSlaveId(SlaveID.newBuilder().setValue(offer.getSlaveId.getValue).build())
      .setTaskId(TaskID.newBuilder().setValue(s"${prefix}_DRFTask_$uuid"))
      .setExecutor(executorInfo)
      .setName(UUID.randomUUID().toString)
      .addResources(cpuResource)
      .addResources(memResource)
      .build()
   }

   def buildExecutorInfo(d: SchedulerDriver, prefix: String): ExecutorInfo = {
      val scriptPath = System.getProperty("executor.path","/throttle/throttle-executor.sh")
      ExecutorInfo.newBuilder()
      .setCommand(CommandInfo.newBuilder().setValue("/bin/sh "+scriptPath))
      .setExecutorId(ExecutorID.newBuilder().setValue(s"${prefix}_$uuid"))
      .build()
   }

   def uuid = UUID.randomUUID()
}