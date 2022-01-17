package dev.luin.file.server

import cats.syntax.all.*
import dev.luin.file.server.config.*
import dev.luin.file.server.dbMigrator.DbMigrator
import dev.luin.file.server.file.FileSystem
import dev.luin.file.server.file.fileRepo.FileRepo
import dev.luin.file.server.file.fileService.*
import dev.luin.file.server.user.userManager.UserManager
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
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import org.http4s.HttpRoutes
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.{fileServerEndpoints as _, *}

object RestServer extends App:

  lazy val config = descriptor[ApplicationConfig]

  lazy val loggingLayer =
    Logging.console(
      logLevel = LogLevel.Info,
      format = LogFormat.ColoredLogFormat()
    ) >>> Logging.withRootLoggerName("file-server")

  lazy val dataSourceLayer = DataSourceLayer.fromPrefix("db").orDie

  type Env = Has[UserService] & Has[FileService]

  val serverRoutes: HttpRoutes[RIO[Clock & Blocking & Env, *]] =
    ZHttp4sServerInterpreter()
      .from(
        userServerEndpoints.map(_.widen[Env]) ++ fileServerEndpoints.map(_.widen[Env])
      )
      .toRoutes

  val swaggerRoutes: HttpRoutes[RIO[Clock & Blocking & Env, *]] =
    ZHttp4sServerInterpreter()
      .from(
        SwaggerInterpreter()
          .fromEndpoints[RIO[Clock & Blocking & Env, *]](
            userServiceEndpoints ++ fileServiceEndpoints,
            "Users",
            "1.0"
          )
      )
      .toRoutes

  val program: ZIO[ZEnv & Has[ApplicationConfig] & Has[DbMigrator] & Logging & Env, Throwable, Unit] =
    ZIO.runtime.flatMap { implicit runtime =>
      for
        conf <- getConfig[ApplicationConfig]
        _ <- DbMigrator.migrate(
          conf.db.dataSource.url.stripPrefix("\"").stripSuffix("\""),
          conf.db.dataSource.user,
          conf.db.dataSource.password
        )
        _ <- log.info(s"Starting with $conf")
        _ <- BlazeServerBuilder[RIO[Clock & Blocking & Env, *]]
          .withExecutionContext(runtime.platform.executor.asEC)
          .bindHttp(conf.server.port, conf.server.host)
          .withHttpApp(
            Router("/" -> (serverRoutes <+> swaggerRoutes)).orNotFound
          )
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
    lazy val userServiceLayer = (loggingLayer ++ userRepoLayer >>> UserService.defaultLayer)
    lazy val userManagerLayer = (userRepoLayer >>> UserManager.defaultLayer)
    lazy val fileRepoLayer = (dataSourceLayer >>> FileRepo.defaultLayer)
    lazy val fileSystemLayer = (ZEnv.live ++ fileRepoLayer >>> FileSystem.defaultLayer)
    lazy val fileServiceLayer =
      (loggingLayer ++ userManagerLayer ++ fileSystemLayer >>> FileService.defaultLayer)
    program
      .provideLayer(
        ZEnv.live ++ configLayer ++ DbMigrator.defaultLayer ++ loggingLayer ++ userServiceLayer ++ fileServiceLayer
      )
      .exitCode
