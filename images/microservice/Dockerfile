FROM java:openjdk-8u72-jre

COPY akka-microservice.jar /

EXPOSE 31337

CMD java -cp /akka-microservice.jar io.datastrophic.microservice.AkkaMicroservice --service-port 31337 --cassandra-host $CASSANDRA_HOST --keyspace $CASSANDRA_KEYSPACE --table $CASSANDRA_TABLE