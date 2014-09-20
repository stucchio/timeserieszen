package com.timeserieszen

import scala.collection.mutable.ArrayBuilder
import scala.reflect.ClassTag

private class SeriesBuilder[T](ident: SeriesIdent)(implicit val ct:ClassTag[T]) {
  private val timeBuffer = ArrayBuilder.make[Long]
  private val valBuffer = ArrayBuilder.make[T]

  def append(t: Long, v: T) = {
    timeBuffer += t
    valBuffer += v
  }

  def append(kv: (Long, T)) = {
    timeBuffer += kv._1
    valBuffer += kv._2
  }

  def result(): Series[T] = BufferedSeries(ident, timeBuffer.result(), valBuffer.result())

}
