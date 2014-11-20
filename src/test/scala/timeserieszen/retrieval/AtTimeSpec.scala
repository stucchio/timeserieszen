package com.timeserieszen.retrieval

import org.scalacheck._
import scalaz._
import Scalaz._
import Arbitrary.arbitrary
import Prop._
import com.timeserieszen.retrieval.AtTime
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat,ISODateTimeFormat, DateTimeFormatter}

object AtTimeSpec extends Properties("AtTime") {
  val prop_stringToEpoch = {
    val suffixes = AtTime.suffixToUnit.keys.toList

    val epochStringGen = for {
      n <- arbitrary[Long] suchThat (_ > 0)
      suf <- Gen.oneOf(suffixes)
    } yield n.toString + suf

    forAll(epochStringGen)(s => AtTime.stringToEpoch(s).isDefined)
  }

  val prop_stringToDateTime = {
    // warning: joda assumes n is ms since the epoch. thus if you pass in `date +%s` you'll get a date in 1970 instead of a date in 2014.
    def dtString(p: DateTimeFormatter, n:Long) = p.print(new DateTime(n)) // TODO: take into account the units of the epoch time

    val dateTimeStringGen = for {
      p <- Gen.oneOf(AtTime.jodaPatterns)
      n <- Gen.choose(0L, 2000000000000L) // between 1970-01-01 and 2033-05-18, in milliseconds
    } yield dtString(p,n)

    // there should be one and and only one Some in the List[Option[DateTime]] returned by stringToDateTime
    forAll(dateTimeStringGen)(s => AtTime.stringToDateTime(s).isDefined)
  }

  property("stringToDateTime: exists unique") = prop_stringToDateTime
  property("parseAtTime: true positive") = prop_stringToEpoch && prop_stringToDateTime

  /*
  property("implicit conversions amongst [Sec,Deci,Centi,Milli,Micro,Nano] works")

  let's just give a proof of correctness since there are only 30 cases to inspect.

  here's the proof that the conversion between Sec and Nano works:

  scala> val log10 = (x:Double) => scala.math.log10(x).toInt
  scala> log10((Sec(1):Nano).n)
  res7: Int = 9

  so we just repeat that check for all of the other implicit conversions. the first group of fifteen conversions goes from a bigger unit to a smaller one. the second group of 15 conversion goes from smaller to bigger.

  log10((Sec(1):Deci).n) == 1
  log10((Sec(1):Centi).n) == 2
  log10((Sec(1):Milli).n) == 3
  log10((Sec(1):Micro).n) == 6
  log10((Sec(1):Nano).n) == 9
  log10((Deci(1):Centi).n) == 1
  log10((Deci(1):Milli).n) == 2
  log10((Deci(1):Micro).n) == 5
  log10((Deci(1):Nano).n) == 8
  log10((Centi(1):Milli).n) == 1
  log10((Centi(1):Micro).n) == 4
  log10((Centi(1):Nano).n) == 7
  log10((Milli(1):Micro).n) == 3
  log10((Milli(1):Nano).n) == 6
  log10((Micro(1):Nano).n) == 3

  List(
      (Deci(1):Sec).n
    , (Centi(1):Sec).n
    , (Centi(1):Deci).n
    , (Milli(1):Sec).n
    , (Milli(1):Deci).n
    , (Milli(1):Centi).n
    , (Micro(1):Sec).n
    , (Micro(1):Deci).n
    , (Micro(1):Centi).n
    , (Micro(1):Milli).n
    , (Nano(1):Sec).n
    , (Nano(1):Deci).n
    , (Nano(1):Centi).n
    , (Nano(1):Milli).n
    , (Nano(1):Micro).n
  )
  res8: List[Long] = List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
  */
}
