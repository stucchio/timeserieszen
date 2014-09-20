package com.timeserieszen.storage

import java.io._
import java.nio.ByteBuffer
import java.util.UUID
import com.timeserieszen.Utils

class SequentialBinaryV1Storage(dataDir: File, stagingDir: File) extends SeriesStorageFromAtomic(dataDir, stagingDir, SequentialBinaryV1StorageHandler)

private object SequentialBinaryV1StorageHandler extends AtomicStorageHandler {
  final val VERSION_TAG: Long = 1 //NEVER CHANGE THIS, NEVER BUILD ANOTHER ATOMICSTORAGEHANDLER WITH THE SAME FORMAT TAG

  def read(f: File): (Array[Long], Array[Double]) = {
    withFile(f)(input => {
      val fileLength = input.length()
      val versionTag = input.readLong()
      require(versionTag == VERSION_TAG, "Invalid format")
      val start = input.readLong()
      val end = input.readLong()
      val numRecords = ((fileLength - 8*3) / 16).toInt //THIS IS WRONG WILL FAIL HORRIBLY FOR HUGE FILES
      var i=0
      val times = new Array[Long](numRecords)
      val values = new Array[Double](numRecords)
      while (i < numRecords) {
        times(i) = input.readLong()
        values(i) = input.readDouble()
        i += 1
      }
      (times, values)
    })
  }

  override protected def minMax(f: File): (Long, Long) =     withFile(f)(input => {
    val versionTag = input.readLong()
    require(versionTag == VERSION_TAG, "Invalid format")
    val start = input.readLong()
    val end = input.readLong()
    (start, end)
  })


  protected def writeHeaders(o: RandomAccessFile, times: Seq[Long], values: Seq[Double]): Unit = {
    o.seek(0)
    o.writeLong(VERSION_TAG)
    o.writeLong(times(0))
    o.writeLong(times(times.size-1))
  }

  protected def append(o: java.io.RandomAccessFile, times: Seq[Long],values: Seq[Double]): Unit = {
    require(times.size == values.size, "Times and values must be same size")
    val length = o.length()
    o.seek(length)
    var i=0
    var lastTime: Long = times(0)-1
    while (i < times.size) {
      if (times(i) != lastTime) { //Do NOT write duplicate times to the file
        o.writeLong(times(i))
        o.writeDouble(values(i))
        lastTime = times(i)
      }
      i += 1
    }
  }

}
