
package com.timeserieszen.retrieval

import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat,ISODateTimeFormat, DateTimeFormatter}
import scala.util.control.Exception._
import scalaz._
import Scalaz._

object AtTime {

  sealed trait Epoch { val n: Long }
  case class Sec(n: Long) extends Epoch
  case class Deci(n: Long) extends Epoch
  case class Centi(n: Long) extends Epoch
  case class Milli(n: Long) extends Epoch
  case class Micro(n: Long) extends Epoch
  case class Nano(n: Long) extends Epoch

  val suffixToUnit = Map(
    ""    -> { Milli(_: Long) }, // temporary hack due to the fact that joda stores epoch in millis. TODO: fix the units configuration...
    // ""    -> { Sec(_: Long) },
    "s"   -> { Sec(_: Long) },
    "ds"  -> { Deci(_: Long) },
    "cs"  -> { Centi(_: Long) },
    "ms"  -> { Milli(_: Long) },
    "us"  -> { Micro(_: Long) },
    "μs"  -> { Micro(_: Long) },
    "mus" -> { Micro(_: Long) },
    "ns"  -> { Nano(_: Long) }
  )

  def ordersOfMagnitude(x: Epoch): Int = x match {
    case Sec(n) => 0
    case Deci(n) => -1
    case Centi(n) => -2
    case Milli(n) => -3
    case Micro(n) => -6
    case Nano(n) => -9
  }

  def ordersOfMagnitudeToScale(x: Epoch, y: Epoch): Int = ordersOfMagnitude(x) - ordersOfMagnitude(y)
  // invariant: (x.n)*10^ordersOfMagnitudeToScale(x,y) == y.n
  // TODO: need test for ordersOfMagnitudeToScale

  val e: Array[Long] = {
    def tenToThe(k: Int): Long = List.fill(k)(10.toLong).foldLeft(1.toLong)({(x:Long,y:Long) => x*y})
    (0 to 9).toArray.map(tenToThe)
  }

  def convertByOrdersOfMagnitude(n: Long, o: Int): Long = {
    require(-9 <= o && o <= 9)
    o match {
      case o if (o == 0) => n
      case o if (o > 0) => n*e(o)
      case o if (o < 0) => n/e(-o)
    }
  }

