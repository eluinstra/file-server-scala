package dev.luin.file.server.user

import io.circe.generic.auto.*
import sttp.tapir.PublicEndpoint
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.ztapir.*
import zhttp.http.HttpApp
import zio.{App, ExitCode, IO, RIO, UIO, URIO, ZEnv, ZIO}
import zio.clock.Clock
import zio.blocking.Blocking
import dev.luin.file.server.user.User

object UserService:

  val usersEndpoint: PublicEndpoint[Unit, String, List[User], Any] =
    endpoint.get.in("user").errorOut(stringBody).out(jsonBody[List[User]])

  val usersServerEndpoint: ZServerEndpoint[Any, Any] = usersEndpoint.zServerLogic { _ =>
      UIO(List(User("username", "certificate"), User("username1", "certificate")))
  }

  val userEndpoint: PublicEndpoint[Int, String, User, Any] =
    endpoint.get.in("user" / path[Int]("userId")).errorOut(stringBody).out(jsonBody[User])

  val userServerEndpoint: ZServerEndpoint[Any, Any] = userEndpoint.zServerLogic { userId =>
    if (userId == 35)
      UIO(User("username", "certificate"))
    else
      IO.fail("Unknown user")
  }
  val userServerRoutes: HttpApp[Any, Throwable] = ZioHttpInterpreter().toHttp(List(usersServerEndpoint, userServerEndpoint))
