package com.timeserieszen

import scalaz._
import Scalaz._
import com.nicta.rng._
import com.timeserieszen.retrieval.AtTime._

object RandomSeries {
  // randSeries is simple minded, with values uniformly distributed within the range [ub,lb], and time steps of 1.
  // gives us some examples to play with.
  def randSeries(name: String, start: Epoch, end: Epoch, lb: Double, ub: Double): Series[Double] = {
    require(start.getClass == end.getClass) // prevent implicit conversions
    require(end.n >= start.n)               // so this line makes sense
    val indices = (start.n |-> end.n) // `|->` produces a List

    // com.nicta.rng is a purely functional random number generator, using a state monad internally to keep the rng seed.
    // calling x.run gives a new seed for the next call of x.run, thus the for loop below.
    val x = Rng.choosedouble(lb,ub)

    val values = (for (i <- indices) yield x.run)
                   .sequenceU // sequenceU :: List[IO[Double]] -> IO[List[Double]]
                   .unsafePerformIO // extracts the inner List[Double], akin to ghc evaluating main :: IO ()

    BufferedSeries(SeriesIdent(name), indices.toArray, values.toArray)
  }
}
