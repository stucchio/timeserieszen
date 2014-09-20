package com.timeserieszen.wal_handlers

import com.timeserieszen._

import org.scalacheck._
import scalaz._
import Scalaz._
import scalaz.stream._
import scalaz.concurrent._
import scalacheck.ScalazProperties._
import Arbitrary.arbitrary
import Prop._
import TestHelpers._

object TextWALHandlerSpec extends Properties("TextWALHandler") {
  property("to and from file") = forAllNoShrink(arbitrary[Seq[DataPoint[Double]]])((m: Seq[DataPoint[Double]]) => {
    withTempDir(f => {
      val wal = new TextWALHandler(f)

      if (m.size > 0) {
        //Dump to file
        Process.emitAll(m).to(wal.writer).run.run
        //Read from file
        val result = wal.reader.runLog.run
        result == m
      } else { //Nothing will be written for no data coming in
        true
      }
    })
  })

  property("to and from file, make sure rotation occurs") = forAllNoShrink(arbitrary[Seq[DataPoint[Double]]])((m: Seq[DataPoint[Double]]) => {
    withTempDir(f => {
      val wal = new TextWALHandler(f, rotateSize = 256)

      if (m.size > 0) {
        //Dump to file
        Process.emitAll(m).to(wal.writer).run.run
        //Read from file
        val result = wal.reader.runLog.run
        result == m
      } else { //Nothing will be written for no data coming in
        true
      }
    })
  })

  property("all series are flushed when wal files are closed") = forAllNoShrink(arbitrary[Seq[DataPoint[Double]]])((m: Seq[DataPoint[Double]]) => {
    withTempDir(f => {
      val wal = new TextWALHandler(f, queueSize=0)
      if (m.size > 0) {
        //Dump to file
        Process.emitAll(m).to(wal.writer).run.run
        //Read from file
        val fromSyncedSeries = wal.flushedSeries.runLog.run
        val originalIdents = Set(m.flatMap( _.data.keys ): _*)
        val flushedIdents = fromSyncedSeries.flatMap( _.fold(_ => None, x => Some(x.ident)) ).toSet
        flushedIdents == originalIdents
      } else { //Nothing will be written for no data coming in
        true
      }
    })
  })

}
