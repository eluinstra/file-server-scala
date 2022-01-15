package dev.luin.file.server.user

import dev.luin.file.server.user.FsUser
import io.getquill.*
import zio.*

import javax.sql.DataSource

object userRepo:

  val jdbcContext = new PostgresZioJdbcContext(SnakeCase)
  import jdbcContext.*

  // @accessible
  trait UserRepo:
    def findById(id: Long): IO[jdbcContext.Error, FsUser]
    // def findByCertificate(certificate: String): ZIO[Any, String, FsUser]
    def findAll(): IO[jdbcContext.Error, List[FsUser]]
  // def countAll(): ZIO[Any, String, Int]
  // def create(user: FsUser): ZIO[Any, String, FsUser]
  // def update(user: FsUser): ZIO[Any, String, Int]
  // def delete(id: Long): ZIO[Any, String, Int]

  object UserRepo:
    val defaultLayer: ZLayer[Has[DataSource], Nothing, Has[UserRepo]] =
      ZLayer.fromService[DataSource, UserRepo] { dataSource =>
        new UserRepo {
          def findById(id: Long): IO[jdbcContext.Error, FsUser] =
            run {
              query[FsUser].filter(u => u.id == lift(id))
            }.provide(Has(dataSource)).map(_.head)

          // def findByCertificate(certificate: String): ZIO[Any, String, FsUser] = ???
          def findAll(): IO[jdbcContext.Error, List[FsUser]] =
            run {
              query[FsUser]
            }.provide(Has(dataSource))
        }

      // def countAll(): ZIO[Any, String, Int] = ???
      // def create(user: FsUser): ZIO[Any, String, FsUser] = ???
      // def update(user: FsUser): ZIO[Any, String, Int] = ???
      // def delete(Id: Long): ZIO[Any, String, Int] = ???
      }

    def findById(id: Long): ZIO[Has[UserRepo], jdbcContext.Error, FsUser] = ZIO.accessM(_.get.findById(id))
    // def findByCertificate(certificate: String): ZIO[Has[UserRepo], String, FsUser] =
    //   ZIO.accessM(_.get.findByCertificate(certificate))
    def findAll(): ZIO[Has[UserRepo], jdbcContext.Error, List[FsUser]] = ZIO.accessM(_.get.findAll())
  // def countAll(): ZIO[Has[UserRepo], String, Int] = ZIO.accessM(_.get.countAll())
  // def create(user: FsUser): ZIO[Has[UserRepo], String, FsUser] = ZIO.accessM(_.get.create(user))
  // def update(user: FsUser): ZIO[Has[UserRepo], String, Int] = ZIO.accessM(_.get.update(user))
  // def delete(id: Long): ZIO[Has[UserRepo], String, Int] = ZIO.accessM(_.get.delete(id))
