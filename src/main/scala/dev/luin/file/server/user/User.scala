package dev.luin.file.server.user

import io.circe.*
import io.circe.generic.semiauto.*
import sttp.tapir.*

case class User(id: Long, name: String, certificate: String)
