package com.timeserieszen

import org.scalacheck._
import scalaz._
import Scalaz._
import scalaz.stream._
import scalacheck.ScalazProperties._
import Arbitrary.arbitrary

object TestHelpers {
  implicit object ArbitraryApplicative extends Applicative[Gen] {
    def point[A](a: =>A) = Gen.oneOf(Seq(a))
    def ap[A,B](ga: =>Gen[A])(gf: =>Gen[A=>B]) = for {
      a <- ga
      f <- gf
    } yield (f(a))
  }
}
