package dev.luin.file.server.config

import io.getquill.*
import zio.config.*
import zio.config.magnolia.*

case class ApplicationConfig(server: ServerConfig, db: DbConfig)

case class ServerConfig(host: String, port: Int)
case class DbConfig(dataSourceClassName: String, dataSource: DataSource)
case class DataSource(url: String, user: String, password: String)

val JdbcContext = new PostgresZioJdbcContext(SnakeCase)
