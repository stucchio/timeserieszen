package com.timeserieszen.storage

import com.timeserieszen._
import com.timeserieszen.storage._

import java.io.File
import java.util.UUID
import org.scalacheck._
import scalaz._
import Scalaz._
import scalaz.stream._
import scalacheck.ScalazProperties._
import Arbitrary.arbitrary
import Prop._
import TestHelpers._

object SequentialBinaryV1StorageHandlerSpec extends Properties("SequentialBinaryV1StorageHandler") {
  property("to and from file") = forAllNoShrink(genArrayForSeries)( data => {
    withTempDir(f => {
      val stagingDir = new File(f, "staging")
      stagingDir.mkdir()
      val dataDir = new File(f, "data")
      dataDir.mkdir()
      val dataFile = new File(dataDir, UUID.randomUUID().toString + ".dat")
      SequentialBinaryV1StorageHandler.write(dataFile, stagingDir, data._1, data._2)
      val (inTimes, inValues) = SequentialBinaryV1StorageHandler.read(dataFile)
      require(data._1.toSeq == inTimes.toSeq, "times do not match")
      require(data._2.toSeq == inValues.toSeq, "values do not match")
      true
    })
  })
  property("to and from file + append, no rewrite") = forAllNoShrink(genArrayForSeries)( data => {
    withTempDir(f => {
      val stagingDir = new File(f, "staging")
      stagingDir.mkdir()
      val dataDir = new File(f, "data")
      dataDir.mkdir()
      val dataFile = new File(dataDir, UUID.randomUUID().toString + ".dat")

      val N = data._1.size
      val cutPoint = data._1.size/2
      val (firstTimes, firstValues) = (data._1.slice(0,cutPoint), data._2.slice(0,cutPoint))
      val (lastTimes, lastValues) = (data._1.slice(cutPoint,N), data._2.slice(cutPoint, N))
      SequentialBinaryV1StorageHandler.write(dataFile, stagingDir, firstTimes, firstValues)
      SequentialBinaryV1StorageHandler.write(dataFile, stagingDir, lastTimes, lastValues)
      val (inTimes, inValues) = SequentialBinaryV1StorageHandler.read(dataFile)
      require(data._1.toSeq == inTimes.toSeq, "times do not match")
      require(data._2.toSeq == inValues.toSeq, "values do not match")
      true
    })
  })
  property("to and from file + append, with rewrite") = forAllNoShrink(for {
    x <- genArrayForSeries
    y <- genArrayForSeries
  } yield (x,y))( data2 => {
    withTempDir(f => {
      val stagingDir = new File(f, "staging")
      stagingDir.mkdir()
      val dataDir = new File(f, "data")
      dataDir.mkdir()
      val dataFile = new File(dataDir, UUID.randomUUID().toString + ".dat")

      val (t1, v1) = data2._1
      val (t2, v2) = data2._2
      var i=0 //Interleave the timestamps
      while(i < t1.size) {
        t1(i) = t1(i)*2
        i += 1
      }
      i = 0
      while(i < t2.size) {
        t2(i) = t2(i)*2+1
        i += 1
      }
      val (t, v) = ((t1 ++ t2).toArray, (v1 ++ v2).toArray)
      Utils.sortSeries(t,v)

      SequentialBinaryV1StorageHandler.write(dataFile, stagingDir, t1, v1)
      SequentialBinaryV1StorageHandler.write(dataFile, stagingDir, t2, v2)
      val (inTimes, inValues) = SequentialBinaryV1StorageHandler.read(dataFile)

      require(t.toSeq == inTimes.toSeq, "times do not match")
      require(v.toSeq == inValues.toSeq, "values do not match")
      true
    })
  })
}
