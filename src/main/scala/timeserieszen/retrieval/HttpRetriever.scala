package com.timeserieszen.retrieval

import com.timeserieszen.storage._
import com.timeserieszen._

import scala.concurrent.{ExecutionContext, Future}
import scalaz.concurrent.Task
import scalaz.stream.Process
import scalaz.stream.Process._

import org.http4s.Header.`Content-Type`
import org.http4s._
import org.http4s.dsl._
import org.http4s.server._
import org.http4s.server.middleware.EntityLimiter
import org.http4s.server.middleware.EntityLimiter.EntityTooLarge
import org.http4s.server.middleware.PushSupport._
import scodec.bits.ByteVector

import org.http4s.server.blaze.BlazeServer

class HttpRetriever(storage: SeriesStorage[Double], hostname: String = "localhost", port: Int = 9999)(implicit ec: ExecutionContext = ExecutionContext.global) {

  def run = BlazeServer.newBuilder.mountService(service, "/").withHost(hostname).withPort(port).run()

  lazy val service = HttpService {
    case req @ GET -> Root / "get" / tsIdent => {
      val result = storage.read(SeriesIdent(tsIdent))

      Ok("foo")
    }

    case req => NotFound()
  }
}
