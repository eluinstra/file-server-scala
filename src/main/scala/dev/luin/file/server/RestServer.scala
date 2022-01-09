package dev.luin.file.server

import dev.luin.file.server.user.userRepo.UserRepo
import dev.luin.file.server.user.userService.*
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import zio.App
import zio.ExitCode
import zio.Has
import zio.IO
import zio.RIO
import zio.UIO
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

object RestServer extends App:

  case class Config(host: String, port: Int)

  val config = descriptor[Config]

  val loggingLayer =
    Logging.console(
      logLevel = LogLevel.Info,
      format = LogFormat.ColoredLogFormat()
    ) >>> Logging.withRootLoggerName("file-server")

  val dbContext = new PostgresJdbcContext(SnakeCase, "db")

  val program: ZIO[ZEnv & Has[Config] & Logging & UserService, Throwable, Unit] =
    ZIO.runtime[ZEnv & Has[Config] & Logging & UserService].flatMap { implicit runtime =>
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
    program
      .provideLayer(
        ZEnv.live ++ configLayer ++ loggingLayer ++ (loggingLayer ++ UserRepo.live >>> UserService.live)
      )
      .exitCode
