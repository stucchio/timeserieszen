package com.timeserieszen

import com.timeserieszen.listener._
import com.timeserieszen.wal_handlers._

object Main {
  def main(args: Array[String]) {
    val inputStream = UDPListener(Config.Listener.port, Config.Listener.block_size)
    val walWriter = TextWALHandler(new java.io.File(Config.WAL.path), rotateSize=Config.WAL.blockSize)
    inputStream.to(walWriter.writer).run.run
  }
}
