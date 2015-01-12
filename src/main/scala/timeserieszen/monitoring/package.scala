package com.timeserieszen.monitoring

import com.timeserieszen.Config

import org.slf4j.{Logger, LoggerFactory}
import com.codahale.metrics._
import java.util.concurrent.TimeUnit
import scalaz._
import Scalaz._
import scalaz.stream._

trait Logging {
  protected lazy val log = LoggerFactory.getLogger(this.getClass)
}

object Metrics {
  protected lazy val registry = new MetricRegistry {
    override def counter(name: String) = super.counter(Config.Monitoring.metrics_prefix + "." + name)
    override def histogram(name: String) = super.histogram(Config.Monitoring.metrics_prefix + "." + name)
    override def meter(name: String) = super.meter(Config.Monitoring.metrics_prefix + "." + name)
    override def timer(name: String) = super.timer(Config.Monitoring.metrics_prefix + "." + name)
  }

  /* FOR DEBUG ONLY
  val reporter = ConsoleReporter.forRegistry(registry).convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS).build();
  reporter.start(1, TimeUnit.SECONDS);
   */
}

trait Metrics {
  protected def metricPrefix: String

  protected def counter(name: String) = Metrics.registry.counter(metricPrefix + "." + name)
  protected def histogram(name: String) = Metrics.registry.histogram(metricPrefix + "." + name)
  protected def meter(name: String) = Metrics.registry.meter(metricPrefix + "." + name)
  protected def timer(name: String) = Metrics.registry.timer(metricPrefix + "." + name)

  implicit class StreamMetric[T, M[_]](stream: Process[M,T]) {
    def count(label: String): Process[M,T] = {
      val c = counter(label)
      stream.map( (t:T) => {
        c.inc()
        t
      })
    }

    def hist(label: String, value: T => Long): Process[M,T] = {
      val h = histogram(label)
      stream.map( (t:T) => {
        h.update(value(t))
        t
      })
    }
  }
}
