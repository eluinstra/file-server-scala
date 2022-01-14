package dev.luin.file.server

import dev.luin.file.server.user.FsUser
import io.getquill.*
import io.getquill.context.ZioJdbc.DataSourceLayer
import io.getquill.context.ZioJdbc

import zio.*
import zio.interop.catz.*
import zio.logging.*

import javax.sql.DataSource

object dataService:

  type DataService = Has[DataService.Service]

  val jdbcContext = new PostgresZioJdbcContext(SnakeCase)
  import jdbcContext.*

  object DataService:
    trait Service:
      def findById(id: Long): IO[jdbcContext.Error, FsUser]

    val live: ZLayer[Has[DataSource], Nothing, DataService] =
      ZLayer.fromService[DataSource, DataService.Service] { dataSource =>
        new Service {
          inline def findById(id: Long): IO[jdbcContext.Error, FsUser] =
            run {
              quote {
                query[FsUser].filter(u => u.id == lift(id))
              }
            }.provide(Has(dataSource)).map(_.head)
        }
      }

    def findById(id: Int): ZIO[DataService, jdbcContext.Error, FsUser] = ZIO.accessM(_.get.findById(id))

object Database extends App:

  import dev.luin.file.server.dataService.DataService

  val loggingLayer =
    Logging.console(
      logLevel = LogLevel.Info,
      format = LogFormat.ColoredLogFormat()
    ) >>> Logging.withRootLoggerName("database")

  val dataSourceLayer: ULayer[Has[DataSource]] = DataSourceLayer.fromPrefix("db").orDie

  val program: ZIO[ZEnv & Logging & DataService, Throwable, Unit] =
    ZIO.runtime[ZEnv & Logging & DataService].flatMap { implicit runtime =>
      for
        _ <- log.info(s"Find user 1")
        user <- DataService.findById(1)
        _ <- log.info(s"Found user $user")
      yield ()
    }

  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    program
      .provideLayer(ZEnv.live ++ loggingLayer ++ (dataSourceLayer >>> DataService.live))
      .exitCode
