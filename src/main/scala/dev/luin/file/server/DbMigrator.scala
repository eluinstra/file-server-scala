package dev.luin.file.server

import org.flywaydb.core.Flyway
import zio.*
import zio.config.*
import zio.console.*

object dbMigrator:

  private val basePath = "classpath:/db/migration/"
  private val locations = Map(
    ("jdbc:db2:" -> (basePath + "db2")),
    ("jdbc:h2:" -> (basePath + "h2")),
    ("jdbc:hsqldb:" -> (basePath + "hsqldb")),
    ("jdbc:mariadb:" -> (basePath + "mysql")),
    ("jdbc:sqlserver:" -> (basePath + "mssql")),
    ("jdbc:mysql:" -> (basePath + "mysql")),
    ("jdbc:oracle:" -> (basePath + "oracle")),
    ("jdbc:postgresql:" -> (basePath + "postgresql"))
  )

  private def findLocation(url: String) = locations.find((key, _) => url.startsWith(key)).map((_, value) => value)

  trait DbMigrator:
    def migrate(url: String, username: String, password: String): Task[Unit]

  object DbMigrator:
    val defaultLayer: ZLayer[Any, Nothing, Has[DbMigrator]] =
      ZLayer.succeed {
        new DbMigrator {
          def migrate(url: String, username: String, password: String): Task[Unit] =
            ZIO.effect {
              findLocation(url).foreach { l =>
                val config = Flyway.configure()
                  .dataSource(url, username, password)
                  .locations(l)
                config.load().migrate()
              }
            }
        }
      }
    
    def migrate(url: String, username: String, password: String): ZIO[Has[DbMigrator], Throwable, Unit] =
      ZIO.accessM(_.get.migrate(url, username, password))