package dev.luin.file.server.file

import dev.luin.file.server.file.*
import dev.luin.file.server.file.Filename
import dev.luin.file.server.user.FsUser
import dev.luin.file.server.file.fileRepo.FileRepo
import zio.*
import zio.random.Random
import java.util.Date

trait FileSystem:
  def createFile(file: NewFile, user: FsUser): ZIO[Any, Throwable, FsFile]

object FileSystem:
  val defaultLayer: ZLayer[Random & Has[FileRepo], Nothing, Has[FileSystem]] =
    ZLayer.fromServices[Random.Service, FileRepo, FileSystem] { (random, userRepo) =>
      new FileSystem {

        def createFile(file: NewFile, user: FsUser): ZIO[Any, Throwable, FsFile] =
          for
            randomFile <- RandomFile.createRandomFile(random)
            md5Checksum <- ZIO.succeed("123")
            sha256Checksum <- ZIO.succeed("123")
            randomVirtualPath <- random.nextString(32)
            timestamp <- ZIO.succeed(new Date())
          yield FsFile(
            randomVirtualPath,
            randomFile.path,
            file.filename,
            file.contentType,
            md5Checksum,
            sha256Checksum,
            timestamp,
            file.startDate,
            file.endDate,
            user.id,
            0,
            FileState.Partial
          )
      }
    }

  def createFile(file: NewFile, user: FsUser): ZIO[Has[FileSystem], Throwable, FsFile] =
    ZIO.accessM(_.get.createFile(file, user))
