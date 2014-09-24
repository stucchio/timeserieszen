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
import org.scalacheck.Prop.BooleanOperators

object TextWALHandlerSpec extends Properties("TextWALHandler") {
  property("to and from file") = forAllNoShrink(arbitrary[Seq[DataPoint[Double]]])((m: Seq[DataPoint[Double]]) => {
    withTempDir(f => {
      val wal = new TextWALHandler(f)

      if (m.size > 0) {
        //Dump to file
        Process.emitAll(m).to(wal.writer).run.run
        //Read from file
        val result = wal.reader.runLog.run
        (result == m) :| "Datapoint written != datapoint read"
      } else { //Nothing will be written for no data coming in
        true :| "vacuous"
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
        (result == m) :| "Datapoint written != datapoint read"
      } else { //Nothing will be written for no data coming in
        true :| "vacuous"
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
        (flushedIdents == originalIdents) :| "Datapoint written != datapoint read"
      } else { //Nothing will be written for no data coming in
        true :| "vacuous"
      }
    })
  })

  property("series are published to the topic") = forAllNoShrink(arbitrary[Seq[DataPoint[Double]]])((m: Seq[DataPoint[Double]]) => {
    if (m.size > 0) {
      withTempDir(f => {
        val wal = new TextWALHandler(f, queueSize=0)
        val result = new WaitFor[Seq[DataPoint[Double]]]
        wal.topic.subscribe.runLog.runAsync( _.fold(_ => ???, x => {result.put(x)}) )
        Process.emitAll(m).to(wal.writer).run.run

        (result() == m) :| "Datapoint written != datapoint read"
      })
    } else { //Nothing will be written for no data coming in
      true :| "vacuous"
    }
  })

}
