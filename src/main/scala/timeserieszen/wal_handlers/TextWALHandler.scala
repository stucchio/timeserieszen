package com.timeserieszen.wal_handlers

import com.timeserieszen._

import scalaz._
import Scalaz._
import scalaz.stream._
import scalaz.concurrent._
import scalaz.stream.async.mutable.Queue
import scala.collection.mutable.{HashMap, ArrayBuffer}
import java.io._

case class TextWALHandler(waldir: java.io.File, rotateSize: Long = 1024*256, prefix: String = "", val queueSize: Int = 32) extends WALHandler[Double]{
  def this(fn: String) = this(new java.io.File(fn))

  val writer = io.resource[WALFile,DataPoint[Double] => Task[Unit]](Task.delay { new WALFile(waldir, rotateSize, prefix, Some(queue)) }
      )( (f:WALFile) => Task.delay { f.close() }
      )( (f:WALFile) => Task.now {(d:DataPoint[Double]) => Task.delay { f.write(Utils.datapointToString(d)) } })

  def reader = {
    Process.emitAll(waldir.list().toSeq.sorted).flatMap( fn => {
      val file = new File(waldir, fn)
      io.linesR(file.toString)
    } ).map(Utils.stringToDatapoint _)
  }

  protected lazy val queue = async.boundedQueue[File](queueSize)

  import WALHandler._

  def flushedSeries: Process[Task, FileRemover \/ Series[Double]] = queue.dequeue.flatMap(f => {
    val serieses = scala.collection.mutable.HashMap[SeriesIdent, SeriesBuilder[Double]]()

    io.linesR(f.toString).map(Utils.stringToDatapoint _).map(dp => {
      dp.iterator.foreach( kv => {
        val (k,v) = kv
        if (!serieses.contains(k)) {
          serieses += ((k, new SeriesBuilder(k)))
        }
        serieses(k).append(dp.timestamp, v)
      })
    }).run.run

    val data = new Array[FileRemover \/ Series[Double]](serieses.size)
    var i=0
    serieses.values.foreach(sb => {
      data(i) = sb.result().right
      i += 1
    })
    val deleter: () => Unit = () => f.delete()
    Process.emitAll(data.toSeq) ++ Process.emit( deleter.left )
  })
}
