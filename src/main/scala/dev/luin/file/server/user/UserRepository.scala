package dev.luin.file.server.user

import dev.luin.file.server.user.FsUser
import io.getquill.*
import zio.*

import javax.sql.DataSource

object userRepo:

  type UserRepo = Has[UserRepo.Service]

  val jdbcContext = new PostgresZioJdbcContext(SnakeCase)
  import jdbcContext.*

  object UserRepo:
    trait Service:
      def findById(id: Long): IO[jdbcContext.Error, FsUser]
      // def findByCertificate(certificate: String): ZIO[Any, String, FsUser]
      def findAll(): IO[jdbcContext.Error, List[FsUser]]
      // def countAll(): ZIO[Any, String, Int]
      // def create(user: FsUser): ZIO[Any, String, FsUser]
      // def update(user: FsUser): ZIO[Any, String, Int]
      // def delete(id: Long): ZIO[Any, String, Int]

    val any: ZLayer[UserRepo, Nothing, UserRepo] =
      ZLayer.requires[UserRepo]

    val live: ZLayer[Has[DataSource], Nothing, UserRepo] =
      ZLayer.fromService[DataSource, UserRepo.Service] { dataSource =>
        new Service {
          inline def findById(id: Long): IO[jdbcContext.Error, FsUser] =
            run {
              quote {
                query[FsUser].filter(u => u.id == lift(id))
              }
            }.provide(Has(dataSource)).map(_.head)

          // def findByCertificate(certificate: String): ZIO[Any, String, FsUser] = ???
          def findAll(): IO[jdbcContext.Error, List[FsUser]] =
            run {
              quote {
                query[FsUser]
              }
            }.provide(Has(dataSource))
          }

        // def countAll(): ZIO[Any, String, Int] = ???
        // def create(user: FsUser): ZIO[Any, String, FsUser] = ???
        // def update(user: FsUser): ZIO[Any, String, Int] = ???
        // def delete(Id: Long): ZIO[Any, String, Int] = ???
      }

    def findById(id: Long): ZIO[UserRepo, jdbcContext.Error, FsUser] = ZIO.accessM(_.get.findById(id))
    // def findByCertificate(certificate: String): ZIO[UserRepo, String, FsUser] =
    //   ZIO.accessM(_.get.findByCertificate(certificate))
    def findAll(): ZIO[UserRepo, jdbcContext.Error, List[FsUser]] = ZIO.accessM(_.get.findAll())
    // def countAll(): ZIO[UserRepo, String, Int] = ZIO.accessM(_.get.countAll())
    // def create(user: FsUser): ZIO[UserRepo, String, FsUser] = ZIO.accessM(_.get.create(user))
    // def update(user: FsUser): ZIO[UserRepo, String, Int] = ZIO.accessM(_.get.update(user))
    // def delete(id: Long): ZIO[UserRepo, String, Int] = ZIO.accessM(_.get.delete(id))
