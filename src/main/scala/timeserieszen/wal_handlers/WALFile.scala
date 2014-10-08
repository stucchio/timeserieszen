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
  private var rotations: Long = 0
  protected var output: Option[(File, OutputStream)] = None //This should NOT be used by subclasses - protected vs private is only to prevent weird AbstractMethod errors

  def numRotations = rotations

  def closed = isClosed

  private val rotationCounter = counter("rotations")

  var numEnqueued = 0
  private def closeFile = output.foreach( (p) => {
    val (of, os) = p
    os.flush()
    os.close()
    notifyQueue.foreach(q => q.enqueueOne(of).run)
  })

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
      val oldfile = output.map(_._1)
      val outputFile = new File(waldir, walPrefix + lpadNum(System.currentTimeMillis, 22) + "_" + lpadNum(rotations,8) + ".dat")
      val outputstream = new FileOutputStream(outputFile)
      output = Some((outputFile, outputstream))
      fileSize = 0
      rotations += 1
      oldfile.map( of => log.info("Rotated old WAL file {}, new WAL file is {}", Seq(of, outputFile): _*) ).getOrElse({ log.info("Created first WAL file is {}", Seq(outputFile): _*)  })
      rotationCounter.inc()
    } else {
      throw new java.io.IOException("Writer is closed")
    }
  }

  private var fileSize: Long = 0
  private val CHARSET = java.nio.charset.Charset.forName("UTF-8")

  def write(s: String) = {
    if (output.isEmpty) {
      rotate
    }
    fileSize += s.length
    val (_, outputstream) = output.get
    outputstream.write(s.getBytes(CHARSET))
    outputstream.flush()
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
