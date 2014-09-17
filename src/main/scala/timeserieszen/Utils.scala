package com.timeserieszen

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
}
