## Overview

Docker description: how many containers of what is launched

- ZooKeeper
- Mesos master
- 3 Mesos Agents

##Prerequisites

Docker and docker-compose are used for running code samples:

      docker version 1.10
      docker-compose 1.6.0

For building the app, SBT is used      
      
      SBT 0.13

The application was created with Typesafe Activator

##Setting Dockerized Environment
### Creating docker-machine and launching containers

Depending on one's needs virtual machine memory could be adjusted to different value, but memory should be gte 4GB. Steps to create new 
docker-machine and launch docker images:  

      docker-machine create -d virtualbox --virtualbox-memory "8000" --virtualbox-cpu-count "4" mesos
      eval "$(docker-machine env workshop)"
      
      (!) Add address of `docker-machine ip workshop` to /etc/hosts with next hostnames: 
      <ip address> mesos-master mesos-slave-1 mesos-slave-2 zookeeper marathon chronos  

      docker-compose up -d

After that dockerized Cassandra, MongoDB and single-node Hadoop cluster will launched. `docker ps` 
could be used for verification and getting ids of containers. After a short delay all UI components should be accessible via browser (except SparkUI).

Project build directory is linked to hadoop container and available at `/target` folder. Every time fatjar is rebuilt it is visible inside the container.

For shutting down with deletion of all the data use `docker-compose down`
      
### List of addresses
      
* NodeManager UI [http://sandbox:8042](http://sandbox:8042)
* NameNode UI [http://sandbox:50070](http://sandbox:50070)
* Spark UI [http://sandbox:4040](http://sandbox:4040)

* HDFS root directory to access from code as `hdfs://sandbox:9000`
      
## Submitting docker images to Marathon
To submit a container to Mesos via Marathon send POST request to `http://marathon:8080/v2/apps`, for example:
       

      curl -XPOST 'http://marathon:8080/v2/apps' -H 'Content-Type: application/json' -d '{{
          "id": "ghost",
          "container": {
              "docker": {
                  "image": "ghost",
                  "network": "BRIDGE",
                  "forcePullImage": true,
                  "portMappings": [
                      {
                          "containerPort": 2368,
                          "hostPort": 0,
                          "servicePort": 2368,
                          "protocol": "tcp"
                      }
                  ]
              },
              "type": "DOCKER",
              "volumes": []
          },
          "shell": false,
          "ports": [
              2368
          ],
          "cpus": 1,
          "mem": 512,
          "instances": 1
      }
      
      curl -XPOST 'http://marathon:8080/v2/apps' -H 'Content-Type: application/json' -d '{
        "id": "spark-server",
        "container": {
          "type": "DOCKER",
          "docker": {
            "network": "BRIDGE",
              "image": "sparkserver/spark-server",
              "forcePullImage": true,
              "portMappings": [
                { "containerPort": 3000 }
              ]
          }
        },
        "cpus": 1,
        "mem": 1024,
        "instances": 1
      }'