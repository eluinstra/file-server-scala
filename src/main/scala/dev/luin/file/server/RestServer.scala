package dev.luin.file.server

import org.http4s.server.Router
import org.http4s.blaze.server.BlazeServerBuilder
import zio.{App, ExitCode, Has, IO, RIO, UIO, URIO, ZEnv, ZIO}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.config.*
import zio.config.magnolia.descriptor
import zio.console.*
import zio.interop.catz.*
import dev.luin.file.server.user.UserService.*

object RestServer extends App:

  case class Config(host: String, port: Int)

  val configuration = descriptor[Config]

  val program: ZIO[ZEnv & Has[Config], Throwable, Unit] =
    ZIO.runtime[ZEnv].flatMap { implicit runtime =>
      for
        conf <- getConfig[Config]
        out <- BlazeServerBuilder[RIO[Clock & Blocking, *]]
          .withExecutionContext(runtime.platform.executor.asEC)
          .bindHttp(conf.port, conf.host)
          .withHttpApp(Router("/" -> (userServerRoutes)).orNotFound)
          .serve
          .compile
          .drain
      yield out
    }

  override def run(args: List[String]): URIO[ZEnv, ExitCode] = //serve.exitCode
    val configLayer = ZConfig.fromPropertiesFile(
      "/home/user/gb/file-server-scala/src/main/resources/application.conf",
      configuration
    )
    program.provideLayer(ZEnv.live ++ configLayer).exitCode
