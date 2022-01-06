package dev.luin.file.server.user

import sttp.tapir.*
import io.circe.*
import io.circe.generic.semiauto.*

case class User(name: String, certificate: String)
