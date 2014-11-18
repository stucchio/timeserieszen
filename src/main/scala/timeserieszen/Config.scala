package com.timeserieszen

import com.timeserieszen.monitoring.Logging

import com.typesafe.config._

class Subconfig(config: Config, name: String) {
  protected lazy val cfg = config.getConfig(name)
}

object Config extends Logging {
  private lazy val config = ConfigFactory.load()

  lazy val ensure_directories_exist = config.getBoolean("ensure_directories_exist")

  object Listener extends Subconfig(config, "listener") {
    lazy val port = cfg.getInt("port")
    lazy val block_size = cfg.getInt("block_size")
  }

  object WAL extends Subconfig(config, "wal") {
    lazy val path = cfg.getDirectory("path")
    lazy val blockSize = cfg.getInt("wal_block_size")
  }

  object Storage extends Subconfig(config, "storage") {
    lazy val data_path = cfg.getDirectory("data_path")
    lazy val staging_path = cfg.getDirectory("staging_path")
  }

  object Retrieval extends Subconfig(config, "retrieval") {
    lazy val hostname = cfg.getString("hostname")
    lazy val port = cfg.getInt("port")
  }

  object Monitoring extends Subconfig(config, "monitoring") {
    lazy val metrics_prefix = cfg.getString("metrics_prefix")
  }

  private implicit class CfgWrapper(val cfg: Config) extends AnyVal {
    def getDirectory(nm: String) = ensureDirExists(cfg.getString(nm))
  }

  private def ensureDirExists(dir: String): java.io.File = ensureDirExists(new java.io.File(dir))

  private def ensureDirExists(dir: java.io.File): java.io.File = {
    (ensure_directories_exist, dir.exists(), dir.isDirectory()) match {
      case (false, true, true) => dir
      case (false, _, _)       => throw new java.io.IOException("Directory " + dir + " does not exist. Check your configuration.")
      case (true, true, false) => throw new java.io.IOException("Directory " + dir + " exists but is not a directory!")
      case (true, true, true)  => dir
      case (true, false, false) => {
        if (dir.mkdirs()) {
          log.info("Directory {} does not exist, creating", dir)
          dir
        } else {
          throw new java.io.IOException("Unable to create directory " + dir)
        }
      }
      case (true, _, _) => throw new java.io.IOException("Directory " + dir + " is in a wacky state.")
    }
  }
}
