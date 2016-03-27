package io.datastrophic.mesos.drf

import java.util
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

import io.datastrophic.mesos.{BaseMesosScheduler, TaskBuilder}
import org.apache.mesos.Protos._
import org.apache.mesos.SchedulerDriver
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._

class DRFDemoScheduler(val config: Config) extends BaseMesosScheduler with TaskBuilder{

   private val logger = LoggerFactory.getLogger(classOf[DRFDemoScheduler])
   private val stateLock = new ReentrantLock()
   val executors = new ConcurrentHashMap[String, ExecutorInfo]()

   override def statusUpdate(driver: SchedulerDriver, status: TaskStatus): Unit = {
      status.getState match {
         case TaskState.TASK_FINISHED =>
            logger.info(s"Task finished on slave ${status.getSlaveId.getValue}. Message: ${deserialize[String](status.getData.toByteArray)}")
         case TaskState.TASK_RUNNING => ()
         case _ =>
            logger.info(s"${status.toString}")
      }
   }

   override def resourceOffers(driver: SchedulerDriver, offers: util.List[Offer]): Unit = {
      for(offer <- offers){
         stateLock.synchronized {
            logger.info(s"Received resource offer: cpus:${getCpus(offer)} mem: ${getMemory(offer)}")


            if(isOfferValid(offer)){
               val executorInfo = executors.getOrElseUpdate(offer.getSlaveId.getValue, buildExecutorInfo(driver, "DRFDemoExecutor"))

               //amount of tasks is calculated to fully use resources from the offer
               val tasks = buildTasks(offer, config, executorInfo)
               logger.info(s"Launching ${tasks.size} tasks on slave ${offer.getSlaveId.getValue}")
               driver.launchTasks(List(offer.getId), tasks)
            } else {
               logger.info(s"Offer provides insufficient resources. Declining.")
               driver.declineOffer(offer.getId)
            }
         }
      }
   }

   def buildTasks(offer: Offer, config: Config, executorInfo: ExecutorInfo): List[TaskInfo] = {
      val amount = Math.min(getCpus(offer)/config.cpus, getMemory(offer)/config.mem).toInt
      (1 to amount).map(_ =>
         buildDummyTask(offer, config.cpus, config.mem, executorInfo, config.name.replace(" ","_"))
      ).toList
   }

   private def isOfferValid(offer: Offer): Boolean = getCpus(offer) >= config.cpus && getMemory(offer) >= config.mem

   private def getCpus(offer: Offer) = offer.getResourcesList.find(resource => resource.getName == "cpus").map(_.getScalar.getValue).getOrElse(0.0)

   private def getMemory(offer: Offer) = offer.getResourcesList.find(resource => resource.getName == "mem").map(_.getScalar.getValue).getOrElse(0.0)
}
