package com.timeserieszen.wal_handlers

import java.io._
import scalaz.stream.async.mutable.Queue

private class WALFile(waldir: File, rotateSize: Long=1024*256, prefix: String = "", notifyQueue: Option[Queue[File]] = None) {
  def this(fn: String) = this(new File(fn))

  require(waldir.isDirectory(), "You must point to a directory of WAL files.")

  private var isClosed: Boolean = false
  private var outputFile: File = _
  private var filewriter: FileWriter = _
  private var rotations: Long = 0

  def numRotations = rotations

  def closed = isClosed

  private def closeFile = if (filewriter != null) {
    if (filewriter != null) {
      filewriter.flush()
      filewriter.close()
    }
    if (outputFile != null) {
      notifyQueue.foreach(_.enqueueOne(outputFile))
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
      outputFile = new File(waldir, prefix + lpadNum(System.currentTimeMillis, 22) + "_" + lpadNum(rotations,8) + ".dat")
      filewriter = new FileWriter(outputFile)
      fileSize = 0
      rotations += 1
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
  }
}
