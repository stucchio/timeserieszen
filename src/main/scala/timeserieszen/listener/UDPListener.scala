package com.timeserieszen.listener

import com.timeserieszen._
import scalaz.concurrent._
import scalaz.stream._
import java.net.{DatagramSocket, DatagramPacket, InetAddress}

private object UDP {
  sealed trait UDPMessage
  case object SocketClosed extends UDPMessage
  case class UDPPacket(data: Array[Byte], ip: java.net.InetAddress, port: Int) extends UDPMessage {
    def getString(encoding: String = "UTF-8"): String = new String(data, encoding)
  }
  case class UDPError(exception: java.io.IOException) extends UDPMessage

  def listenUDP(port: Int, bufferSize: Int = 1024*4): Process[Task, UDPMessage] = {
    val acquire = Task { (new java.net.DatagramSocket(port), new java.net.DatagramPacket(new Array[Byte](bufferSize), bufferSize)) }
    def flushAndRelease(u: (java.net.DatagramSocket, java.net.DatagramPacket)): Task[UDPMessage] = Task { u._1.close; SocketClosed }
    def step(r: (java.net.DatagramSocket, java.net.DatagramPacket)): Task[UDPMessage] = Task {
      try {
        r._1.receive(r._2)
        UDPPacket(java.util.Arrays.copyOf(r._2.getData(), r._2.getLength() ), r._2.getAddress, r._2.getPort)
      } catch {
        case (e:java.io.IOException) => UDPError(e)
      }
    }
    io.bufferedResource(acquire)(flushAndRelease _)(step _)
  }
}

object UDPListener {
  private def packetToMessage(packet: UDP.UDPMessage): Process[Task,DataPoint[Double]] = packet match {
    case UDP.UDPPacket(data, _, _) => {
      try {
        Process.emit(Utils.stringToDatapoint(new String(data)))
      } catch {
        case (_:Throwable) => Process.halt
      }
    }
    case _ => Process.halt
  }

  def apply(port: Int): Process[Task,DataPoint[Double]] = UDP.listenUDP(port).flatMap( packetToMessage _)
}
