## Overview

This project is dedicated to provide a set of examples to illustrate main Mesos use cases applied to architectures based on SMACK stack.
What is covered:

* How to run dockerized Chronos via Marathon
* How to build and submit microservices in Docker containers via Marathon
* Different modes of running Spark jobs on cluster (fine-grained and coarse-crained)
* Running scheduled Spark jobs in Chronos
* Example implementation of Mesos Framework aimed to show how to build basic schedulers

####Environment description:
The whole environment is represented by set of Docker containers orchestrated via `docker-compose`:

- ZooKeeper
- Mesos Master
- Mesos Agent
- Marathon
- Cassandra

To attach to running container (consider as ssh-ing to the host) container name or id is needed and could be found via
 
      docker-compose ps
or

      docker ps
and then 
      
      docker exec -ti <container name/id> bash      
For example to connect to Mesos Slave:

      docker exec -ti mesosworkshop_mesos-slave_1 bash

####Prerequisites

Docker and docker-compose are used for running code samples:

      docker version 1.10
      docker-compose 1.6.0

For building the app, SBT is used      
      
      SBT 0.13

The application was created with Typesafe Activator

####Environment setup
It's possible to create a full Mesos environment on the local machine using docker-compose. 

Depending on one's needs virtual machine memory could be adjusted to different value, but memory should be gte 4GB. Steps to create new 
docker-machine and launch docker images:  

      docker-machine create -d virtualbox --virtualbox-memory "8000" --virtualbox-cpu-count "6" mesos
      eval "$(docker-machine env mesos)"
      
      (!) Add address of `docker-machine ip mesos` to /etc/hosts with next hostnames: 
      <ip address> mesos mesos-master mesos-slave zookeeper marathon chronos  

      docker-compose up
      
After that Mesos Master, Mesos Slave and Marathon should be available:

