package io.datastrophic.mesos

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

trait BinarySerDe {
   def serialize[T](o: T): Array[Byte] = {
      val bos = new ByteArrayOutputStream
      val oos = new ObjectOutputStream(bos)
      oos.writeObject(o)
      oos.close
      return bos.toByteArray
   }

   def deserialize[T](bytes: Array[Byte]): T = {
      val bis = new ByteArrayInputStream(bytes)
      val ois = new ObjectInputStream(bis)
      return ois.readObject.asInstanceOf[T]
   }
}
