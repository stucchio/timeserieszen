package com.timeserieszen.wal_handlers

import com.timeserieszen._
import scalaz.stream._
import scalaz.concurrent._
import java.io._

class TextWALHandler(file: java.io.File) extends WALHandler[Double] {
  def this(fn: String) = this(new java.io.File(fn))

  def writer = io.resource[FileWriter,DataPoint[Double] => Task[Unit]](Task.delay { new FileWriter(file, true) })( (f:FileWriter) => Task.delay {f.close()} )( (f:FileWriter) => Task.now {(d:DataPoint[Double]) => Task.delay { f.write(Utils.datapointToString(d)); f.flush() } })

  def reader = io.linesR(file.toString).map(Utils.stringToDatapoint _)

}
