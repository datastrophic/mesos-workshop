package io.datastrophic.mesos.throttler

import java.util.concurrent.{ExecutorService, Executors}

import com.google.protobuf.ByteString
import io.datastrophic.common.CassandraUtil
import io.datastrophic.mesos.{BaseMesosExecutor, BinarySerDe}
import org.apache.mesos.Protos._
import org.apache.mesos.{ExecutorDriver, MesosExecutorDriver}
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

object ThrottleExecutor extends BinarySerDe {

   def main(args: Array[String]) {
      val logger = LoggerFactory.getLogger(getClass.getName)
      System.loadLibrary("mesos")
      var classLoader: ClassLoader = null
      var threadPool: ExecutorService = null

      val exec = new BaseMesosExecutor {
         override def launchTask(driver: ExecutorDriver, task: TaskInfo): Unit = {
            val arg = task.getData.toByteArray

            threadPool.execute(new Runnable() {
               override def run(): Unit = {
                  val deserializedTask = deserialize[Task[Any]](task.getData.toByteArray)

                  val taskStatus = TaskStatus.newBuilder().setTaskId(task.getTaskId)

                  logger.info(s"Task ${task.getTaskId.getValue} received by executor: ${task.getExecutor.getExecutorId.getValue}")

                  driver.sendStatusUpdate(
                     taskStatus
                     .setState(TaskState.TASK_RUNNING)
                     .build()
                  )

                  val start = System.currentTimeMillis()
                  Try{
                     val session = CassandraUtil.buildSession(deserializedTask.host)
                     deserializedTask.queries.foreach(session.execute)
                  } match {
                     case Success(_) =>
                        logger.info(s"Task ${task.getTaskId.getValue} finished")
                        val msg = s"Executed ${deserializedTask.queries.size} queries in ${(System.currentTimeMillis()- start)/1000f} sec."

                        Thread.sleep(5000)

                        driver.sendStatusUpdate(
                           taskStatus
                           .setState(TaskState.TASK_FINISHED)
                                 .setData(ByteString.copyFrom(
                                    serialize(msg)
                                 ))
                           .build()
                        )

                     case Failure(ex) =>
                        logger.error(s"Exception in Task ${task.getTaskId.getValue}", ex)
                        driver.sendStatusUpdate(
                           taskStatus
                           .setState(TaskState.TASK_ERROR)
                           .setData(ByteString.copyFrom(serialize(ex.getMessage)))
                           .build())
                  }
               }

            })
         }

         override def registered(driver: ExecutorDriver, executorInfo: ExecutorInfo, frameworkInfo: FrameworkInfo, slaveInfo: SlaveInfo): Unit = {
            classLoader = this.getClass.getClassLoader
            Thread.currentThread.setContextClassLoader(classLoader)
            threadPool = Executors.newCachedThreadPool()
         }
      }

      new MesosExecutorDriver(exec).run()
   }


}
