#!/bin/bash

java -cp /throttle/throttle-framework.jar -Dexecutor.path=/throttle/throttle-executor.sh io.datastrophic.mesos.Throttler \
--mesos-master zk://zookeeper:2181/mesos \
--cassandra-host cassandra \
--keyspace demo_framework \
--total-queries 100 \
--queries-per-task 5 \
--parallelism 5 \
--mode fine-grained

java -cp /throttle/throttle-framework.jar -Dexecutor.path=/throttle/throttle-executor.sh io.datastrophic.mesos.Throttler \
--mesos-master zk://zookeeper:2181/mesos \
--cassandra-host cassandra \
--keyspace demo_framework \
--total-queries 100 \
--queries-per-task 5 \
--parallelism 5 \
--mode node-local