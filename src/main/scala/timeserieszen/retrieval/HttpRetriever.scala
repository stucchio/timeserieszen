package com.timeserieszen.retrieval

import com.timeserieszen._
import com.timeserieszen.storage._
import com.timeserieszen.retrieval.AtTime._
import com.timeserieszen.Validator._
import com.timeserieszen.Utils.Tryz

import scalaz._
import Scalaz._
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

import org.json4s._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write}

import org.http4s.server.blaze.BlazeServer
import com.typesafe.scalalogging.slf4j.StrictLogging

object Target extends ParamMatcher("target")
object Format extends ParamMatcher("format")
object From extends ParamMatcher("from")
object Until extends ParamMatcher("until")

case class Route(series: Series[Double], from: Epoch, until: Epoch, format: String)
case class GraphiteOutput(target: String, datapoints: Array[Array[Any]])

class HttpRetriever(storage: SeriesStorage[Double], hostname: String = "localhost", port: Int = 9999)(implicit ec: ExecutionContext = ExecutionContext.global) extends StrictLogging {

  def run = BlazeServer.newBuilder.mountService(service, "/").withHost(hostname).withPort(port).run()

  def validateRoute(target: String, from: String, until: String, format: String = "json"): ValidationNel[String,Route] = {

    val checkTarget: Validator[Series[Double]] = (t: String) =>
      storage.read(SeriesIdent(t)) <|> s"error: failed to read the file ${t}.dat" // reads the whole file; TODO: smarter retrieval

    val checkFrom: Validator[Epoch] = (f: String) =>
      parseAtTime(f) <|> s"error: failed to parse 'from=${f}'"

    val checkUntil: Validator[Epoch] = (u: String) =>
      parseAtTime(u) <|> s"error: failed to parse 'until=${u}'"

    val checkFormat: Validator[String] = (fmt: String) =>
      (fmt == "json").option("json") <|> s"error: format must be json"

    val v = (checkTarget(target) |@| checkFrom(from) |@| checkUntil(until) |@| checkFormat(format))(Route.apply _)

    if (v.isSuccess) {
        val (f,u) = (v.toOption.get.from, v.toOption.get.until)
        if (f.getClass == u.getClass) // demand that from and until are in the same units; TODO: global Epoch units config setting
          v
        else "error: from and until are not in the same units".failureNel[Route]
    }
    else v
  }

  /* output format. reference: http://graphite.readthedocs.org/en/latest/render_api.html#json
    [{
      "target": "entries",
      "datapoints": [
        [1.0, 1311836008],
        [2.0, 1311836009],
        [3.0, 1311836010],
        [5.0, 1311836011],
        [6.0, 1311836012]
      ]
    }]
  */

  implicit val formats = DefaultFormats // json4s serialization settings for rendering GraphiteOutput
  def render(name: String, data: Seq[(Long,Double)]): String = {
    def f(xs: Seq[(Long,Double)]): Array[Array[Any]] = xs.map(p => Array[Any](p._2,p._1)).toArray // flip p in accordance with the graphite output format
    write(Array(GraphiteOutput(name, f(data))))
  }

  def renderJson(v: ValidationNel[String,Route]): String = v match {
      case Success(r) => {
        val datapoints = r.series.data.filter({p:(Long,Double) => r.from.n <= p._1 && p._1 <= r.until.n})
        render(r.series.ident.name, datapoints)
      }
      case Failure(xs) => xs.toList.mkString("\n")
    }

  lazy val service: HttpService = { // http4s 0.3.0
  // lazy val service = HttpService { // http4s 0.4.0-snapshot

    case req @ GET -> Root / "get" / tsIdent => {
      Tryz(storage.read(SeriesIdent(tsIdent))) match {
        case Success(s) => if (s.isDefined) {
                              val series = s.get
                              Ok(render(tsIdent, series.data))
                           } else NotFound(s"error: failed to read the file ${tsIdent}.dat")
        case Failure(xs) => NotFound(xs.head.getMessage)
      }
    }

    case GET -> Root / "render" :? Target(target) +& From(from) +& Until(until) +& Format(format) =>
      Ok(renderJson(validateRoute(target,from,until,format)))

    // format=json is the default, so may omit it
    case GET -> Root / "render" :? Target(target) +& From(from) +& Until(until) =>
      Ok(renderJson(validateRoute(target,from,until)))

    case req => NotFound()
  }
}
