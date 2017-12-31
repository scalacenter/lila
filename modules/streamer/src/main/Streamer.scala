package lila.streamer

import org.joda.time.DateTime

import lila.user.User

case class Streamer(
    _id: Streamer.Id, // user ID
    listed: Streamer.Listed, // user wants to be in the list
    approval: Streamer.Approval,
    chatEnabled: Streamer.ChatEnabled, // embed chat inside lichess
    picturePath: Option[Streamer.PicturePath],
    name: Streamer.Name,
    description: Option[Streamer.Description],
    twitch: Option[Streamer.Twitch],
    youTube: Option[Streamer.YouTube],
    sorting: Streamer.Sorting,
    createdAt: DateTime,
    updatedAt: DateTime
) {

  def id = _id

  def is(user: User) = id.value == user.id

  def hasPicture = picturePath.isDefined

  def isListed = listed.value && approval.granted

  def seenAt: Option[DateTime] = sorting.seenAt
  def liveAt: Option[DateTime] = (twitch.flatMap(_.live.liveAt), youTube.flatMap(_.live.liveAt)) match {
    case (Some(twitch), Some(youTube)) => Some {
      if (twitch isAfter youTube) twitch else youTube
    }
    case (twitch, youTube) => twitch orElse youTube
  }

  def completeEnough = {
    twitch.isDefined || youTube.isDefined
  } && description.isDefined && hasPicture
}

object Streamer {

  def make(user: User) = Streamer(
    _id = Id(user.id),
    listed = Listed(true),
    approval = Approval(
      requested = false,
      granted = false,
      ignored = false,
      autoFeatured = false
    ),
    chatEnabled = ChatEnabled(true),
    picturePath = none,
    name = Name(user.realNameOrUsername),
    description = none,
    twitch = none,
    youTube = none,
    sorting = Sorting.empty,
    createdAt = DateTime.now,
    updatedAt = DateTime.now
  )

  case class Id(value: User.ID) extends AnyVal with StringValue
  case class Listed(value: Boolean) extends AnyVal
  case class Approval(
      requested: Boolean, // user requests a mod to approve
      granted: Boolean, // a mod approved
      ignored: Boolean, // further requests are ignored
      autoFeatured: Boolean // on homepage when title contains "lichess.org"
  )
  case class ChatEnabled(value: Boolean) extends AnyVal
  case class PicturePath(value: String) extends AnyVal with StringValue
  case class Name(value: String) extends AnyVal with StringValue
  case class Description(value: String) extends AnyVal with StringValue
  case class Sorting(streaming: Boolean, seenAt: Option[DateTime])
  object Sorting { val empty = Sorting(false, none) }
  case class Live(liveAt: Option[DateTime], checkedAt: Option[DateTime]) {
    def now = liveAt ?? { l =>
      checkedAt ?? { l == }
    }
  }
  object Live { val empty = Live(none, none) }
  case class Twitch(userId: String, live: Live) {
    def fullUrl = s"https://www.twitch.tv/$userId"
    def minUrl = s"twitch.tv/$userId"
  }
  object Twitch {
    private val UserIdRegex = """^(\w{2,24})$""".r
    private val UrlRegex = """.*twitch\.tv/(\w{2,24}).*""".r
    // https://www.twitch.tv/chessnetwork
    def parseUserId(str: String): Option[String] = str match {
      case UserIdRegex(u) => u.some
      case UrlRegex(u) => u.some
      case _ => none
    }
  }
  case class YouTube(channelId: String, live: Live) {
    def fullUrl = s"https://www.youtube.com/channel/$channelId"
    def minUrl = s"youtube.com/channel/$channelId"
  }
  object YouTube {
    private val ChannelIdRegex = """^(\w{11})$""".r
    private val UrlRegex = """.*youtube\.com/channel/(\w{11}).*""".r
    def parseChannelId(str: String): Option[String] = str match {
      case ChannelIdRegex(c) => c.some
      case UrlRegex(c) => c.some
      case _ => none
    }
  }

  case class WithUser(streamer: Streamer, user: User)
}
