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

object userService:

  type UserService = Has[UserService.Service]

  object UserService:
    trait Service:
      def find(id: Int): ZIO[Any, String, User]
      def get(): ZIO[Any, String, List[User]]

    val live: Layer[Nothing, UserService] = ZLayer.succeed(
      new Service {
        def find(userId: Int): ZIO[Any, String, User] =
          if (userId == 35)
            UIO(User("username", "certificate"))
          else
            IO.fail("Unknown user")

        def get(): ZIO[Any, String, List[User]] =
          UIO(
            List(
              User("username", "certificate"),
              User("username1", "certificate")
            )
          )
      }
    )

    def find(id: Int): ZIO[UserService, String, User] = ZIO.accessM(_.get.find(id))
    def get(): ZIO[UserService, String, List[User]] = ZIO.accessM(_.get.get())

  val usersEndpoint: PublicEndpoint[Unit, String, List[User], Any] =
    endpoint.get.in("user").errorOut(stringBody).out(jsonBody[List[User]])

  val usersServerEndpoint: ZServerEndpoint[UserService, Any] =
    usersEndpoint.zServerLogic(userId => UserService.get())

  val userEndpoint: PublicEndpoint[Int, String, User, Any] =
    endpoint.get
      .in("user" / path[Int]("userId"))
      .errorOut(stringBody)
      .out(jsonBody[User])

  val userServerEndpoint: ZServerEndpoint[UserService, Any] =
    userEndpoint.zServerLogic(userId => UserService.find(userId))

  val userServerRoutes: HttpRoutes[RIO[UserService & Clock & Blocking, *]] =
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
