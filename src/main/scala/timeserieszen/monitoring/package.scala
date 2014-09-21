package com.timeserieszen.monitoring

import com.timeserieszen.Config

import org.slf4j.{Logger, LoggerFactory}
import com.codahale.metrics._
import java.util.concurrent.TimeUnit

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
}

trait Metrics {
  protected def prefix: String

  protected def counter(name: String) = Metrics.registry.counter(Config.Monitoring.metrics_prefix + "." + prefix + "." + name)
  protected def histogram(name: String) = Metrics.registry.histogram(Config.Monitoring.metrics_prefix + "." + prefix + "." + name)
  protected def meter(name: String) = Metrics.registry.meter(Config.Monitoring.metrics_prefix + "." + prefix + "." + name)
  protected def timer(name: String) = Metrics.registry.timer(Config.Monitoring.metrics_prefix + "." + prefix + "." + name)
}
