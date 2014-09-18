package com.timeserieszen

import scalaz.stream._
import scalaz.concurrent._
import org.joda.time.DateTime

case class SeriesIdent(name: String) extends AnyVal

sealed trait DataPoint[T] {
  def timestamp: Long //UTC time, java style
  def time: DateTime = new DateTime(timestamp)
  def data: Map[SeriesIdent, T]
  def iterator: Iterator[(SeriesIdent,T)] = data.iterator
  def numSeries: Int
}

object DataPoint {
  def apply[T](timestamp: Long, identifiers: Seq[SeriesIdent], values: Seq[T]): DataPoint[T] = FlatDataPoint(timestamp, identifiers, values)
  def apply[T](timestamp: Long, data: Map[SeriesIdent, T]): DataPoint[T] = SimpleDatapoint(timestamp, data)

  implicit object Ordering extends Ordering[DataPoint[_]] {
  def compare(a: DataPoint[_], b: DataPoint[_]) = a.timestamp.compare(b.timestamp)
  }
}

case class SimpleDatapoint[T](timestamp: Long, data: Map[SeriesIdent, T]) extends DataPoint[T] {
  require(!data.isEmpty, "cannot have empty datapoint")
  def numSeries = data.size
}

case class FlatDataPoint[T](timestamp: Long, identifiers: Seq[SeriesIdent], values: Seq[T]) extends DataPoint[T] {
  /* This will work best if the identifiers/values are stored as arrays. */
  require(identifiers.size == values.size, "Identifiers and values must have same size")
  require(!data.isEmpty, "cannot have empty datapoint")
  lazy val data = identifiers.zip(values).toMap
  def numSeries = identifiers.size
}

trait WALHandler[T] {
  def writer: Sink[Task,DataPoint[T]]
  def reader: Process[Task,DataPoint[T]]
}
