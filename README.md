## Overview

Docker description: how many containers of what is launched

- ZooKeeper
- Mesos master
- Mesos Agent
- Marathon

##Prerequisites

Docker and docker-compose are used for running code samples:

      docker version 1.10
      docker-compose 1.6.0

For building the app, SBT is used      
      
      SBT 0.13

The application was created with Typesafe Activator

###Environment setup
It's possible to create a full Mesos environment on the local machine using docker-compose. 

Depending on one's needs virtual machine memory could be adjusted to different value, but memory should be gte 4GB. Steps to create new 
docker-machine and launch docker images:  

      docker-machine create -d virtualbox --virtualbox-memory "8000" --virtualbox-cpu-count "4" mesos
      eval "$(docker-machine env mesos)"
      
      (!) Add address of `docker-machine ip mesos` to /etc/hosts with next hostnames: 
      <ip address> mesos-master mesos-slave zookeeper marathon chronos  

      docker-compose up
      
After that Mesos Master, Mesos Slave and Marathon should be available:

* Mesos Master [http://mesos-master:5050](http://mesos-master:5050)
* Marathon [http://marathon:8080](http://marathon:8080)
      
## Submitting docker images to Marathon
To submit a container to Mesos via Marathon send POST request to `http://marathon:8080/v2/apps` 
([swagger ui available](http://marathon:8080/v2/apps)). Let's launch dockerized Chronos via Marathon. Because of local environment
specifics hostname resolution suffers when running docker-in-docker, so ip address in `CHRONOS_MASTER` and `CHRONOS_ZK_HOSTS` should point
to `docker-machine ip mesos`.
      
      curl -XPOST 'http://marathon:8080/v2/apps' -H 'Content-Type: application/json' -d '{
        "id": "chronos",
        "container": {
          "type": "DOCKER",
          "docker": {
            "network": "BRIDGE",
              "image": "datastrophic/chronos:mesos-0.27.1-chronos-2.5",
              "parameters": [
                   { "key": "env", "value": "CHRONOS_HTTP_PORT=4400" },
                   { "key": "env", "value": "CHRONOS_MASTER=zk://'"$(docker-machine ip mesos)"':2181/mesos" },
                   { "key": "env", "value": "CHRONOS_ZK_HOSTS='"$(docker-machine ip mesos)"':2181"}
              ],
              "portMappings": [
                { "containerPort": 4400 }
              ]
          }
        },
        "cpus": 1,
        "mem": 512,
        "instances": 1
      }'

 
## Running Spark applications

Coarse-grained mode (default) (one executor per host, amount of Mesos tasks = amount number of executors,
spark-submit registered as a framework)
 
In addition, for coarse-grained mode, you can control the maximum number of resources Spark will acquire. 
By default, it will acquire all cores in the cluster (that get offered by Mesos), which only makes sense if you 
run just one application at a time. You can cap the maximum number of cores using conf.set("spark.cores.max", "10") (for example).

      TODO: check that claims hold regarding tasks/executors  
       
      /opt/spark/bin/spark-submit \
        --class org.apache.spark.examples.SparkPi \
        --master mesos://mesos-master:5050 \
        --deploy-mode client \
        --total-executor-cores 4 \
        /opt/spark/lib/spark-examples-1.6.0-hadoop2.6.0.jar \
        1000
        
      
In “fine-grained” mode, each Spark task runs as a separate Mesos task. This allows multiple instances of Spark (and other frameworks) 
to share machines at a very fine granularity, where each application gets more or fewer machines as it ramps up and down, but it 
comes with an additional overhead in launching each task. This mode may be inappropriate for low-latency requirements like 
interactive queries or serving web requests. 
      
     /opt/spark/bin/spark-submit \
       --class org.apache.spark.examples.SparkPi \
       --master mesos://mesos-master:5050 \
       --deploy-mode client \
       --conf "spark.mesos.coarse=false"\
       --total-executor-cores 4 \
       /opt/spark/lib/spark-examples-1.6.0-hadoop2.6.0.jar \
       1000
             
             
## Submitting Spark jobs in Docker via Marathon

Running from inside mesos-slave:

      docker run -ti -e LIBPROCESS_IP=192.168.99.105 -e SPARK_PUBLIC_DNS=192.168.99.105 --net=host datastrophic/spark:mesos-0.27.1-spark-1.6 /opt/spark/bin/spark-submit --class org.apache.spark.examples.SparkPi --master mesos://172.18.0.3:5050 --deploy-mode client --conf spark.mesos.coarse=false --total-executor-cores 4 /opt/spark/lib/spark-examples-1.6.0-hadoop2.6.0.jar 100
      
Running via Marathon (todo: finalize):
      
     curl -XPOST 'http://marathon:8080/v2/apps' -H 'Content-Type: application/json' -d '{
        "id": "spark-pi-applicaion",
        "container": {
            "type": "DOCKER",
            "docker": {
                "network": "HOST",
                "image": "datastrophic/spark:mesos-0.27.1-spark-1.6",
                "parameters": [
                    { "key": "env", "value": "CHRONOS_HTTP_PORT=4400" },
                    { "key": "env", "value": "LIBPROCESS_IP='"$(docker-machine ip mesos)"'" },
                    { "key": "env", "value": "SPARK_PUBLIC_DNS='"$(docker-machine ip mesos)"'"}
                ],
                "cmd": "/opt/spark/bin/spark-submit --class org.apache.spark.examples.SparkPi --master mesos://172.18.0.3:5050 --deploy-mode client --total-executor-cores 4 /opt/spark/lib/spark-examples-1.6.0-hadoop2.6.0.jar 100"
            }
        },
        "cpus": 1,
        "mem": 512,
        "instances": 1
    }'
    
