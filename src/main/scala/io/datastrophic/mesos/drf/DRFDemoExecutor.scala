package io.datastrophic.mesos.drf

import java.util.concurrent.{ExecutorService, Executors}

import com.google.protobuf.ByteString
import io.datastrophic.mesos.{BaseMesosExecutor, BinarySerDe}
import org.apache.mesos.Protos._
import org.apache.mesos.{ExecutorDriver, MesosExecutorDriver}
import org.slf4j.LoggerFactory

import scala.util.Random

object DRFDemoExecutor extends BinarySerDe {

   def main(args: Array[String]) {
      val logger = LoggerFactory.getLogger(getClass.getName)
      System.loadLibrary("mesos")
      var threadPool: ExecutorService = null

      val exec = new BaseMesosExecutor {
         override def launchTask(driver: ExecutorDriver, task: TaskInfo): Unit = {
            threadPool.execute(new Runnable() {
               override def run(): Unit = {
                  val taskStatus = TaskStatus.newBuilder().setTaskId(task.getTaskId)
                  val taskId = task.getTaskId.getValue

                  logger.info(s"Task $taskId received by executor: ${task.getExecutor.getExecutorId.getValue}")

                  driver.sendStatusUpdate(
                     taskStatus
                     .setState(TaskState.TASK_RUNNING)
                     .build()
                  )

                  val delay = 20000 + Random.nextInt(20000)
                  logger.info(s"Running dummy task for ${delay/1000f} sec.")

                  Thread.sleep(delay)

                  val msg = s"Task $taskId finished"
                  logger.info(msg)

                  driver.sendStatusUpdate(
                     taskStatus
                     .setState(TaskState.TASK_FINISHED)
                     .setData(ByteString.copyFrom(serialize(msg)))
                     .build()
                  )
               }
            })
         }

         override def registered(driver: ExecutorDriver, executorInfo: ExecutorInfo, frameworkInfo: FrameworkInfo, slaveInfo: SlaveInfo): Unit = {
            threadPool = Executors.newCachedThreadPool()
         }
      }

      new MesosExecutorDriver(exec).run()
   }
}
