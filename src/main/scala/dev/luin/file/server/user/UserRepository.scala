package dev.luin.file.server.user

import dev.luin.file.server.user.FsUser
import io.getquill.*
import zio.*

object userRepo:

  type UserRepo = Has[UserRepo.Service]

  object UserRepo:
    trait Service:
      def findById(id: Long): ZIO[Any, String, FsUser]
      def findByCertificate(certificate: String): ZIO[Any, String, FsUser]
      def findAll(): ZIO[Any, String, List[FsUser]]
      def countAll(): ZIO[Any, String, Int]
      def create(user: FsUser): ZIO[Any, String, FsUser]
      def update(user: FsUser): ZIO[Any, String, Int]
      def delete(id: Long): ZIO[Any, String, Int]

    val any: ZLayer[UserRepo, Nothing, UserRepo] =
      ZLayer.requires[UserRepo]

    val live: Layer[Nothing, UserRepo] = ZLayer.succeed(
      new Service {
        def findById(id: Long): ZIO[Any, String, FsUser] =
          if (id == 1)
            UIO(FsUser(1, "username", "certificate"))
          else if (id == 2)
            UIO(FsUser(2, "username1", "certificate"))
          else
            IO.fail("Unknown user")

        def findByCertificate(certificate: String): ZIO[Any, String, FsUser] = ???
        def findAll(): ZIO[Any, String, List[FsUser]] =
          UIO(
            List(
              FsUser(1, "username", "certificate"),
              FsUser(2, "username1", "certificate")
            )
          )

        def countAll(): ZIO[Any, String, Int] = ???
        def create(user: FsUser): ZIO[Any, String, FsUser] = ???
        def update(user: FsUser): ZIO[Any, String, Int] = ???
        def delete(Id: Long): ZIO[Any, String, Int] = ???
      }
    )

    def findById(id: Long): ZIO[UserRepo, String, FsUser] = ZIO.accessM(_.get.findById(id))
    def findByCertificate(certificate: String): ZIO[UserRepo, String, FsUser] =
      ZIO.accessM(_.get.findByCertificate(certificate))
    def findAll(): ZIO[UserRepo, String, List[FsUser]] = ZIO.accessM(_.get.findAll())
    def countAll(): ZIO[UserRepo, String, Int] = ZIO.accessM(_.get.countAll())
    def create(user: FsUser): ZIO[UserRepo, String, FsUser] = ZIO.accessM(_.get.create(user))
    def update(user: FsUser): ZIO[UserRepo, String, Int] = ZIO.accessM(_.get.update(user))
    def delete(id: Long): ZIO[UserRepo, String, Int] = ZIO.accessM(_.get.delete(id))
