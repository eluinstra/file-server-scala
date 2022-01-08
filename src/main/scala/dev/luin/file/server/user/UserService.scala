package dev.luin.file.server.user

import dev.luin.file.server.user.User
import io.circe.generic.auto.*
import org.http4s.*
import sttp.tapir.PublicEndpoint
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.*
import zio.{App, ExitCode, Has, IO, Layer, RIO, UIO, URIO, ZEnv, ZIO, ZLayer}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.interop.catz.*
import zio.logging.*
import dev.luin.file.server.user.userRepo.UserRepo

object userService:

  type UserService = Has[UserService.Service]

  object UserService:
    trait Service:
      def findById(id: Int): ZIO[Any, String, User]
      def findAll(): ZIO[Any, String, List[User]]

    val any: ZLayer[UserService, Nothing, UserService] =
      ZLayer.requires[UserService]

    val live: ZLayer[Logging & UserRepo, Nothing, UserService] =
      ZLayer.fromServices[Logger[String], UserRepo.Service, UserService.Service] { (logger, userRepo) =>
        new Service {
          def findById(id: Int): ZIO[Any, String, User] =
            for
              _ <- logger.info(s"find user $id")
              u <- userRepo.findById(id)
            yield u

          def findAll(): ZIO[Any, String, List[User]] =
            for
              _ <-logger.info(s"find all users")
              u <- userRepo.findAll()
            yield u
        }
      }

    def findById(id: Int): ZIO[UserService, String, User] = ZIO.accessM(_.get.findById(id))
    def findAll(): ZIO[UserService, String, List[User]] = ZIO.accessM(_.get.findAll())

  val usersEndpoint: PublicEndpoint[Unit, String, List[User], Any] =
    endpoint.get.in("user").errorOut(stringBody).out(jsonBody[List[User]])

  val usersServerEndpoint: ZServerEndpoint[UserService, Any] =
    usersEndpoint.zServerLogic(userId => UserService.findAll())

  val userEndpoint: PublicEndpoint[Int, String, User, Any] =
    endpoint.get
      .in("user" / path[Int]("userId"))
      .errorOut(stringBody)
      .out(jsonBody[User])

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
