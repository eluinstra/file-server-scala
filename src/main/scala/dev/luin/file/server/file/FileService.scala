package dev.luin.file.server.file.fileService

import dev.luin.file.server.file.*
import dev.luin.file.server.file.fileRepo.FileRepo
import dev.luin.file.server.user.UserId
import dev.luin.file.server.user.userManager.UserManager
import dev.luin.file.server.user.userService.UserService
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
import zio.stream.Stream
import java.util.Base64
import java.util.Date
import zio.stream.ZSink
import java.nio.file.Paths
// import zio.macros.accessible

// @accessible
trait FileService:
  def uploadFile(
      userId: UserId,
      file: NewFile
  ): ZIO[Any, StatusCode, VirtualPath]

object FileService:
  val defaultLayer: ZLayer[Logging & Has[UserManager] & Has[FileSystem], Nothing, Has[FileService]] =
    ZLayer.fromServices[Logger[String], UserManager, FileSystem, FileService] {
      (logger, userManager, fileSystem) =>
        new FileService {

          def uploadFile(
              userId: UserId,
              file: NewFile
          ): ZIO[Any, StatusCode, VirtualPath] =
            // streamBody(ZioStreams)(schema, format)
            for
              _ <- logger.info(s"uploadFile for $userId")
              userO <- userManager.findUser(userId).mapError(_ => StatusCode(500))
              user <- userO match
                case None        => ZIO.fail(StatusCode(404))
                case Some(value) => ZIO.succeed(value)
              // _ <- content.run(ZSink.fromFile(Paths.get("file.txt")))
              f <- fileSystem.createFile(file, user).mapError(_ => StatusCode(500))
            yield f.virtualPath
        }
    }

  def uploadFile(userId: UserId, file: NewFile): ZIO[Has[FileService], StatusCode, VirtualPath] =
    ZIO.accessM(_.get.uploadFile(userId, file))

val baseEndpoint: PublicEndpoint[Unit, StatusCode, Unit, Any] =
  endpoint.in("service" / "rest" / "v1" / "files").errorOut(statusCode)

val uploadFileEndpoint: PublicEndpoint[(UserId, NewFile), StatusCode, VirtualPath, Any] =
  baseEndpoint.post
    .in(path[UserId]("userId").and(multipartBody[NewFile]))
    .out(jsonBody[VirtualPath])

val fileServiceEndpoints = List(uploadFileEndpoint)

val fileServerEndpoints = List(uploadFileEndpoint.zServerLogic(FileService.uploadFile(_, _)))
