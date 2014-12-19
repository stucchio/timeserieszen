package com.timeserieszen.retrieval

import org.scalacheck._
import scalaz._
import Scalaz._
import Arbitrary.arbitrary
import Prop._
import com.timeserieszen.storage.SeriesStorage
import com.timeserieszen.{Series, SeriesIdent, TestHelpers}
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat,ISODateTimeFormat, DateTimeFormatter}
import org.http4s.server.MockServer._
import org.http4s._
import org.http4s.server._

// Probably look: https://github.com/http4s/http4s/blob/master/server/src/test/scala/org/http4s/server/MockServerSpec.scala
// To figure out how to test the webserver.
object HttpRetrieverSpec extends Properties("HttpRetriever") {
  trait MockSeriesStorageReadOnly[T] extends SeriesStorage[T] {
    def write(series: Series[T]) = ???
    def append(series: Series[T]) = ???
  }

  val emptyRetriever = new MockServer(new HttpRetriever(new MockSeriesStorageReadOnly[Double] {
    def read(ident: SeriesIdent): Option[Series[Double]] = None
  }).service)

  property("empty retriever yields 404s") = forAllNoShrink(TestHelpers.arbitrarySeriesIdent)( ident => {
    val req = Request(method=Method.GET, uri=Uri(path="/get/" + ident))
    emptyRetriever(req).run.statusLine == Status.NotFound
  })

  property("empty retriever yields 404s pt 2") = forAllNoShrink(TestHelpers.arbitrarySeriesIdent)( ident => {
    val req = Request(method=Method.GET, uri=Uri(path="/render?target=" + ident + "&from=07to=1"))
    emptyRetriever(req).run.statusLine == Status.NotFound
  })
}