* Mesos Master [http://mesos-master:5050](http://mesos-master:5050)
* Marathon [http://marathon:8080](http://marathon:8080)
      
## Submitting docker images to Marathon
To submit a container to Mesos via Marathon send POST request to `http://marathon:8080/v2/apps` 
([swagger ui available](http://marathon:8080/help)). Let's launch dockerized Chronos via Marathon. Because of local environment
specifics hostname resolution and network discovery suffers when running docker-in-docker, so ip address in `CHRONOS_MASTER` and `CHRONOS_ZK_HOSTS` should point
to `docker-machine ip mesos`.  
      
      curl -XPOST 'http://marathon:8080/v2/apps' -H 'Content-Type: application/json' -d '{
        "id": "chronos",
        "container": {
          "type": "DOCKER",
          "docker": {
            "network": "HOST",
              "image": "datastrophic/chronos:mesos-0.27.1-chronos-2.5",
              "parameters": [
                   { "key": "env", "value": "CHRONOS_HTTP_PORT=4400" },
                   { "key": "env", "value": "LIBPROCESS_IP='"$(docker-machine ip mesos)"'" },
                   { "key": "env", "value": "CHRONOS_MASTER=zk://'"$(docker-machine ip mesos)"':2181/mesos" },
                   { "key": "env", "value": "CHRONOS_ZK_HOSTS='"$(docker-machine ip mesos)"':2181"}
              ]
          }
        },
        "ports": [
           4400
        ],
        "cpus": 1,
        "mem": 512,
        "instances": 1
      }'
 
After that Chronos application should appear in Marathon UI and [Chronos UI](http://mesos:4400/) should be available on it's own and visible in 
Mesos Master's UI.

## Running dockerized services
This project comes with simple [Akka microservice](src/main/scala/akka/microservice/AkkaMicroservice) that accepts events, stores them in Cassandra and 
allows to count totals based on campaign id and event type. Event model: 

      case class Event(id: String, campaignId: String, eventType: String, value: Long, timestamp: String)
      
We're going to launch this microservice on Mesos in Docker container and scale number of its instances up and down with Marathon.
First the Docker image with the latest service binary should be built and deployed to Mesos. From the root of the project:
      
      sbt clean assembly distribute
      
      docker build -t datastrophic/akka-microservice:latest images/microservice
      
      curl -XPOST 'http://marathon:8080/v2/apps' -H 'Content-Type: application/json' -d '{
        "id": "akka-microservice",
        "container": {
          "type": "DOCKER",
          "docker": {
            "image": "datastrophic/akka-microservice:latest",
            "network": "BRIDGE",
            "portMappings": [{"containerPort": 31337, "servicePort": 31337}],
            "parameters": [
                { "key": "env", "value": "CASSANDRA_HOST='"$(docker-machine ip mesos)"'" },
                { "key": "env", "value": "CASSANDRA_KEYSPACE=demo" },
                { "key": "env", "value": "CASSANDRA_TABLE=event"}
            ]
          }
        },
        "cpus": 1,
        "mem": 512,
        "instances": 1
      }'

Via Marathon UI identify the host and port of the service (e.g. mesos-slave:31158) and perform some queries to post and read the data:

         curl -XPOST 'http://mesos-slave:31711/event' -H 'Content-Type: application/json' -d '{
            "id": "2e272715-c267-4c6b-8ab7-c9f96c5ab15a",
            "campaignId": "275ef4a2-513e-43e2-b85a-e656737c1147",
            "eventType": "impression",
            "value": 42,
            "timestamp": "2016-03-15 12:15:39"
         }'
         
         curl -XPOST 'http://mesos-slave:31711/event' -H 'Content-Type: application/json' -d '{
            "id": "2e272715-c267-4c6b-8ab7-c9f96c5ab15a",
            "campaignId": "275ef4a2-513e-43e2-b85a-e656737c1147",
            "eventType": "click",
            "value": 13,
            "timestamp": "2016-03-15 12:15:39"
         }'
         
         
         curl -XGET http://mesos-slave:31711/campaign/275ef4a2-513e-43e2-b85a-e656737c1147/totals/impression
         
         curl -XGET http://mesos-slave:31711/campaign/275ef4a2-513e-43e2-b85a-e656737c1147/totals/click

You can try to scale up and down the number of instances of the service and query different ones to verify that everything is working.
 
## Running Spark applications

###Running from cluster nodes
For running Spark jobs attaching one of the slave nodes is needed to run `spark-shell` and `spark-submit`:
 
      docker exec -ti mesosworkshop_mesos-slave_1 bash
      
Coarse-grained mode (default) (one executor per host, amount of Mesos tasks = amount of Spark executors = number of physical nodes,
spark-submit registered as a framework)
 
In addition, for coarse-grained mode, you can control the maximum number of resources Spark will acquire. 
By default, it will acquire all cores in the cluster (that get offered by Mesos), which only makes sense if you 
run just one application at a time. You can cap the maximum number of cores using conf.set("spark.cores.max", "10") (for example).

      /opt/spark/bin/spark-submit \
        --class org.apache.spark.examples.SparkPi \
        --master mesos://zk://zookeeper:2181/mesos \
        --deploy-mode client \
        --total-executor-cores 2 \
        /opt/spark/lib/spark-examples-1.6.0-hadoop2.6.0.jar \
        250
        
      
In “fine-grained” mode, each Spark task runs as a separate Mesos task. This allows multiple instances of Spark (and other frameworks) 
to share machines at a very fine granularity, where each application gets more or fewer machines as it ramps up and down, but it 
comes with an additional overhead in launching each task. This mode may be inappropriate for low-latency requirements like 
interactive queries or serving web requests. 
      
     /opt/spark/bin/spark-submit \
       --class org.apache.spark.examples.SparkPi \
       --master mesos://zk://zookeeper:2181/mesos \
       --deploy-mode client \
       --conf "spark.mesos.coarse=false"\
       --total-executor-cores 2 \
       /opt/spark/lib/spark-examples-1.6.0-hadoop2.6.0.jar \
       250
             
             
### Submitting Spark jobs via Marathon and Chronos

####Marathon
Marathon is designed for keeping long-running apps alive, so in context of Spark execution long-running Spark Streaming jobs
are the best candidates to run via Marathon.

      curl -XPOST 'http://marathon:8080/v2/apps' -H 'Content-Type: application/json' -d '{
        "cmd": "/opt/spark/bin/spark-submit --class org.apache.spark.examples.SparkPi --master mesos://zk://zookeeper:2181/mesos --deploy-mode client --total-executor-cores 2 /opt/spark/lib/spark-examples-1.6.0-hadoop2.6.0.jar 250",
        "id": "spark-pi",
        "cpus": 1,
        "mem": 1024,
        "instances": 1
      }'

