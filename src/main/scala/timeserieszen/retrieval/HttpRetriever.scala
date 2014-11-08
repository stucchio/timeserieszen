package com.timeserieszen.retrieval

import com.timeserieszen._
import com.timeserieszen.storage._
import com.timeserieszen.retrieval.AtTime._
import com.timeserieszen.Validator._

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

import org.http4s.server.blaze.BlazeServer
import com.typesafe.scalalogging.slf4j.StrictLogging

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

object Target extends ParamMatcher("target")
object Format extends ParamMatcher("format")
object From extends ParamMatcher("from")
object Until extends ParamMatcher("until")

case class Route(series: Series[Double], from: Epoch, until: Epoch, format: String)

class HttpRetriever(storage: SeriesStorage[Double], hostname: String = "localhost", port: Int = 9999)(implicit ec: ExecutionContext = ExecutionContext.global) extends StrictLogging {

  def run = BlazeServer.newBuilder.mountService(service, "/").withHost(hostname).withPort(port).run()

  def validateRoute(target: String, from: String, until: String, format: String = "json"): ValidationNel[String,Route] = {

    val checkTarget: Validator[Series[Double]] = (t: String) => {
      val entirety: Option[Series[Double]] = storage.read(SeriesIdent(t)) // read the whole file and return the requested interval; todo: smarter retrieval
      if (entirety.isDefined) entirety.get.success
      else s"error: failed to read the file ${target}.dat".failureNel[Series[Double]]
    }

    val checkFrom: Validator[Epoch] = (f: String) =>
      alternative(parseAtTime(f))(s"error: failed to parse 'from=${f}'")

    val checkUntil: Validator[Epoch] = (u: String) =>
      alternative(parseAtTime(u))(s"error: failed to parse 'until=${u}'")

    val checkFormat: Validator[String] = (fmt: String) =>
      alternative((fmt == "json").option("json"))(s"error: format must be json")

    val v = (checkTarget(target) |@| checkFrom(from) |@| checkUntil(until) |@| checkFormat(format))(Route.apply _)

    if (v.isSuccess) {
        val (f,u) = (v.toOption.get.from, v.toOption.get.until)
        if (f.getClass == u.getClass) // demand that from and until are in the same units; todo: global Epoch units config setting
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

  def renderJson(v: ValidationNel[String,Route]): String = v match {
      case Success(r) => {
        // problem: graphite json output has heterogenous lists [Double, Long], which scala doesn't allow (shapeless lib?). this output is wrong. todo: fix this.
        val datapoints = r.series.data
                          .filter({p:(Long,Double) => r.from.n <= p._1 && p._1 <= r.until.n})
                          .map(p => List(p._2.toString, p._1.toString)) // flip the coordinates as per the graphite output format
        compact(render(List(("target" -> r.series.ident.name) ~ ("datapoints" -> datapoints))))
      }
      case Failure(xs) => compact(render(xs.toList))
    }

  lazy val service = HttpService {
    case req @ GET -> Root / "get" / tsIdent => {
      val result = storage.read(SeriesIdent(tsIdent)) // catch the EntityTooLarge error here
      Ok("foo")
    }

    case GET -> Root / "stream" / n => Ok(Process.range(0, n.toInt).map(i => s"This is string number $i\n"))
    case GET -> Root / "render" :? Target(target) +& From(from) +& Until(until) +& Format(format) =>
      Ok(renderJson(validateRoute(target,from,until,format)))
    case req => NotFound()
  }
}
