package dev.luin.file.server

import dev.luin.file.server.user.userRepo.UserRepo
import dev.luin.file.server.user.userService.*
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
import io.getquill.PostgresJdbcContext
import io.getquill.SnakeCase

  import javax.sql.DataSource

object RestServer extends App:

  case class Config(host: String, port: Int)

  val config = descriptor[Config]

  val loggingLayer =
    Logging.console(
      logLevel = LogLevel.Info,
      format = LogFormat.ColoredLogFormat()
    ) >>> Logging.withRootLoggerName("file-server")

  val dataSourceLayer: ULayer[Has[DataSource]] = DataSourceLayer.fromPrefix("db").orDie

  val program: ZIO[ZEnv & Has[Config] & Logging & UserService & UserRepo, Throwable, Unit] =
    ZIO.runtime[ZEnv & Has[Config] & Logging & UserService & UserRepo].flatMap { implicit runtime =>
      for
        conf <- getConfig[Config]
        _ <- log.info(s"Starting with $conf")
        _ <- BlazeServerBuilder[RIO[Clock & Blocking & UserService, *]]
          .withExecutionContext(runtime.platform.executor.asEC)
          .bindHttp(conf.port, conf.host)
          .withHttpApp(Router("/" -> (userServerRoutes)).orNotFound)
          .serve
          .compile
          .drain
      yield ()
    }

  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    val configLayer = ZConfig.fromPropertiesFile(
      "/home/user/gb/file-server-scala/src/main/resources/application.conf",
      config
    )
    val userRepo = (dataSourceLayer >>> UserRepo.live)
    program
      .provideLayer(
        ZEnv.live ++ configLayer ++ loggingLayer ++ (loggingLayer ++ userRepo >>> UserService.live) ++ userRepo
      )
      .exitCode
