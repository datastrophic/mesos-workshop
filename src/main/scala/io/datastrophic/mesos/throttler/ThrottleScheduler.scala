package io.datastrophic.mesos.throttler

import java.util
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

import io.datastrophic.mesos.BaseMesosScheduler
import org.apache.mesos.Protos._
import org.apache.mesos.SchedulerDriver
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._

class ThrottleScheduler(val config: Config) extends BaseMesosScheduler with CassandraTaskBuilder {

   private val logger = LoggerFactory.getLogger(classOf[ThrottleScheduler])
   private val stateLock = new ReentrantLock()

   val queriesToRun = new AtomicInteger(config.totalQueries)
   val currentTasks = new AtomicInteger(0)
   val errors = new AtomicInteger(0)
   val executors = new ConcurrentHashMap[String, ExecutorInfo]()

   override def statusUpdate(driver: SchedulerDriver, status: TaskStatus): Unit = {
      status.getState match {
         case TaskState.TASK_FINISHED =>
            logger.info(s"Task finished on slave ${status.getSlaveId.getValue}. Message: ${deserialize[String](status.getData.toByteArray)}")
            currentTasks.decrementAndGet()

            if(queriesToRun.get() == 0){
               logger.info(s"All queries launched, exit now.")
               System.exit(0)
            }
         case TaskState.TASK_ERROR =>
            logger.error(s"Task error on slave ${status.getSlaveId.getValue}. Exception message: ${deserialize[String](status.getData.toByteArray)}. Total " +
               s"errors: ${errors.get()}")
            currentTasks.decrementAndGet()

            if(errors.incrementAndGet() > 5){
               logger.info("Too many errors in tasks, shutting down.")
               System.exit(1)
            }

         case TaskState.TASK_RUNNING => ()

         case _ =>
            logger.info(s"${status.toString}")
      }
   }

   override def resourceOffers(driver: SchedulerDriver, offers: util.List[Offer]): Unit = {
      for(offer <- offers){
         stateLock.synchronized {
            logger.debug(s"Received resource offer: ${offer.toString}")
            if(queriesToRun.get() > 0) {
               if(currentTasks.get() <= config.parallelism){
                  logger.info(s"Launching task on slave ${offer.getSlaveId.getValue}")

                  val numberOfQueries = if(queriesToRun.get() < config.queriesPerTask) queriesToRun.get() else config.queriesPerTask
                  val executorInfo = executors.getOrElseUpdate(offer.getSlaveId.getValue, buildExecutorInfo(driver, "ThrottlerFG"))

                  val taskInfo = buildCassandraTask(offer, executorInfo, numberOfQueries)
                  driver.launchTasks(List(offer.getId), List(taskInfo))

                  currentTasks.incrementAndGet()
                  queriesToRun.getAndSet(queriesToRun.get() - numberOfQueries)
               } else {
                  logger.info(s"Already running ${config.parallelism} tasks on cluster. Declining the offer")
                  driver.declineOffer(offer.getId)
               }
            } else {
               logger.info(s"All queries launched, waiting for tasks to complete. Declining offer.")
               driver.declineOffer(offer.getId)
            }
         }
      }
   }


}
