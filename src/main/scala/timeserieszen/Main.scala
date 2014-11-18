package com.timeserieszen

import com.timeserieszen.listener._
import com.timeserieszen.monitoring._
import com.timeserieszen.wal_handlers._
import com.timeserieszen.storage._
import com.timeserieszen.retrieval._

import scalaz.concurrent._

object Main extends Logging {

  def main(args: Array[String]) {
    log.info("Starting timeserieszen server")
    val inputStream = UDPListener(Config.Listener.port, Config.Listener.block_size)
    val walWriter = TextWALHandler(Config.WAL.path, rotateSize=Config.WAL.blockSize)
    Task.fork( inputStream.to(walWriter.writer).run ).runAsync(_ => ())
    log.info("Created wal listener")
    val storageWriter = SequentialBinaryV1Storage(Config.Storage.data_path, Config.Storage.staging_path)
    walWriter.flushedSeries.to(storageWriter.sink).run.run
    log.info("Created storage writer")
    // the above code is blocking? the code below works if the above is commented out.
    // val hostname = Config.Retrieval.hostname
    // val port = Config.Retrieval.port
    // log.info(s"Starting timeserieszen http4s-blaze server on '$hostname:$port'")
    // val httpRetriever = new HttpRetriever(storageWriter, hostname, port).run
  }
}
