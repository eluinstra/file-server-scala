package dev.luin.file.server

import dev.luin.file.server.user.userService.*
import org.http4s.server.Router
import org.http4s.blaze.server.BlazeServerBuilder
import zio.{App, ExitCode, Has, IO, RIO, UIO, URIO, ZEnv, ZIO}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.config.*
import zio.config.magnolia.descriptor
import zio.console.*
import zio.interop.catz.*
import zio.logging.*
import dev.luin.file.server.user.userRepo.UserRepo

object RestServer extends App:

  case class Config(host: String, port: Int)

  val configuration = descriptor[Config]

  val loggingLayer =
    Logging.console(
      logLevel = LogLevel.Info,
      format = LogFormat.ColoredLogFormat()
    ) >>> Logging.withRootLoggerName("file-server")

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
      configuration
    )
    program
      .provideLayer(
        ZEnv.live ++ configLayer ++ loggingLayer ++ (loggingLayer ++ UserRepo.live >>> UserService.live)
      )
      .exitCode
