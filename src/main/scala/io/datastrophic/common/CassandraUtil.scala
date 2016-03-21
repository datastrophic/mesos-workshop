package io.datastrophic.common

import com.datastax.driver.core.{Cluster, Session}

object CassandraUtil {

   def buildSession(host: String): Session = {
      Cluster.builder()
      .addContactPoint(host)
      .build()
      .connect()
   }
}
