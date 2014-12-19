package com.timeserieszen.storage

import com.timeserieszen._
import com.timeserieszen.wal_handlers.WALHandler
import com.timeserieszen.monitoring._

import scalaz._
import Scalaz._
import scalaz.stream._
import scalaz.concurrent._
import java.io._

sealed trait SeriesStorageError
case object SeriesMissing extends SeriesStorageError

trait SeriesStorage[T] {
  def write(series: Series[T]): Unit
  def append(series: Series[T]): Unit
  def read(ident: SeriesIdent): Validation[SeriesStorageError,Series[T]]

  private def deleteOrWrite(x: WALHandler.FileRemover \/ Series[T]): Task[Unit] = Task { x.fold( rm => rm(), write _ ) }

  def sink: Sink[Task,(WALHandler.FileRemover \/ Series[T])] = Process.emit( deleteOrWrite _ ).repeat

}

private abstract class SeriesStorageFromAtomic(dataDir: File, stagingDir: File) extends SeriesStorage[Double] with AtomicStorageHandler with Logging {
  log.info("Created {} with datadir {} and stagingdir {}", this.getClass, dataDir, stagingDir)

  private def identToFilename(si: SeriesIdent): java.io.File = new File(dataDir, si.name + ".dat")

  def write(series: Series[Double]): Unit = writeToFile(identToFilename(series.ident), stagingDir, series.times, series.values)
  def append(series: Series[Double]): Unit = writeToFile(identToFilename(series.ident), stagingDir, series.times, series.values)
  def read(ident: SeriesIdent): Validation[SeriesStorageError, Series[Double]] = {
    val f = identToFilename(ident)
    if (f.exists()) {
      val data = readFile(f)
      BufferedSeries(ident, data._1, data._2).success
    } else {
      SeriesMissing.fail[Series[Double]]
    }
  }
}
