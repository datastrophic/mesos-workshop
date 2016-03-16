FROM java:openjdk-8u72-jre

COPY akka-microservice.jar /

#COPY entrypoint.sh /

EXPOSE 8088

CMD java -cp /akka-microservice.jar akka.microservice.AkkaMicroservice --service-port 8088 --cassandra-host $CASSANDRA_HOST --keyspace $CASSANDRA_KEYSPACE --table $CASSANDRA_TABLE

#ENTRYPOINT ["/entrypoint.sh"]