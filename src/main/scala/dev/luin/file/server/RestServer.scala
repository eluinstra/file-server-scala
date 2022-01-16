package dev.luin.file.server

import cats.syntax.all.*
import dev.luin.file.server.config.*
import dev.luin.file.server.dbMigrator.DbMigrator
import dev.luin.file.server.user.userRepo.UserRepo
import dev.luin.file.server.user.userService.*
import io.getquill.PostgresJdbcContext
import io.getquill.SnakeCase
import io.getquill.*
import io.getquill.context.ZioJdbc.DataSourceLayer
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import zio.App
import zio.ExitCode
import zio.Has
import zio.IO
import zio.RIO
import zio.UIO
import zio.ULayer
import zio.URIO
import zio.ZEnv
import zio.ZIO
import zio.blocking.Blocking
import zio.clock.Clock
import zio.config.*
import zio.config.magnolia.descriptor
import zio.console.*
import zio.interop.catz.*
import zio.logging.*

import javax.sql.DataSource

object RestServer extends App:

  lazy val config = descriptor[ApplicationConfig]

  lazy val loggingLayer =
    Logging.console(
      logLevel = LogLevel.Info,
      format = LogFormat.ColoredLogFormat()
    ) >>> Logging.withRootLoggerName("file-server")

  lazy val dataSourceLayer = DataSourceLayer.fromPrefix("db").orDie

  val program: ZIO[ZEnv & Has[ApplicationConfig] & Has[DbMigrator] & Logging & Has[UserService], Throwable, Unit] =
    ZIO.runtime[ZEnv & Has[ApplicationConfig] & Has[DbMigrator] & Logging & Has[UserService]].flatMap { implicit runtime =>
      for
        conf <- getConfig[ApplicationConfig]
        _ <- DbMigrator.migrate(conf.db.dataSource.url.stripPrefix("\"").stripSuffix("\""), conf.db.dataSource.user, conf.db.dataSource.password)
        _ <- log.info(s"Starting with $conf")
        _ <- BlazeServerBuilder[RIO[Clock & Blocking & Has[UserService], *]]
          .withExecutionContext(runtime.platform.executor.asEC)
          .bindHttp(conf.server.port, conf.server.host)
          .withHttpApp(Router("/" -> (userServerRoutes <+> userSwaggerRoutes)).orNotFound)
          .serve
          .compile
          .drain
      yield ()
    }

  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    lazy val configLayer = ZConfig.fromPropertiesFile(
      "/home/user/gb/file-server-scala/src/main/resources/application.conf",
      config,
      Some('.'),
      Some(',')
    )
    lazy val userRepoLayer = (dataSourceLayer >>> UserRepo.defaultLayer)
    program
      .provideLayer(
        ZEnv.live ++ configLayer ++ DbMigrator.defaultLayer ++ loggingLayer ++ (loggingLayer ++ userRepoLayer >>> UserService.defaultLayer)
      )
      .exitCode
