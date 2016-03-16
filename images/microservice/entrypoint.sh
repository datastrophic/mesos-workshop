#!/bin/bash

exec java -cp /akka-microservice.jar akka.microservice.AkkaMicroservice --service-port 8088 --cassandra-host $CASSANDRA_HOST --keyspace $CASSANDRA_KEYSPACE
--table $CASSANDRA_TABLE