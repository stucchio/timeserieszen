package com.timeserieszen
import com.typesafe.config._

class Subconfig(config: Config, name: String) {
  protected lazy val cfg = config.getConfig(name)
}

object Config {
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

  implicit class CfgWrapper(val cfg: Config) extends AnyVal {
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
          dir
        } else {
          throw new java.io.IOException("Unable to create directory " + dir)
        }
      }
      case (true, _, _) => throw new java.io.IOException("Directory " + dir + " is in a wacky state.")
    }
  }
}
