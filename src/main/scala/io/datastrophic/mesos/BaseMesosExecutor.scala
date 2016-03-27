package io.datastrophic.mesos

import org.apache.mesos.Protos.{SlaveInfo, TaskID}
import org.apache.mesos.{Executor, ExecutorDriver}

trait BaseMesosExecutor extends Executor {

   override def frameworkMessage(driver: ExecutorDriver, data: Array[Byte]): Unit = {}

   override def error(driver: ExecutorDriver, message: String): Unit = {}

   override def reregistered(driver: ExecutorDriver, slaveInfo: SlaveInfo): Unit = {}

   override def killTask(driver: ExecutorDriver, taskId: TaskID): Unit = {}

   override def disconnected(driver: ExecutorDriver): Unit = {}

   override def shutdown(driver: ExecutorDriver): Unit = {}

}
