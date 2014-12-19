package com.timeserieszen.retrieval

import org.scalacheck._
import scalaz._
import Scalaz._
import Arbitrary.arbitrary
import Prop._
import com.timeserieszen.storage.{SeriesStorage, SeriesStorageError, SeriesMissing}
import com.timeserieszen.{Series, BufferedSeries, SeriesIdent, TestHelpers}
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat,ISODateTimeFormat, DateTimeFormatter}
import org.http4s.server.MockServer._
import org.http4s._
import org.http4s.server._
import org.json4s._
import org.json4s.native.Serialization

// Probably look: https://github.com/http4s/http4s/blob/master/server/src/test/scala/org/http4s/server/MockServerSpec.scala
// To figure out how to test the webserver.
object HttpRetrieverSpec extends Properties("HttpRetriever") {
  import TestHelpers._

  trait MockSeriesStorageReadOnly[T] extends SeriesStorage[T] {
    def write(series: Series[T]) = ???
    def append(series: Series[T]) = ???
  }

  val emptyRetriever = new MockServer(new HttpRetriever(new MockSeriesStorageReadOnly[Double] {
    def read(ident: SeriesIdent): Validation[SeriesStorageError,Series[Double]] = SeriesMissing.fail[Series[Double]]
  }).service)

  property("empty retriever yields 404s") = forAll( (ident:SeriesIdent) => {
    val req = Request(method=Method.GET, uri=Uri(path="/get/" + ident))
    emptyRetriever(req).run.statusLine == Status.NotFound
  })

  property("empty retriever yields 404s pt 2") = forAll( (ident:SeriesIdent) => {
    val req = Request(method=Method.GET, uri=Uri(path="/render?target=" + ident + "&from=07to=1"))
    emptyRetriever(req).run.statusLine == Status.NotFound
  })


  private def nonEmptyRetrieverData(ident: SeriesIdent) = BufferedSeries(ident, List[Long](1,2,3,4,5,6,7,8).toArray, List[Double](1,2,1,3,1,4,1,5).toArray)
  val nonEmptyRetrieverInner = new HttpRetriever(new MockSeriesStorageReadOnly[Double] {
    def read(ident: SeriesIdent): Validation[SeriesStorageError,Series[Double]] = nonEmptyRetrieverData(ident).success
  })
  val nonEmptyRetriever = new MockServer(nonEmptyRetrieverInner.service)

  property("nonEmpty retriever yields result") = forAll( (ident:SeriesIdent) => {
    val req = Request(method=Method.GET, uri=Uri(path="/get/" + ident.name))
    val res = nonEmptyRetriever(req).run
    ((res.statusLine == Status.Ok) :| "Status is OK") &&
    ((new String(res.body) == nonEmptyRetrieverInner.renderSeries(ident.name, nonEmptyRetrieverData(ident).data)) :| "Body is correct")
  })
}
