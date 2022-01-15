package dev.luin.file.server.user.userService

import dev.luin.file.server.user.FsUser
import dev.luin.file.server.user.userRepo.UserRepo
import io.circe.generic.auto.*
import org.http4s.*
import sttp.tapir.PublicEndpoint
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.*
import zio.App
import zio.ExitCode
import zio.Has
import zio.IO
import zio.Layer
import zio.RIO
import zio.UIO
import zio.URIO
import zio.ZEnv
import zio.ZIO
import zio.ZLayer
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.interop.catz.*
import zio.logging.*
// import zio.macros.accessible

// @accessible
trait UserService:
  def findById(id: Int): ZIO[Any, String, FsUser]
  def findAll(): ZIO[Any, String, List[FsUser]]

object UserService:
  val defaultLayer: ZLayer[Logging & Has[UserRepo], Nothing, Has[UserService]] =
    ZLayer.fromServices[Logger[String], UserRepo, UserService] { (logger, userRepo) =>
      new UserService {
        def findById(id: Int): ZIO[Any, String, FsUser] =
          for
            _ <- logger.info(s"find user $id")
            user <- userRepo.findById(id).mapError(_.toString)
            _ <- logger.info(s"found user $user")
          yield user

        def findAll(): ZIO[Any, String, List[FsUser]] =
          for
            _ <- logger.info(s"find all users")
            users <- userRepo.findAll().mapError(_.toString)
          yield users
      }
    }

  def findById(id: Int): ZIO[Has[UserService], String, FsUser] = ZIO.accessM(_.get.findById(id))
  def findAll(): ZIO[Has[UserService], String, List[FsUser]] = ZIO.accessM(_.get.findAll())

val usersEndpoint: PublicEndpoint[Unit, String, List[FsUser], Any] =
  endpoint.get.in("user").errorOut(stringBody).out(jsonBody[List[FsUser]])

val usersServerEndpoint: ZServerEndpoint[Has[UserService], Any] =
  usersEndpoint.zServerLogic(userId => UserService.findAll())

val userEndpoint: PublicEndpoint[Int, String, FsUser, Any] =
  endpoint.get
    .in("user" / path[Int]("userId"))
    .errorOut(stringBody)
    .out(jsonBody[FsUser])

val userServerEndpoint: ZServerEndpoint[Has[UserService], Any] =
  userEndpoint.zServerLogic(userId => UserService.findById(userId))

val userServerRoutes: HttpRoutes[RIO[Clock & Blocking & Has[UserService], *]] =
  ZHttp4sServerInterpreter()
    .from(List(usersServerEndpoint, userServerEndpoint))
    .toRoutes

val swaggerRoutes: HttpRoutes[RIO[Clock & Blocking, *]] =
  ZHttp4sServerInterpreter()
    .from(
      SwaggerInterpreter()
        .fromEndpoints[RIO[Clock & Blocking, *]](List(usersEndpoint, userEndpoint), "Users", "1.0")
    )
    .toRoutes
