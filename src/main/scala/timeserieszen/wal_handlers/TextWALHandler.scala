package com.timeserieszen.wal_handlers

import com.timeserieszen._
import scalaz.stream._
import scalaz.concurrent._
import java.io._

class TextWALHandler(file: java.io.File) extends WALHandler[Double] {
  def this(fn: String) = this(new java.io.File(fn))

  private def datapointToString(d: DataPoint[Double]): String = {
    val sb = new StringBuilder()
    d.iterator.foreach(kv => {
      sb.append(kv._1.name)
      sb.append(" ")
      sb.append(kv._2)
      sb.append(" ")
    })
    sb.append(d.timestamp)
    sb.append("\n")
    sb.toString()
  }

  private def stringToDatapoint(s: String): DataPoint[Double] = {
    val spl = s.split(' ')
    if (spl.length % 2 != 1) {
      throw new IllegalArgumentException("Line was badly formatted: \n" + s)
    }
    val n = (spl.length - 1)/2
    val idents = new Array[SeriesIdent](n)
    val values = new Array[Double](n)
    var i=0
    while (i < n) {
      idents(i) = SeriesIdent(spl(2*i))
      values(i) = spl(2*i+1).toDouble
      i += 1
    }
    FlatDataPoint(spl(spl.length-1).toLong, idents, values)
  }

  def writer = io.resource[FileWriter,DataPoint[Double] => Task[Unit]](Task.delay { new FileWriter(file, true) })( (f:FileWriter) => Task.delay {f.close()} )( (f:FileWriter) => Task.now {(d:DataPoint[Double]) => Task.delay { f.write(datapointToString(d)) } })

  def reader = io.linesR(file.toString).map(stringToDatapoint _)

}
