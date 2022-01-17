package dev.luin.file.server.file

import java.io.File
import java.nio.file.Paths
import zio.*
import dev.luin.file.server.config.ApplicationConfig
import zio.random.Random

case class RandomFile (path: java.nio.file.Path) //, file : File)

object RandomFile:

  def createRandomFile(random: Random.Service): ZIO[Any, Throwable, RandomFile] =
    createRandomFile(randomPathSupplier(random, "", 32))

  def randomPathSupplier(random: Random.Service, baseDir: String, fileLength: Int) =
    for
      filename <- random.nextString(fileLength)
      path <- ZIO.succeed(Paths.get(baseDir,filename))
    yield path
    
  def createRandomFile(randomPathSupplier: ZIO[Any, Throwable, java.nio.file.Path]) =
    for
      path <- randomPathSupplier
      file <- ZIO.succeed(RandomFile(path))
    yield file
