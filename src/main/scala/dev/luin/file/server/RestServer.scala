package dev.luin.file.server

import dev.luin.file.server.user.UserService.*
import zhttp.service.Server
import zio.{App, ExitCode, Has, IO, RIO, UIO, URIO, ZEnv, ZIO}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.config.*
import zio.config.magnolia.descriptor
import zio.console.*
import zio.interop.catz.*
import zio.logging.*

object RestServer extends App:

  case class Config(host: String, port: Int)

  val configuration = descriptor[Config]

  val loggingLayer =
    Logging.console(
      logLevel = LogLevel.Info,
      format = LogFormat.ColoredLogFormat()
    ) >>> Logging.withRootLoggerName("file-server")

  val program: ZIO[ZEnv & Has[Config] & Logging, Throwable, Unit] =
    ZIO.runtime[ZEnv].flatMap { implicit runtime =>
      for
        conf <- getConfig[Config]
        _ <- log.info(s"Starting with $conf")
        out <- Server.start(conf.port, userServerRoutes <> userSwaggerRoutes)
      yield out
    }

  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    val configLayer = ZConfig.fromPropertiesFile(
      "/home/user/gb/file-server-scala/src/main/resources/application.conf",
      configuration
    )
    program.provideLayer(ZEnv.live ++ configLayer ++ loggingLayer).exitCode
