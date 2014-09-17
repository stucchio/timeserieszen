package com.timeserieszen.wal_handlers

import com.timeserieszen._
import scalaz.stream._
import scalaz.concurrent._
import scalaz.stream.async.mutable.Queue
import java.io._

case class TextWALHandler(waldir: java.io.File, rotateSize: Long = 1024*256, prefix: String = "", notifyQueue: Option[Queue[File]] = None) extends WALHandler[Double] {
  def this(fn: String) = this(new java.io.File(fn))

  val writer = io.resource[WALFile,DataPoint[Double] => Task[Unit]](Task.delay { new WALFile(waldir, rotateSize, prefix, notifyQueue) }
      )( (f:WALFile) => Task.delay {f.close()}
      )( (f:WALFile) => Task.now {(d:DataPoint[Double]) => Task.delay { f.write(Utils.datapointToString(d)) } })

  def reader = {
    Process.emitAll(waldir.list().toSeq.sorted).flatMap( fn => {
      val file = new File(waldir, fn)
      io.linesR(file.toString)
    } ).map(Utils.stringToDatapoint _)
  }
}
