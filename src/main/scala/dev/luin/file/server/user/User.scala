package dev.luin.file.server.user

import io.circe.*
import io.circe.generic.semiauto.*
import sttp.tapir.*

type UserId = Long
type Username = String
type Certificate = Array[Byte]

// opaque type UserId = Long

// object UserId:
//   def apply(val: Long): UserId = val

// case class UserId(value: Long) extends AnyVal

case class FsUser(id: UserId, name: Username, certificate: Certificate)

object FsUser:

  def apply(name: Username, certificate: Certificate) : FsUser = FsUser(-1, name, certificate)
