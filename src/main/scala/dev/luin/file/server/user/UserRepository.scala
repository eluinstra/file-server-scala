package dev.luin.file.server.user

import dev.luin.file.server.user.User
import zio.*

object userRepo:

  type UserRepo = Has[UserRepo.Service]

  object UserRepo:
    trait Service:
      def findById(id: Long): ZIO[Any, String, User]
      def findByCertificate(certificate: String): ZIO[Any, String, User]
      def findAll(): ZIO[Any, String, List[User]]
      def countAll(): ZIO[Any, String, Int]
      def create(user: User): ZIO[Any, String, User]
      def update(user: User): ZIO[Any, String, Int]
      def delete(id: Long): ZIO[Any, String, Int]

    val any: ZLayer[UserRepo, Nothing, UserRepo] =
      ZLayer.requires[UserRepo]

    val live: Layer[Nothing, UserRepo] = ZLayer.succeed(
      new Service {
        def findById(id: Long): ZIO[Any, String, User] =
          if (id == 1)
            UIO(User(1, "username", "certificate"))
          else if (id == 2)
            UIO(User(2, "username1", "certificate"))
          else
            IO.fail("Unknown user")

        def findByCertificate(certificate: String): ZIO[Any, String, User] = ???
        def findAll(): ZIO[Any, String, List[User]] =
          UIO(
            List(
              User(1, "username", "certificate"),
              User(2, "username1", "certificate")
            )
          )

        def countAll(): ZIO[Any, String, Int] = ???
        def create(user: User): ZIO[Any, String, User] = ???
        def update(user: User): ZIO[Any, String, Int] = ???
        def delete(Id: Long): ZIO[Any, String, Int] = ???
      }
    )

    def findById(id: Long): ZIO[UserRepo, String, User] = ZIO.accessM(_.get.findById(id))
    def findByCertificate(certificate: String): ZIO[UserRepo, String, User] =
      ZIO.accessM(_.get.findByCertificate(certificate))
    def findAll(): ZIO[UserRepo, String, List[User]] = ZIO.accessM(_.get.findAll())
    def countAll(): ZIO[UserRepo, String, Int] = ZIO.accessM(_.get.countAll())
    def create(user: User): ZIO[UserRepo, String, User] = ZIO.accessM(_.get.create(user))
    def update(user: User): ZIO[UserRepo, String, Int] = ZIO.accessM(_.get.update(user))
    def delete(id: Long): ZIO[UserRepo, String, Int] = ZIO.accessM(_.get.delete(id))
