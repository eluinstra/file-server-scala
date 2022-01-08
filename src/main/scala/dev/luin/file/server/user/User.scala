package dev.luin.file.server.user

import io.circe.*
import io.circe.generic.semiauto.*
import sttp.tapir.*

case class User(name: String, certificate: String)
