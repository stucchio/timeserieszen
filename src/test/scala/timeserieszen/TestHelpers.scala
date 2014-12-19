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

  private def deleteRecursive(f: java.io.File): Unit = {
    if (f.isDirectory()) {
      f.listFiles().foreach( deleteRecursive _)
      f.delete()
    } else {
      f.delete()
    }
  }

  def withTempDir[T](f:java.io.File => T) = {
    val file = java.nio.file.Files.createTempDirectory("tszen_test_").toFile()
    try {
      f(file)
    } finally {
      deleteRecursive(file)
    }
  }

  implicit val arbitrarySeriesIdent: Arbitrary[SeriesIdent] = Arbitrary( Gen.alphaStr.map(x => "a" + x.replace(" ", "")).map(SeriesIdent))

  implicit val ArbitraryDataPoint = Arbitrary(for {
    ts <- arbitrary[Long]
    numVals <- Gen.chooseNum(1,10)
    idents <- Gen.containerOfN[List,SeriesIdent](numVals, arbitrary[SeriesIdent])
    identsCleaned = idents.toSet.toList
    values <- identsCleaned.map( _ => arbitrary[Double]).sequence
  } yield DataPoint[Double](ts, identsCleaned, values))

  val genArrayForSeries: Gen[(Array[Long], Array[Double])] = for {
    numVals <- Gen.choose(5,1024)
    times = (1 to numVals).map(_.toLong).toArray
    _ = scala.util.Sorting.quickSort(times)
    values <- Gen.containerOfN[Array,Double](numVals, arbitrary[Double])
  } yield (times, values)

  class WaitFor[T] {
    private val semaphore = new java.util.concurrent.Semaphore(1)
    semaphore.acquire()
    private var o: Option[T] = None
    def put(x: T) = {
      o = Some(x)
      semaphore.release()
    }
    def apply(): T = {
      //Block until result is returned.
      semaphore.acquire()
      try {
        o.get
      } finally {
        semaphore.release()
      }
    }
  }
}
