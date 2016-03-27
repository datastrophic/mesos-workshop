#!/bin/bash

java -cp /throttle/throttle-framework.jar -Dexecutor.path=/throttle/throttle-executor.sh io.datastrophic.mesos.Throttler \
--mesos-master zk://zookeeper:2181/mesos \
--cassandra-host cassandra \
--keyspace demo_framework \
--total-queries 100 \
--queries-per-task 5 \
--parallelism 5

java -cp /throttle/throttle-framework.jar -Dexecutor.path=/throttle/drf-executor.sh io.datastrophic.mesos.drf.DRFDemoFramework \
--mesos-master zk://zookeeper:2181/mesos \
--framework-name 'Framework A' \
--task-cpus 2 \
--task-memory 1000

java -cp /throttle/throttle-framework.jar -Dexecutor.path=/throttle/drf-executor.sh io.datastrophic.mesos.drf.DRFDemoFramework \
--mesos-master zk://zookeeper:2181/mesos \
--framework-name 'Framework B' \
--task-cpus 1 \
--task-memory 2500

java -cp /throttle/throttle-framework.jar -Dexecutor.path=/throttle/drf-executor.sh io.datastrophic.mesos.drf.DRFDemoFramework \
--mesos-master zk://zookeeper:2181/mesos \
--framework-name 'Framework C' \
--task-cpus 0.5 \
--task-memory 512