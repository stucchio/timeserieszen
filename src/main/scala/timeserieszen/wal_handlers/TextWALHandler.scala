package com.timeserieszen.wal_handlers

import com.timeserieszen._
import com.timeserieszen.monitoring._

import scalaz._
import Scalaz._
import scalaz.stream._
import scalaz.concurrent._
import scalaz.stream.async.mutable.Queue
import scala.collection.mutable.{HashMap, ArrayBuffer}
import java.io._

case class TextWALHandler(waldir: java.io.File, override val rotateSize: Long = 1024*256, override val walPrefix: String = "", queueSize: Int = 0) extends WALHandler[Double] with Logging with Metrics with WALFile {
  /**
    * rotateSize - when the size of a WAL file *approximately* exceeds this, the log will be rotated.
    *  prefix    - prefix the WAL files with this
    * queueSize  - After the WAL files are written, a queue of completed WAL files is maintained.
    *              A separate process should drain this queue and process the files.
   */
  def this(fn: String) = this(new java.io.File(fn))

  log.info("Created {} with directory {}", this.getClass: Any, waldir: Any)

  require(queueSize >= 0, "Queue size cannot be negative.")

  //Metrics
  protected val metricPrefix: String = "text_wal_handler"
  private val dataPoints = counter("datapoints")

  // val writer = io.resource[WALFile,DataPoint[Double] => Task[Unit]](Task.delay { this }
  // see https://github.com/scalaz/scalaz-stream/blob/master/src/main/scala/scalaz/stream/io.scala#L149
  val writer = io.resource[Task,WALFile,DataPoint[Double] => Task[Unit]](Task.delay { this }
      )( (f:WALFile) => Task.delay {
        f.close()
        topic.close.run
        log.info("Terminated writer with dir {}", waldir)
      }
      )( (f:WALFile) => Task.now {(d:DataPoint[Double]) => Task.delay {
        f.write(Utils.datapointToString(d))
        topic.publishOne( d ).run // publish to the topic after flush.
        dataPoints.inc()
      } })

  val topic = async.topic[DataPoint[Double]]()

  def reader = {
    Process.emitAll(waldir.list().toSeq.sorted).flatMap( fn => {
      val file = new File(waldir, fn)
      io.linesR(file.toString)
    } ).map(Utils.stringToDatapoint _)
  }

  private val queue = async.boundedQueue[File](queueSize)
  override protected val notifyQueue = Some(queue)

  import WALHandler._

  private val startingProcesses: Process[Task,File] = Process.emitAll(waldir.listFiles())

  def flushedSeries: Process[Task, FileRemover \/ Series[Double]] = (startingProcesses ++ queue.dequeue).flatMap(f => {
    val serieses = scala.collection.mutable.HashMap[SeriesIdent, SeriesBuilder[Double]]()

    //Read all the series in the WAL
    io.linesR(f.toString).map(Utils.stringToDatapoint _).map(dp => {
      dp.iterator.foreach( kv => {
        val (k,v) = kv
        if (!serieses.contains(k)) {
          serieses += ((k, new SeriesBuilder(k)))
        }
        serieses(k).append(dp.timestamp, v)
      })
    }).run.run

    //Now build a list of the series
    val data = new Array[FileRemover \/ Series[Double]](serieses.size)
    var i=0
    serieses.values.foreach(sb => {
      data(i) = sb.result().right
      i += 1
    })
    val deleter: (() => Unit) = (() => f.delete())
    //Finally emit
    Process.emitAll(data) ++ Process.emit( deleter.left )
  })
}
