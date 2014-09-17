package com.timeserieszen
import com.typesafe.config._

class Subconfig(config: Config, name: String) {
  protected lazy val cfg = config.getConfig(name)
}

object Config {
  private lazy val config = ConfigFactory.load()

  object Listener extends Subconfig(config, "listener") {
    lazy val port = cfg.getInt("port")
    lazy val block_size = cfg.getInt("block_size")
  }

  object WAL extends Subconfig(config, "wal") {
    lazy val path = cfg.getString("path")
    lazy val blockSize = cfg.getInt("wal_block_size")
  }
}