  // begin code to generate the following implicit defs
  /*
  val xs = List("Sec","Deci","Centi","Milli","Micro","Nano")
  val ps = for (i <- xs; j <- xs if i != j) yield (i,j)

  for (p <- ps) {
    val from = p._1
    val to = p._2
    println(s"implicit def ${from}To${to}(x: ${from}): ${to} = ${to}(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(${from}(1),${to}(1))))")
  }
  */
  // end code to generate the following implicit defs
  implicit def SecToDeci(x: Sec): Deci = Deci(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(Sec(1),Deci(1))))
  implicit def SecToCenti(x: Sec): Centi = Centi(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(Sec(1),Centi(1))))
  implicit def SecToMilli(x: Sec): Milli = Milli(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(Sec(1),Milli(1))))
  implicit def SecToMicro(x: Sec): Micro = Micro(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(Sec(1),Micro(1))))
  implicit def SecToNano(x: Sec): Nano = Nano(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(Sec(1),Nano(1))))
  implicit def DeciToSec(x: Deci): Sec = Sec(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(Deci(1),Sec(1))))
  implicit def DeciToCenti(x: Deci): Centi = Centi(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(Deci(1),Centi(1))))
  implicit def DeciToMilli(x: Deci): Milli = Milli(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(Deci(1),Milli(1))))
  implicit def DeciToMicro(x: Deci): Micro = Micro(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(Deci(1),Micro(1))))
  implicit def DeciToNano(x: Deci): Nano = Nano(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(Deci(1),Nano(1))))
  implicit def CentiToSec(x: Centi): Sec = Sec(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(Centi(1),Sec(1))))
  implicit def CentiToDeci(x: Centi): Deci = Deci(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(Centi(1),Deci(1))))
  implicit def CentiToMilli(x: Centi): Milli = Milli(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(Centi(1),Milli(1))))
  implicit def CentiToMicro(x: Centi): Micro = Micro(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(Centi(1),Micro(1))))
  implicit def CentiToNano(x: Centi): Nano = Nano(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(Centi(1),Nano(1))))
  implicit def MilliToSec(x: Milli): Sec = Sec(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(Milli(1),Sec(1))))
  implicit def MilliToDeci(x: Milli): Deci = Deci(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(Milli(1),Deci(1))))
  implicit def MilliToCenti(x: Milli): Centi = Centi(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(Milli(1),Centi(1))))
  implicit def MilliToMicro(x: Milli): Micro = Micro(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(Milli(1),Micro(1))))
  implicit def MilliToNano(x: Milli): Nano = Nano(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(Milli(1),Nano(1))))
  implicit def MicroToSec(x: Micro): Sec = Sec(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(Micro(1),Sec(1))))
  implicit def MicroToDeci(x: Micro): Deci = Deci(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(Micro(1),Deci(1))))
  implicit def MicroToCenti(x: Micro): Centi = Centi(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(Micro(1),Centi(1))))
  implicit def MicroToMilli(x: Micro): Milli = Milli(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(Micro(1),Milli(1))))
  implicit def MicroToNano(x: Micro): Nano = Nano(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(Micro(1),Nano(1))))
  implicit def NanoToSec(x: Nano): Sec = Sec(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(Nano(1),Sec(1))))
  implicit def NanoToDeci(x: Nano): Deci = Deci(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(Nano(1),Deci(1))))
  implicit def NanoToCenti(x: Nano): Centi = Centi(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(Nano(1),Centi(1))))
  implicit def NanoToMilli(x: Nano): Milli = Milli(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(Nano(1),Milli(1))))
  implicit def NanoToMicro(x: Nano): Micro = Micro(convertByOrdersOfMagnitude(x.n, ordersOfMagnitudeToScale(Nano(1),Micro(1))))
  // sanity check: there are 30 implicit defs above, and the number of pairs of (x,y) of a n element set S with x != y is n*(n-1). in our case n = 6.

  def stringToEpoch(s: String): Option[Epoch] = {
    val (digits,suffix) = s span {(_:Char).isDigit}
    val n = catching(classOf[Exception]).opt { digits.toLong }   //TODO could a less general exception be used?
    val f = suffixToUnit.get(suffix)
    n <*> f
  }

  val jodaPatterns = List(  // http://graphite.readthedocs.org/en/latest/render_api.html
    "HH:mm:ss dd.MM.yyyy",  // 23:59:30 31.12.1999 -- 30 seconds to the year 2000
    "HH:mm dd.MM.yyyy",     // 23:59 31.12.1999 -- 1 minute to the year 2000
    "MM/dd/yy hh:mmaa",     // 12/31/99 11:59pm -- 1 minute to the year 2000 in USA notation
    "hhaa MM/dd/yy",        // 12am 01/01/01 -- start of the new millennium
    "yyyyMMdd HH:mm"        // 19970703 12:45 -- 12:45 July 3th, 1997
  ).map( DateTimeFormat.forPattern ) ++
  List(ISODateTimeFormat.basicDateTime()) // default ISO8601 format "20141027T122555.001Z"

  //TODO could a less general exception be used?
  def stringToDateTime(s: String): Option[DateTime] = jodaPatterns.flatMap({ (p:DateTimeFormatter) => catching(classOf[Exception]).opt { p.parseDateTime(s) } }).headOption

  // _.getMillis is the number of ms since the epoch. millis are the smallest unit possible with joda time.
  def dateTimeToEpoch(d: DateTime): Milli = Milli(d.getMillis)

  def parseAtTime(s: String): Option[Epoch] = stringToEpoch(s) <+> (stringToDateTime(s).map(dateTimeToEpoch))
}
