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

    log.info("Created storage writer pointing at " + (Config.Storage.data_path, Config.Storage.staging_path))
    val storageWriter = SequentialBinaryV1Storage(Config.Storage.data_path, Config.Storage.staging_path)

    //Spin up listener process for new datapoints
    Task.fork(Task {
      val inputStream = UDPListener(Config.Listener.port, Config.Listener.block_size)
      val walWriter = TextWALHandler(Config.WAL.path, rotateSize=Config.WAL.blockSize)
      Task.fork( inputStream.to(walWriter.writer).run ).runAsync(_ => ())
      log.info("Created wal listener on port " +Config.Listener.port)
      walWriter.flushedSeries.to(storageWriter.sink).run.run
      log.info("Service is listening for new datapoints...")
    }).runAsync(_ => ())

    //Spin up retriever (graphite API) process
    Task.fork(Task {
      log.info(s"Starting timeserieszen http4s-blaze server on '" + Config.Retrieval.hostname + ":" + Config.Retrieval.port + "'")
      val httpRetriever = (new HttpRetriever(storageWriter, Config.Retrieval.hostname, Config.Retrieval.port)).run
    }).runAsync(_ => ())
  }
}
