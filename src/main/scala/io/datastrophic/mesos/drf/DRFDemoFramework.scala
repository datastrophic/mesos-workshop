package io.datastrophic.mesos.drf

import org.apache.mesos.MesosSchedulerDriver
import org.apache.mesos.Protos.FrameworkInfo
import org.slf4j.LoggerFactory

object DRFDemoFramework {

   val logger = LoggerFactory.getLogger(getClass.getName)

   def run(config: Config): Unit ={
      val framework = FrameworkInfo.newBuilder
                         .setName(config.name)
                         .setUser("")
                         .setRole("*")
                         .setCheckpoint(false)
                         .setFailoverTimeout(0.0d)
                         .build()

      val driver = new MesosSchedulerDriver(new DRFDemoScheduler(config), framework, config.mesosURL)
      driver.run()
   }

   def main(args: Array[String]): Unit = {
      val parser = new scopt.OptionParser[Config]("scopt") {
         head("scopt", "3.x")
         opt[String]('m', "mesos-master") required() action { (x, c) => c.copy(mesosURL = x) } text ("mesos master url")
         opt[String]('n', "framework-name") required() action { (x, c) => c.copy(name = x) } text ("name of the framework to register")
         opt[Double]('c', "task-cpus") required() action { (x, c) => c.copy(cpus = x) } text ("cpu share per task")
         opt[Int]('t', "task-memory") required() action { (x, c) => c.copy(mem = x) } text ("amount of memory per task")
         help("help") text("prints this usage text")
      }

      parser.parse(args, Config()) map { config =>
         run(config)
      } getOrElse {
         println("Not all program arguments provided, can't continue")
      }
   }
}

protected case class Config(
   mesosURL: String = "",
   name: String = "",
   cpus: Double = 0.1,
   mem: Int = 512
)