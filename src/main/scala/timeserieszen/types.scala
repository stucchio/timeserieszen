package com.timeserieszen

import scalaz._
import scalaz.stream._
import scalaz.concurrent._
import org.joda.time.DateTime

case class SeriesIdent(name: String) extends AnyVal

sealed trait Series[T] {
  def ident: SeriesIdent
  def data: Seq[(Long,T)]
  def times: Seq[Long] = data.map(_._1)
  def values: Seq[T] = data.map(_._2)
  def length = data.size
}

case class BufferedSeries[T](ident: SeriesIdent, timesA: Array[Long], valuesA: Array[T]) extends Series[T] {
  require(timesA.size == valuesA.size)
  require(timesA.size > 0)
  override val times = timesA.toSeq
  override val values = valuesA.toSeq
  override lazy val data = {
    val result = new Array[(Long,T)](timesA.size)
    var i=0
    while (i < timesA.size) {
      result(i) = (timesA(i), valuesA(i))
      i += 1
    }
    result.toSeq
  }
}

case class BoxedSeries[T](ident: SeriesIdent, data: Seq[(Long,T)]) extends Series[T]

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
