package com.timeserieszen

import org.scalacheck._
import scalaz._
import Scalaz._
import scalaz.stream._
import scalacheck.ScalazProperties._
import Arbitrary.arbitrary
import Prop._
import TestHelpers._
import java.util.Arrays

object UtilsSpec extends Properties("Utils") {

  property("sorting") = forAllNoShrink(arbitrary[(Array[Long], Array[Double])])((m: (Array[Long], Array[Double])) => {
    val n = math.min(m._1.distinct.length, m._2.distinct.length)
    val (t,v) = ( m._1.distinct.slice(0,n), m._2.distinct.slice(0,n) )
    val (oldT, oldV) = (Arrays.copyOf(t, n), Arrays.copyOf(v, n))
    Utils.sortSeries(t,v)
    oldT.zip(oldV).toMap == t.zip(v).toMap
  })

}
