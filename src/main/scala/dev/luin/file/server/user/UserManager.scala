package dev.luin.file.server.user.userManager

import dev.luin.file.server.user.*
import dev.luin.file.server.user.userManager.UserManager
import dev.luin.file.server.user.userRepo.UserRepo
import zio.*

trait UserManager:
  def findUser(id: UserId): ZIO[Any, String, Option[FsUser]]
  def findUser(certificate: Certificate): ZIO[Any, String, Option[FsUser]]

object UserManager:
  val defaultLayer: ZLayer[Has[UserRepo], Nothing, Has[UserManager]] =
    ZLayer.fromService[UserRepo, UserManager] { userRepo =>
      new UserManager {

        def findUser(id: UserId): ZIO[Any, String, Option[FsUser]] =
          for user <- userRepo.findById(id).mapError(_.toString)
          yield user

        def findUser(certificate: Certificate): ZIO[Any, String, Option[FsUser]] =
          for user <- userRepo.findByCertificate(certificate).mapError(_.toString)
          yield user
      }
    }

  def findUser(id: UserId): ZIO[Has[UserManager], String, Option[FsUser]] = ZIO.accessM(_.get.findUser(id))
  def findUser(certificate: Certificate): ZIO[Has[UserManager], String, Option[FsUser]] =
    ZIO.accessM(_.get.findUser(certificate))
