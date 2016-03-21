package io.datastrophic.mesos

import java.util.concurrent.{Executors, ExecutorService}

import com.google.protobuf.ByteString
import io.datastrophic.common.CassandraUtil
import org.apache.mesos.Protos._
import org.apache.mesos.{MesosExecutorDriver, ExecutorDriver, Executor}
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

object ThrottleExecutor extends BinarySerDe {

   def main(args: Array[String]) {
      val logger = LoggerFactory.getLogger(getClass.getName)
      System.loadLibrary("mesos")
      var classLoader: ClassLoader = null
      var threadPool: ExecutorService = null

      val exec = new Executor {
         override def launchTask(driver: ExecutorDriver, task: TaskInfo): Unit = {
            val arg = task.getData.toByteArray
            threadPool.execute(new Runnable() {
               override def run(): Unit = {
                  val deserializedTask = deserialize[Task[Any]](task.getData.toByteArray)

                  val taskStatus = TaskStatus.newBuilder().setTaskId(task.getTaskId)

                  logger.info(s"Task ${task.getTaskId.getValue} received: $deserializedTask")

                  val start = System.currentTimeMillis()
                  Try{
                     val session = CassandraUtil.buildSession(deserializedTask.host)
                     deserializedTask.queries.foreach(session.execute)
                  } match {
                     case Success(_) =>
                        logger.info(s"Task ${task.getTaskId.getValue} finished")
                        val msg = s"Executed ${deserializedTask.queries.size} queries in ${(System.currentTimeMillis()- start)/1000f} sec."

                        //delay is needed to demonstrate declines of resources offers in node-local scheduler
                        Thread.sleep(10000)

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
                        System.exit(1)
                  }
               }

            })
         }

         override def registered(driver: ExecutorDriver, executorInfo: ExecutorInfo, frameworkInfo: FrameworkInfo, slaveInfo: SlaveInfo): Unit = {
            classLoader = this.getClass.getClassLoader
            Thread.currentThread.setContextClassLoader(classLoader)
            threadPool = Executors.newCachedThreadPool()
         }

         override def frameworkMessage(driver: ExecutorDriver, data: Array[Byte]): Unit = {}

         override def error(driver: ExecutorDriver, message: String): Unit = {}

         override def reregistered(driver: ExecutorDriver, slaveInfo: SlaveInfo): Unit = {}

         override def killTask(driver: ExecutorDriver, taskId: TaskID): Unit = {}

         override def disconnected(driver: ExecutorDriver): Unit = {}

         override def shutdown(driver: ExecutorDriver): Unit = {}
      }

      new MesosExecutorDriver(exec).run()
   }


}
