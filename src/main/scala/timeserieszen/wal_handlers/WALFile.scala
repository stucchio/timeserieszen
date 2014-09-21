package com.timeserieszen.wal_handlers

import com.timeserieszen.monitoring._

import java.io._
import scalaz.stream.async.mutable.Queue

trait WALFile extends Logging with Metrics {
  protected val waldir: File
  protected val rotateSize: Long=1024*256
  protected val walPrefix: String = ""
  protected val notifyQueue: Option[Queue[File]] = None

  require(waldir.isDirectory(), "You must point to a directory of WAL files.")

  log.info("Logging WAL files to {}", waldir)

  private var isClosed: Boolean = false
  private var outputFile: File = _
  private var filewriter: FileWriter = _
  private var rotations: Long = 0

  def numRotations = rotations

  def closed = isClosed

  private val rotationCounter = counter("rotations")

  var numEnqueued = 0
  private def closeFile = if (filewriter != null) {
    if (filewriter != null) {
      filewriter.flush()
      filewriter.close()
    }
    if (outputFile != null) {
      notifyQueue.foreach(q => q.enqueueOne(outputFile).run)
    }
  }

  private def lpadNum(x: Long, n: Int) = {
    /* Probably inefficient, but doesn't matter much.*/
    var pad = ""
    val s = x.toString
    var i = s.length
    while (i < n) {
      pad = pad + "0"
      i += 1
    }
    pad + s
  }

  private def rotate = {
    closeFile
    if (!isClosed) {
      val oldfile = outputFile
      outputFile = new File(waldir, walPrefix + lpadNum(System.currentTimeMillis, 22) + "_" + lpadNum(rotations,8) + ".dat")
      filewriter = new FileWriter(outputFile)
      fileSize = 0
      rotations += 1
      log.info("Rotated old WAL file {}, new WAL file is {}", Seq(oldfile, outputFile): _*)
      rotationCounter.inc()
    } else {
      throw new java.io.IOException("Writer is closed")
    }
  }
  rotate

  var fileSize: Long = 0

  def write(s: String) = {
    fileSize += s.length
    filewriter.write(s)
    filewriter.flush()
    if (fileSize > rotateSize) {
      rotate
    }
  }

  def close() = {
    closeFile
    isClosed = true
    notifyQueue.map(_.close.run)
  }
}
