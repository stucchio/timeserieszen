package com.timeserieszen.listener

import scalaz.concurrent._
import scalaz.stream._
import java.net.{DatagramSocket, DatagramPacket, InetAddress}

object UDP {

  private class UDPReceiver(port: Int, bufferSize: Int) {
    val socket = new DatagramSocket(port)
    val datagram = new DatagramPacket(new Array[Byte](bufferSize), bufferSize)

    def close = socket.close()
  }

  sealed trait UDPMessage
  case object SocketClosed extends UDPMessage
  case class UDPPacket(data: Array[Byte], ip: InetAddress, port: Int) extends UDPMessage {
    def getString(encoding: String = "UTF-8"): String = new String(data, encoding)
  }
  case class UDPError(exception: java.io.IOException) extends UDPMessage

  def listen(port: Int, bufferSize: Int = 1024*4): Process[Task, UDPMessage] = {
    val acquire = Task { new UDPReceiver(port, bufferSize) }
    def flushAndRelease(u: UDPReceiver) = Task {
      u.close
      SocketClosed
    }
    def step(r: UDPReceiver): Task[UDPMessage] = Task {
      try {
        r.socket.receive(r.datagram)
        UDPPacket(java.util.Arrays.copyOf(r.datagram.getData(), r.datagram.getLength() ), r.datagram.getAddress, r.datagram.getPort)
      } catch {
        case (e:java.io.IOException) => UDPError(e)
      }
    }
    io.bufferedResource[UDPReceiver, UDPMessage](acquire)(flushAndRelease _)(step _)
  }
}
