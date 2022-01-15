package dev.luin.file.server

import zio.*

object main extends App:

  override def run (args: List[String]) =
    program.exitCode
  
  val program =
    for
      _    <- Console.printLine("Hello! What is your name?")
      name <- Console.readLine
      _    <- Console.printLine(s"Hello, $name, welcome to ZIO!")
    yield ()
