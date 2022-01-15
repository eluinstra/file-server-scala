package dev.luin.file.server.user

import io.circe.*
import io.circe.generic.semiauto.*
import sttp.tapir.*

case class FsUser(id: Long, name: String, certificate: Array[Byte])
