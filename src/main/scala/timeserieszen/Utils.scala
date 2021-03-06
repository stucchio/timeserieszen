package com.timeserieszen

import scala.reflect.ClassTag
import scalaz._
import Scalaz._

private object Utils {
  def datapointToString(d: DataPoint[Double]): String = {
    val sb = new StringBuilder()
    d.iterator.foreach(kv => {
      sb.append(kv._1.name)
      sb.append(" ")
      sb.append(kv._2)
      sb.append(" ")
    })
    sb.append(d.timestamp)
    sb.append("\n")
    sb.toString()
  }

  def stringToDatapoint(s: String): DataPoint[Double] = {
    val spl = s.split(' ')
    if (spl.length % 2 != 1) {
      throw new IllegalArgumentException("Line was badly formatted: \n" + s)
    }
    val n = (spl.length - 1)/2
    val idents = new Array[SeriesIdent](n)
    val values = new Array[Double](n)
    var i=0
    while (i < n) {
      idents(i) = SeriesIdent(spl(2*i))
      values(i) = spl(2*i+1).toDouble
      i += 1
    }
    FlatDataPoint(spl(spl.length-1).toLong, idents, values)
  }

  def sortSeries[T](x: Array[Long], y: Array[T]): Unit = {
    require(x.length == y.length, "Arrays have two different lengths")
    sortSeries(x,y,0,x.length)
  }

  def sortSeries[T](x: Array[Long], y: Array[T], off: Int, len: Int): Unit = {
    def swap(a: Int, b: Int) {
      val t = x(a)
      x(a) = x(b)
      x(b) = t
      val v = y(a)
      y(a) = y(b)
      y(b) = v
    }
    def vecswap(_a: Int, _b: Int, n: Int) {
      var a = _a
      var b = _b
      var i = 0
      while (i < n) {
        swap(a, b)
        i += 1
        a += 1
        b += 1
      }
    }
    def med3(a: Int, b: Int, c: Int) = {
      if (x(a) < x(b)) {
        if (x(b) < x(c)) b else if (x(a) < x(c)) c else a
      } else {
        if (x(b) > x(c)) b else if (x(a) > x(c)) c else a
      }
    }
    def sort2(off: Int, len: Int) {
      // Insertion sort on smallest arrays
      if (len < 7) {
        var i = off
        while (i < len + off) {
          var j = i
          while (j>off && x(j-1) > x(j)) {
            swap(j, j-1)
            j -= 1
          }
          i += 1
        }
      } else {
        // Choose a partition element, v
        var m = off + (len >> 1)        // Small arrays, middle element
        if (len > 7) {
          var l = off
          var n = off + len - 1
          if (len > 40) {        // Big arrays, pseudomedian of 9
            val s = len / 8
            l = med3(l, l+s, l+2*s)
            m = med3(m-s, m, m+s)
            n = med3(n-2*s, n-s, n)
          }
          m = med3(l, m, n) // Mid-size, med of 3
        }
        val v = x(m)

        // Establish Invariant: v* (<v)* (>v)* v*
        var a = off
        var b = a
        var c = off + len - 1
        var d = c
        var done = false
        while (!done) {
          while (b <= c && x(b) <= v) {
            if (x(b) == v) {
              swap(a, b)
              a += 1
            }
            b += 1
          }
          while (c >= b && x(c) >= v) {
            if (x(c) == v) {
              swap(c, d)
              d -= 1
            }
            c -= 1
          }
          if (b > c) {
            done = true
          } else {
            swap(b, c)
            c -= 1
            b += 1
          }
        }

        // Swap partition elements back to middle
        val n = off + len
        var s = math.min(a-off, b-a)
        vecswap(off, b-s, s)
        s = math.min(d-c, n-d-1)
        vecswap(b,   n-s, s)

        // Recursively sort non-partition-elements
        s = b - a
        if (s > 1)
          sort2(off, s)
        s = d - c
        if (s > 1)
          sort2(n-s, s)
      }
    }
    sort2(off, len)
  }

  def stripDuplicates[T](x: Array[Long], y: Array[T])(implicit m: ClassTag[T]): (Array[Long], Array[T]) = {
    //Precondition: x is assumed sorted.
    require(x.length == y.length, "Arrays have two different lengths")
    if (x.length <= 1) {
      (x,y)
    } else {
      var dupeCount = 0
      var i=1
      var lastTime: Long = x(0)
      while (i < x.size) {
        if (x(i) == lastTime) {
          dupeCount += 1
        }
        lastTime = x(i)
        i += 1
      }
      if (dupeCount == 0) {
        (x,y)
      } else {
        val t = new Array[Long](x.size-dupeCount)
        val v = new Array[T](x.size-dupeCount)

        lastTime = x(0) - 1
        i=0
        var j = 0
        while (i < x.size) {
          if (x(i) != lastTime) {
            t(j) = x(i)
            v(j) = y(i)
            j += 1
          }
          lastTime = x(i)
          i += 1
        }
        (t,v)
      }
    }
  }
}
