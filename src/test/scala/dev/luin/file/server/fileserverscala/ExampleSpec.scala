package dev.luin.file.server

import zio.*
import zio.test.*
import zio.test.Assertion.*

object ExampleSpec extends DefaultRunnableSpec:

  def spec = suite("ExampleSpec")(
    test("ZIO.succeed succeeds with specified value") {
      assertM(ZIO.succeed(1 + 1))(equalTo(2))
    }
  )

  // def spec = suite("ExampleSpec")(
  //   test("and") {
  //     for
  //       x <- ZIO.succeed(1)
  //       y <- ZIO.succeed(2)
  //     yield assert(x)(equalTo(1)) && assert(y)(equalTo(2))
  //   }
  // )
