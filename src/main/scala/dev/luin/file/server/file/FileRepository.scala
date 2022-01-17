package dev.luin.file.server.file

import dev.luin.file.server.config.JdbcContext.*
import dev.luin.file.server.file.FsFile
import dev.luin.file.server.user.UserId
import io.getquill.*
import zio.*

import javax.sql.DataSource
import java.nio.file.Paths

object fileRepo:

  // @accessible
  trait FileRepo:
    def isAuthorized(path: VirtualPath, userId: UserId): IO[Error, Option[FsFile]]
    def findByPath(path: VirtualPath): IO[Error, Option[FsFile]]
    def findAll(): IO[Error, List[FsFile]]
    def create(user: FsFile): IO[Error, FsFile]
    def update(user: FsFile): IO[Error, Long]
    def delete(path: VirtualPath): IO[Error, Long]

  object FileRepo:

    val defaultLayer: ZLayer[Has[DataSource], Nothing, Has[FileRepo]] =
      ZLayer.fromService[DataSource, FileRepo] { dataSource =>
        new FileRepo {

          inline given InsertMeta[FsFile] = insertMeta(_.path)
          inline given UpdateMeta[FsFile] = updateMeta(_.path)

          //TODO: check if  necessary
          implicit val encodeFileState: MappedEncoding[FileState, Int] = MappedEncoding[FileState, Int](FileState.encode(_))
          implicit val decodeFileState: MappedEncoding[Int, FileState] = MappedEncoding[Int, FileState](FileState.decode(_))

          implicit val encodePath: MappedEncoding[java.nio.file.Path, String] = MappedEncoding[java.nio.file.Path, String](_.toString)
          implicit val decodePath: MappedEncoding[String, java.nio.file.Path] = MappedEncoding[String, java.nio.file.Path](Paths.get(_))

          def isAuthorized(path: VirtualPath, userId: UserId): IO[Error, Option[FsFile]] =
            run {
              query[FsFile].filter(u => u.virtualPath == lift(path) && u.userId == lift(userId))
            }.provide(Has(dataSource)).map(_.headOption)

          def findByPath(path: VirtualPath): IO[Error, Option[FsFile]] =
            run {
              query[FsFile].filter(_.virtualPath == lift(path))
            }.provide(Has(dataSource)).map(_.headOption)

          def findAll(): IO[Error, List[FsFile]] =
            run {
              query[FsFile]
            }.provide(Has(dataSource))

          def create(file: FsFile): IO[Error, FsFile] =
            run {
              query[FsFile].insert(lift(file))
            }.provide(Has(dataSource)).map(id => file.copy())

          def update(file: FsFile): IO[Error, Long] =
            //TODO: fix with lift(file.virtualPath)
            run {
              query[FsFile].filter(_.virtualPath == file.virtualPath).update(lift(file))
            }.provide(Has(dataSource)).map(_.longValue)

          def delete(path: VirtualPath): IO[Error, Long] =
            run {
              query[FsFile].filter(_.virtualPath == lift(path)).delete
            }.provide(Has(dataSource)).map(_.longValue)
        }
      }

    def isAuthorized(path: VirtualPath, userId: UserId): ZIO[Has[FileRepo], Error, Option[FsFile]] =
      ZIO.accessM(_.get.isAuthorized(path, userId))
    def findById(path: VirtualPath): ZIO[Has[FileRepo], Error, Option[FsFile]] = ZIO.accessM(_.get.findByPath(path))
    def findAll(): ZIO[Has[FileRepo], Error, List[FsFile]] = ZIO.accessM(_.get.findAll())
    def create(user: FsFile): ZIO[Has[FileRepo], Error, FsFile] = ZIO.accessM(_.get.create(user))
    def update(user: FsFile): ZIO[Has[FileRepo], Error, Long] = ZIO.accessM(_.get.update(user))
    def delete(path: VirtualPath): ZIO[Has[FileRepo], Error, Long] = ZIO.accessM(_.get.delete(path))
