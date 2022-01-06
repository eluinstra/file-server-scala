package dev.luin.file.server

import org.http4s.server.Router
import org.http4s.blaze.server.BlazeServerBuilder
import zio.clock.Clock
import zio.blocking.Blocking
import zio.interop.catz.*
import zio.{App, ExitCode, IO, RIO, UIO, URIO, ZEnv, ZIO}
import dev.luin.file.server.user.UserService.*


object RestServer extends App:
  
  val serve: ZIO[ZEnv, Throwable, Unit] =
    ZIO.runtime[ZEnv].flatMap { implicit runtime =>
      BlazeServerBuilder[RIO[Clock & Blocking, *]]
        .withExecutionContext(runtime.platform.executor.asEC)
        .bindHttp(8080, "localhost")
        .withHttpApp(Router("/" -> (userServerRoutes)).orNotFound)
        .serve
        .compile
        .drain
    }

  override def run(args: List[String]): URIO[ZEnv, ExitCode] = serve.exitCode

