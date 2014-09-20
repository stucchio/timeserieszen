package com.timeserieszen.storage

import java.io._
import java.util.UUID
import com.timeserieszen.Utils

private trait AtomicStorageHandler {
  // Returns sequence of timestamps, data points
  def read(f: File): (Array[Long], Array[Double])

  // This function returns the first and last datapoints stored in a file.
  // This should be overridden if the storage scheme allows it.
  protected def minMax(f: File): (Long, Long) = {
    val ts = read(f)._1
    (ts.min, ts.max)
  }

  //Appends a sequence of times, values to the file
  protected def append(o: RandomAccessFile, times: Seq[Long], values: Seq[Double]): Unit

  //Write headers to a new file
  protected def writeHeaders(o: RandomAccessFile, times: Seq[Long], values: Seq[Double]): Unit

  def create(destination: File, stagingDir: File, times: Seq[Long], values: Seq[Double]): Unit = {
    val newFile = new File(stagingDir, UUID.randomUUID().toString + ".dat")
    withFile(newFile)(o => {
      val (t, v) = (times.toArray, values.toArray)
      Utils.sortSeries(t,v)
      writeHeaders(o, t, v)
      append(o, t, v)
    })
    newFile.renameTo(destination) //Atomic write
  }

  protected def withFile[T](f: File)(proc: RandomAccessFile => T): T = {
    val o = new RandomAccessFile(f, "rw")
    try {
      proc(o)
    } finally {
      o.close()
    }
  }

  def write(f: File, stagingDir: File, times: Seq[Long], values: Seq[Double]): Unit = {
    /* This method and read are the public interface of this method.
     *
     */
    require(times.length == values.length, "Times and values must have equal length")
    val (t, v) = (times.toArray, values.toArray)
    Utils.sortSeries(t,v)

    if (f.exists()) {
      val (minFile, maxFile) = minMax(f)
      val (minNew, maxNew) = (t(0), t(t.size-1))
      if (minNew > maxFile) { //This is an easy append
        withFile(f)(o => {
          writeHeaders(o, times, values)
          append(o, times, values)
        })
      } else { //The file requires rewriting
        val (oldTimes, oldValues) = read(f)
        create(f, stagingDir, oldTimes ++ times, oldValues ++ values)
      }
    } else {
      create(f, stagingDir, times, values)
    }
  }
}
