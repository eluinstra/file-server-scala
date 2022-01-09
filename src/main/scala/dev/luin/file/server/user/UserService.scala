package dev.luin.file.server.user

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

object userService:

  type UserService = Has[UserService.Service]

  object UserService:
    trait Service:
      def findById(id: Int): ZIO[Any, String, FsUser]
      def findAll(): ZIO[Any, String, List[FsUser]]

    val any: ZLayer[UserService, Nothing, UserService] =
      ZLayer.requires[UserService]

    val live: ZLayer[Logging & UserRepo, Nothing, UserService] =
      ZLayer.fromServices[Logger[String], UserRepo.Service, UserService.Service] { (logger, userRepo) =>
        new Service {
          def findById(id: Int): ZIO[Any, String, FsUser] =
            for
              _ <- logger.info(s"find user $id")
              u <- userRepo.findById(id)
            yield u

          def findAll(): ZIO[Any, String, List[FsUser]] =
            for
              _ <-logger.info(s"find all users")
              u <- userRepo.findAll()
            yield u
        }
      }

    def findById(id: Int): ZIO[UserService, String, FsUser] = ZIO.accessM(_.get.findById(id))
    def findAll(): ZIO[UserService, String, List[FsUser]] = ZIO.accessM(_.get.findAll())

  val usersEndpoint: PublicEndpoint[Unit, String, List[FsUser], Any] =
    endpoint.get.in("user").errorOut(stringBody).out(jsonBody[List[FsUser]])

  val usersServerEndpoint: ZServerEndpoint[UserService, Any] =
    usersEndpoint.zServerLogic(userId => UserService.findAll())

  val userEndpoint: PublicEndpoint[Int, String, FsUser, Any] =
    endpoint.get
      .in("user" / path[Int]("userId"))
      .errorOut(stringBody)
      .out(jsonBody[FsUser])

  val userServerEndpoint: ZServerEndpoint[UserService, Any] =
    userEndpoint.zServerLogic(userId => UserService.findById(userId))

  val userServerRoutes: HttpRoutes[RIO[Clock & Blocking & UserService, *]] =
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
