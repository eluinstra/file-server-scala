package dev.luin.file.server

import zio.*
import zio.console.*

object main extends App:

  override def run(args: List[String]) =
    program.exitCode

  val program =
    for
      _ <- putStrLn("Hello! What is your name?")
      name <- getStrLn
      _ <- putStrLn(s"Hello, $name, welcome to ZIO!")
    yield ()
