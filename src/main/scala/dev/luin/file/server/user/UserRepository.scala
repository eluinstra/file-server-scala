package dev.luin.file.server.user

import dev.luin.file.server.config.JdbcContext.*
import dev.luin.file.server.user.FsUser
import io.getquill.*
import zio.*

import javax.sql.DataSource

object userRepo:

  // @accessible
  trait UserRepo:
    def findById(id: UserId): IO[Error, Option[FsUser]]
    def findByCertificate(certificate: Certificate): IO[Error, Option[FsUser]]
    def findAll(): IO[Error, List[FsUser]]
    def create(user: FsUser): IO[Error, FsUser]
    def update(user: FsUser): IO[Error, Long]
    def delete(id: UserId): IO[Error, Long]

  object UserRepo:

    val defaultLayer: ZLayer[Has[DataSource], Nothing, Has[UserRepo]] =
      ZLayer.fromService[DataSource, UserRepo] { dataSource =>
        new UserRepo {

          inline given InsertMeta[FsUser] = insertMeta(_.id)
          inline given UpdateMeta[FsUser] = updateMeta(_.id)

          def findById(id: UserId): IO[Error, Option[FsUser]] =
            run {
              query[FsUser].filter(_.id == lift(id))
            }.provide(Has(dataSource)).map(_.headOption)

          def findByCertificate(certificate: Certificate): IO[Error, Option[FsUser]] =
            run {
              query[FsUser].filter(u => u.certificate == lift(certificate))
            }.provide(Has(dataSource)).map(_.headOption)

          def findAll(): IO[Error, List[FsUser]] =
            run {
              query[FsUser]
            }.provide(Has(dataSource))

          def create(user: FsUser): IO[Error, FsUser] =
            run {
              query[FsUser].insert(lift(user)).returning(_.id)
            }.provide(Has(dataSource)).map(id => user.copy(id = id))

          def update(user: FsUser): IO[Error, Long] =
            //TODO: fix with lift(user.id)
            run {
              query[FsUser].filter(_.id == user.id).update(lift(user))
            }.provide(Has(dataSource)).map(_.longValue)

          def delete(id: UserId): IO[Error, Long] =
            run {
              query[FsUser].filter(_.id == lift(id)).delete
            }.provide(Has(dataSource)).map(_.longValue)
        }
      }

    def findById(id: UserId): ZIO[Has[UserRepo], Error, Option[FsUser]] = ZIO.accessM(_.get.findById(id))
    def findByCertificate(certificate: Certificate): ZIO[Has[UserRepo], Error, Option[FsUser]] =
      ZIO.accessM(_.get.findByCertificate(certificate))
    def findAll(): ZIO[Has[UserRepo], Error, List[FsUser]] = ZIO.accessM(_.get.findAll())
    def create(user: FsUser): ZIO[Has[UserRepo], Error, FsUser] = ZIO.accessM(_.get.create(user))
    def update(user: FsUser): ZIO[Has[UserRepo], Error, Long] = ZIO.accessM(_.get.update(user))
    def delete(id: UserId): ZIO[Has[UserRepo], Error, Long] = ZIO.accessM(_.get.delete(id))
