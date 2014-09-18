package com.timeserieszen.storage

import java.io._
import java.util.UUID
import com.timeserieszen.Utils

trait AtomicStorageHandler {
  // Returns sequence of timestamps, data points
  def read(f: File): (Seq[Long], Seq[Double])

  // This function returns the first and last datapoints stored in a file.
  // This should be overridden if the storage scheme allows it.
  def minMax(f: File): (Long, Long) = {
    val ts = read(f)._1
    (ts.min, ts.max)
  }

  //Appends a sequence of times, values to the file
  def append(o: OutputStream, times: Seq[Long], values: Seq[Double]): Unit

  //Write headers to a new file
  def writeHeaders(o: OutputStream, times: Seq[Long], values: Seq[Double]): Unit

  //Update headers for an existing file
  def updateHeaders(f: File, newTimes: Seq[Long], newValues: Seq[Double]): Unit

  def stagingDir: File

  def create(destination: File, times: Seq[Long], values: Seq[Double]): Unit = {
    val newFile = new File(UUID.randomUUID().toString + ".dat")
    val o = new BufferedOutputStream(new FileOutputStream(newFile, true))
    writeHeaders(o, times, values)
    append(o, times, values)
    o.close()
    newFile.renameTo(destination) //Atomic write
  }

  def write(f: File, times: Seq[Long], values: Seq[Double]): Unit = {
    require(times.length == values.length, "Times and values must have equal length")
    val (t, v) = (times.toArray, values.toArray)
    Utils.sortSeries(t,v)

    val (minFile, maxFile) = minMax(f)
    val (minNew, maxNew) = (t(0), t(-1))
    if (minNew > maxFile) {
      val o = new BufferedOutputStream(new FileOutputStream(f, true))
      append(o, times, values)
      o.close()
      updateHeaders(f, times, values)
    } else {
      val (oldTimes, oldValues) = read(f)
      create(f, oldTimes ++ times, oldValues ++ values)
    }
  }
}
