package dev.luin.file.server.user.userService

import dev.luin.file.server.user.*
import dev.luin.file.server.user.userRepo.UserRepo
import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.auto.*
import org.http4s.*
import sttp.model.StatusCode
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

import java.util.Base64
// import zio.macros.accessible

implicit val arrayByteEncoder: Encoder[Array[Byte]] =
  Encoder.encodeString.contramap[Array[Byte]](Base64.getEncoder.encodeToString)
implicit val arrayByteDecoder: Decoder[Array[Byte]] =
  Decoder.decodeString.map[Array[Byte]](Base64.getDecoder.decode)

case class NewUser(name: Username, certificate: Certificate):

  def toUser: FsUser = FsUser(name, certificate)

object NewUser:

  def decode(s: String): Certificate = Base64.getDecoder.decode(s)

// @accessible
trait UserService:
  def getUser(id: UserId): ZIO[Any, StatusCode, FsUser]
  def getUsers(): ZIO[Any, StatusCode, List[FsUser]]
  def createUser(user: NewUser): ZIO[Any, StatusCode, FsUser]
  def updateUser(id: UserId, user: NewUser): ZIO[Any, StatusCode, Long]
  def deleteUser(id: UserId): ZIO[Any, StatusCode, Long]

object UserService:
  val defaultLayer: ZLayer[Logging & Has[UserRepo], Nothing, Has[UserService]] =
    ZLayer.fromServices[Logger[String], UserRepo, UserService] { (logger, userRepo) =>
      new UserService {

        def getUser(id: UserId): ZIO[Any, StatusCode, FsUser] =
          for
            _ <- logger.info(s"getUser $id")
            user <- userRepo.findById(id).mapError(_ => StatusCode(500))
            res <- user match
              case None        => ZIO.fail(StatusCode(404))
              case Some(value) => ZIO.succeed(value)
          yield res

        def getUsers(): ZIO[Any, StatusCode, List[FsUser]] =
          for
            _ <- logger.info(s"getUsers")
            users <- userRepo.findAll().mapError(_ => StatusCode(500))
          yield users

        def createUser(user: NewUser): ZIO[Any, StatusCode, FsUser] =
          for
            _ <- logger.info(s"createUser $user")
            user <- userRepo.create(user.toUser).mapError(_ => StatusCode(500))
          yield user

        def updateUser(id: UserId, user: NewUser): ZIO[Any, StatusCode, Long] =
          lazy val u = FsUser(id, user.name, user.certificate)
          for
            _ <- logger.info(s"updateUser $u")
            updated <- userRepo.update(u).mapError(_ => StatusCode(500))
          yield updated

        def deleteUser(id: UserId): ZIO[Any, StatusCode, Long] =
          for
            _ <- logger.info(s"deleteUser $id")
            deleted <- userRepo.delete(id).mapError(_ => StatusCode(500))
          yield deleted
      }
    }

  def findById(id: UserId): ZIO[Has[UserService], StatusCode, FsUser] = ZIO.accessM(_.get.getUser(id))
  def findAll(): ZIO[Has[UserService], StatusCode, List[FsUser]] = ZIO.accessM(_.get.getUsers())
  def createUser(user: NewUser): ZIO[Has[UserService], StatusCode, FsUser] =
    ZIO.accessM(_.get.createUser(user))
  def updateUser(id: UserId, user: NewUser): ZIO[Has[UserService], StatusCode, Long] =
    ZIO.accessM(_.get.updateUser(id, user))
  def deleteUser(id: UserId): ZIO[Has[UserService], StatusCode, Long] = ZIO.accessM(_.get.deleteUser(id))

val baseEndpoint: PublicEndpoint[Unit, StatusCode, Unit, Any] =
  endpoint.in("service" / "rest" / "v1" / "users").errorOut(statusCode)

val userEndpoint: PublicEndpoint[UserId, StatusCode, FsUser, Any] =
  baseEndpoint.get
    .in(path[UserId]("userId"))
    .out(jsonBody[FsUser])

val usersEndpoint: PublicEndpoint[Unit, StatusCode, List[FsUser], Any] =
  baseEndpoint.get.out(jsonBody[List[FsUser]])

val createUserEndpoint: PublicEndpoint[NewUser, StatusCode, FsUser, Any] =
  baseEndpoint.post
    .in(jsonBody[NewUser])
    .out(jsonBody[FsUser])

val updateUserEndpoint: PublicEndpoint[(UserId, NewUser), StatusCode, Long, Any] =
  baseEndpoint.put
    .in(path[UserId]("userId").and(jsonBody[NewUser]))
    .out(jsonBody[Long])

val deleteUserEndpoint: PublicEndpoint[UserId, StatusCode, Long, Any] =
  baseEndpoint.delete
    .in(path[UserId]("userId"))
    .out(jsonBody[Long])

val userServerRoutes: HttpRoutes[RIO[Clock & Blocking & Has[UserService], *]] =
  ZHttp4sServerInterpreter()
    .from(
      List(
        userEndpoint.zServerLogic(UserService.findById(_)),
        usersEndpoint.zServerLogic(userId => UserService.findAll()),
        createUserEndpoint.zServerLogic(UserService.createUser(_)),
        updateUserEndpoint.zServerLogic(UserService.updateUser(_, _)),
        deleteUserEndpoint.zServerLogic(UserService.deleteUser(_))
      )
    )
    .toRoutes

val userSwaggerRoutes: HttpRoutes[RIO[Clock & Blocking & Has[UserService], *]] =
  ZHttp4sServerInterpreter()
    .from(
      SwaggerInterpreter()
        .fromEndpoints[RIO[Clock & Blocking & Has[UserService], *]](
          List(usersEndpoint, userEndpoint, createUserEndpoint, updateUserEndpoint, deleteUserEndpoint),
          "Users",
          "1.0"
        )
    )
    .toRoutes