####Chronos
Spark jobs for running in Chronos are basically all the computational jobs needed to run on schedule, another option is one-shot
 applications needed to be run only once.

      curl -L -H 'Content-Type: application/json' -X POST http://mesos:4400/scheduler/iso8601 -d '{
          "name": "Scheduled Spark Submit Job",
          "command": "/opt/spark/bin/spark-submit --class org.apache.spark.examples.SparkPi --master mesos://zk://zookeeper:2181/mesos --deploy-mode client --total-executor-cores 2 /opt/spark/lib/spark-examples-1.6.0-hadoop2.6.0.jar 250",
          "shell": true,
          "async": false,
          "cpus": 0.1,
          "disk": 256,
          "mem": 1024,
          "owner": "anton@datastrophic.io",
          "description": "SparkPi job executed every 3 minutes",
          "schedule": "R/2016-03-14T12:35:00.000Z/PT3M"
      }'

##Mesos Framework Examples
###Cassandra Load Testing Framework 
__Throttler__ framework is designed for load testing Cassandra in distributed manner. The Scheduler performance is controlled  
by the next properties:

      --total-queries - total amount of queries to execute during the test
      --queries-per-task - how many queries are executed within single Mesos Task
      --parallelism - how many Tasks are executed simultaneously   
 
Scheduler implementation: [ThrottleScheduler](src/main/scala/io/datastrophic/mesos/throttler/ThrottleScheduler.scala).

To run the framework build the project and distribute across containers:
  
      sbt clean assembly distribute

This build a fatjar and distribute across containers via linked volume directories, so the resulting jar will be 
immediately available in containers. To run Throttler user should be logged in (attached to) docker container and from there execute:
  
     java -cp /throttle/throttle-framework.jar -Dexecutor.path=/throttle/throttle-executor.sh io.datastrophic.mesos.throttler.Throttler \
     --mesos-master zk://zookeeper:2181/mesos \
     --cassandra-host cassandra \
     --keyspace demo_framework \
     --total-queries 100 \
     --queries-per-task 5 \
     --parallelism 5 
 
One can play with load test parameters, but remember that everything is executed inside a virtual machine (in case of Mac) and memory
starvation could start pretty quickly when the load becomes high. Which in turn can lead to containers failures.

###Dominant Resource Fairness Demo Framework
Mesos uses Dominant Resource Fairness algorithm to achieve fair resource allocation across frameworks. DRF framework is used to
demonstrate different cases and how DRF handles them. 

Scheduler implementation: [DRFDemoScheduler](src/main/scala/io/datastrophic/mesos/drf/DRFDemoScheduler.scala).

This framework has been used to provide some experimental results for 
[Datastrophic blog post about DRF](http://datastrophic.io/resource-allocation-in-mesos-dominant-resource-fairness-explained/). 
It is supposed that multiple instances of this framework should be run in parallel on the same cluster to observe DRF behavior 
when frameworks compete for resources. So main parameters for the framework are name (to distinguish it among others) and 
resources needed to run one task (cpu and memory). Invocation example:
 
      java -cp /throttle/throttle-framework.jar -Dexecutor.path=/throttle/drf-executor.sh io.datastrophic.mesos.drf.DRFDemoFramework \
      --mesos-master zk://zookeeper:2181/mesos \
      --framework-name 'Framework A' \
      --task-cpus 0.5 \
      --task-memory 512 