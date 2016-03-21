package io.datastrophic.mesos

import java.util
import java.util.concurrent.locks.ReentrantLock

import org.apache.mesos.Protos._
import org.apache.mesos.{Scheduler, SchedulerDriver}
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._
import scala.collection.mutable

trait ThrottleScheduler extends Scheduler with TaskBuilder {

   private val logger = LoggerFactory.getLogger(getClass.getName)

   override def offerRescinded(driver: SchedulerDriver, offerId: OfferID): Unit = logger.info(s"Offer rescinded: $offerId")

   override def disconnected(driver: SchedulerDriver): Unit = logger.info(s"Disconnected")

   override def reregistered(driver: SchedulerDriver, masterInfo: MasterInfo): Unit = logger.info(s"Reregistered: $masterInfo")

   override def slaveLost(driver: SchedulerDriver, slaveId: SlaveID): Unit = logger.info(s"Slave Lost: $slaveId")

   override def error(driver: SchedulerDriver, message: String): Unit = logger.info(s"Error: $message")

   override def frameworkMessage(driver: SchedulerDriver, executorId: ExecutorID, slaveId: SlaveID, data: Array[Byte]): Unit = {
      logger.info(s"Framework message: ${String.copyValueOf(data.map(_.toChar))}")
   }

   override def registered(driver: SchedulerDriver, frameworkId: FrameworkID, masterInfo: MasterInfo): Unit = {}

   override def executorLost(driver: SchedulerDriver, executorId: ExecutorID, slaveId: SlaveID, status: Int): Unit = {}

   def launch(driver: SchedulerDriver, offer: Offer, numberOfQueries: Int): Unit ={
      val taskInfo = buildMesosTask(driver, offer, numberOfQueries)
      driver.launchTasks(List(offer.getId), List(taskInfo))
   }
}
