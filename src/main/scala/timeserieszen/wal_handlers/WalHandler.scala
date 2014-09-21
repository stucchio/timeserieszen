package com.timeserieszen.wal_handlers

import com.timeserieszen._

import scalaz._
import scalaz.stream._
import scalaz.concurrent._

object WALHandler {
  type FileRemover = (() => Unit)
}

trait WALHandler[T] {
  def writer: Sink[Task,DataPoint[T]]
  def flushedSeries: Process[Task,WALHandler.FileRemover \/ Series[T]]
  def reader: Process[Task,DataPoint[T]]
}
