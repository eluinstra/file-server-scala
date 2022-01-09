package dev.luin.file.server

import io.getquill.*
import dev.luin.file.server.user.FsUser

object Database:

  val dbContext = new PostgresJdbcContext(SnakeCase, "db")
  import dbContext.*

  @main def runQuery(): Unit = {
    // inline def createUser = quote{(user: FsUser) => query[FsUser].insert(user)}
    // dbContext.run(createUser(lift(FsUser(1, "username", "certificate"))))

    inline def userById = quote { (id: Long) =>
      query[FsUser].filter(u => u.id == lift(1))
    }
    val result: List[FsUser] = dbContext.run(userById(1))
    println(result)
  }