package dev.luin.file.server.file

import dev.luin.file.server.user.UserId
import zio.stream.*
// TODO Quill does not support Instant
// import java.time.Instant
import java.util.Date

type VirtualPath = String
type Filename = String
type ContentType = String
type Md5Checksum = String
type Sha256Checksum = String
type Timestamp = Date
type FileLength = Long

case class NewFile(
    filename: Filename,
    contentType: ContentType,
    sha256Checksum: Option[Sha256Checksum],
    startDate: Option[Date],
    endDate: Option[Date]
    // content: Stream[Throwable, Byte]
)

case class FsFile(
    virtualPath: VirtualPath,
    path: java.nio.file.Path,
    filename: Filename,
    contentType: ContentType,
    md5Checksum: Md5Checksum,
    sha256Checksum: Sha256Checksum,
    timestamp: Date,
    startDate: Option[Date],
    endDate: Option[Date],
    userId: UserId,
    length: FileLength,
    state: FileState
):
  def hasTimeFrame: Boolean =
    startDate.isDefined && endDate.isDefined

  def isValidTimeFrame(referenceDate: Date): Boolean =
    (startDate, endDate) match
      case (None, None)       => true
      case (Some(s), None)    => s.compareTo(referenceDate) <= 0
      case (None, Some(e))    => e.compareTo(referenceDate) > 0
      case (Some(s), Some(e)) => s.compareTo(referenceDate) <= 0 && e.compareTo(referenceDate) > 0

enum FileState:
  case Complete, Partial

object FileState:

  def encode(s: FileState): Int =
    s match
      case Complete => 0
      case Partial  => 1

  def decode(i: Int): FileState =
    i match
      case 0 => Complete
      case 1 => Partial

// case class TimeFrame(startDate: Option[Date], endDate: Option[Date]):

//   def hasTimeFrame: Boolean =
//     startDate.isDefined && endDate.isDefined

//   def isValid(referenceDate: Date): Boolean =
//     (startDate, endDate) match
//       case (None, None) => true
//       case (Some(s), None) => s.compareTo(referenceDate) <= 0
//       case (None, Some(e)) => e.compareTo(referenceDate) > 0
//       case (Some(s), Some(e)) => s.compareTo(referenceDate) <= 0 && e.compareTo(referenceDate) > 0

// object TimeFrame:

//   val emptyTimeFrame = new TimeFrame(None, None)

//   def apply(startDate: Option[Date], endDate: Option[Date]) =
//     (startDate, endDate) match
//       // case (None, None) => Return no TimeFrame
//       case (Some(s), Some(e)) =>
//         if s `before` e then new TimeFrame(Some(s), Some(e))
//         //TODO use validation
//         else throw new IllegalStateException("StartDate not before EndDate")
//       case (s, e) => new TimeFrame(s, e)
