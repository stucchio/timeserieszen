
package com.timeserieszen.retrieval

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import scalaz._
import Scalaz._

object AtTime {

  def Try[A](a: => A): Option[A] =
    try Some(a)
    catch { case e: Exception => None }

  sealed trait Epoch
  case class Sec(n: Long) extends Epoch
  case class Deci(n: Long) extends Epoch
  case class Centi(n: Long) extends Epoch
  case class Milli(n: Long) extends Epoch
  case class Micro(n: Long) extends Epoch
  case class Nano(n: Long) extends Epoch

  val suffixToUnit = Map(
    ""    -> { Sec(_: Long) },
    "s"   -> { Sec(_: Long) },
    "ds"  -> { Deci(_: Long) },
    "cs"  -> { Centi(_: Long) },
    "ms"  -> { Milli(_: Long) },
    "us"  -> { Micro(_: Long) },
    "μs"  -> { Micro(_: Long) },
    "mus" -> { Micro(_: Long) },
    "ns"  -> { Nano(_: Long) }
  )

  def ordersOfMagnitude(x: Epoch): Int = {
    x match {
      case Sec(n) => 0
      case Deci(n) => -1
      case Centi(n) => -2
      case Milli(n) => -3
      case Micro(n) => -6
      case Nano(n) => -9
    }
  }

  def ordersOfMagnitudeToScale(x: Epoch, y: Epoch): Int = ordersOfMagnitude(x) - ordersOfMagnitude(y)
  // invariant: (x.n)*10^ordersOfMagnitude(x,y) == y.n

  // this is a String implemented version of (n:Long)*Math.pow(10,o).toLong, since for o negative didn't want to deal with Double or BigDecimal.
  def convertByOrdersOfMagnitude(n: Long, o: Int): Long = {
    val s = n.toString
    val m = s.length + o

    o match {
      case o if (o == 0) => n
      case o if (o > 0) => (s ++ "0"*o).toLong
      case o if (o < 0) => if (m <= 0) 0 else s.take(m).toLong
    }
  }
  // scala> (-5 to 5) map { convertByOrdersOfMagnitude(1234, _:Int) }
  // res8: scala.collection.immutable.IndexedSeq[Long] = Vector(0, 0, 1, 12, 123, 1234, 12340, 123400, 1234000, 12340000, 123400000)

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
  // as a sanity check, there are 30 implicit defs above, and the number of pairs of (x,y) of a n element set S with x != y is n*(n-1). in our case there are 6 elements.
  // scala> ((_:Nano).n + 1)(Sec(1))
  // res10: Long = 1000000001

  def stringToEpoch(s: String): Option[Epoch] = {
    val p = s span {(_:Char).isDigit}
    val digits = p._1
    val suffix = p._2
    val n = Try(digits.toLong)
    val f = suffixToUnit.get(suffix)
    n <*> f
  }

  def stringToDateTime(s: String): Option[DateTime] = {
    // todo: add more parsers. this one is the default ISO8601 format i.e. "20141027T122555.001Z"
    Try(ISODateTimeFormat.basicDateTime().parseDateTime(s))
  }

  // remark: _.getMillis is the number of ms since the epoch. millis are the smallest unit possible with joda time.
  def dateTimeToEpoch(d: DateTime): Milli = Milli(d.getMillis)

  def parseAtTime(s: String): Option[Epoch] = {
    val parsers = List(
      stringToEpoch _,
      (x:String) => stringToDateTime(x).map(dateTimeToEpoch)
    )

    parsers
      .map(f => f(s)) // apply all parsers to s
      .filter({_.isDefined}) // at most one parser succeeded
      .headOption // Option[Option[Epoch]]
      .flatMap(identity) // equivalently: `.getOrElse(None)`
  }

  val successes = List(
    "129837129837129us",
    "129837129837129μs",
    "129837129837129mus",
    "129837129837129ms",
    "129837129837129ns",
    "129837129837129s",
    "129837129837129",
    "20141027T122555.001Z"
  )
  // scala> successes map parseAtTime
  // res1: List[Option[com.timeserieszen.retrieval.AtTime.Epoch]] = List(Some(Micro(129837129837129)), Some(Micro(129837129837129)), Some(Micro(129837129837129)), Some(Milli(129837129837129)), Some(Nano(129837129837129)), Some(Sec(129837129837129)), Some(Sec(129837129837129)), Some(Milli(1414412755001)))
  // scala> (successes map parseAtTime).filter(_.isDefined).length == successes.length
  // res4: Boolean = true

  val fails = List(
    "abc123",
    "102938102938ff",
    "2014-10-27T19:04:27.000+05:30"
  )
  // scala> fails map parseAtTime
  // res5: List[Option[com.timeserieszen.retrieval.AtTime.Epoch]] = List(None, None, None)
}
