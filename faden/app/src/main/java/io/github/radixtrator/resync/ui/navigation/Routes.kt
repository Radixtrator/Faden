package io.github.radixtrator.resync.ui.navigation

import androidx.navigation3.runtime.NavKey
import io.github.radixtrator.resync.data.FilterType
import io.github.radixtrator.resync.data.PostLayout
import io.github.radixtrator.resync.data.Redgifs
import io.github.radixtrator.resync.data.Streamable
import io.github.radixtrator.resync.data.UserWhere
import io.github.radixtrator.resync.ui.settings.SettingsCategory
import kotlinx.serialization.Serializable

@Serializable
data object SetupRoute : NavKey

@Serializable
data object LoginRoute : NavKey

/** Sync's settings: the category list, or with [category] that category's page; [highlight] is a row title to scroll to and flash. */
@Serializable
data class SettingsRoute(val category: SettingsCategory? = null, val highlight: String? = null) : NavKey

/** Sync's Theme management (Settings › Appearance): previews, saved themes, share and load. */
@Serializable
data object ThemeManagementRoute : NavKey

/** Sync's Customize view ("View type" settings), opened on [layout]'s options; [highlight] as in [SettingsRoute]. */
@Serializable
data class CustomizeViewRoute(val layout: PostLayout = PostLayout.CARD, val highlight: String? = null) : NavKey

/** Sync's FilterActivity: one list editor per filter [type] (Settings › Filters). */
@Serializable
data class FilterEditorRoute(val type: FilterType) : NavKey

/**
 * [home] screens are the bottom-navigation tabs (Sync's Posts and Inbox): they show the account
 * avatar instead of a back arrow, plus the navigation bar and the More actions FAB. Feeds picked in
 * Explore stay [home] too; feeds opened from a link are pushed as ordinary pages.
 */
@Serializable
data class InboxRoute(val home: Boolean = false) : NavKey

/** [subreddit] null = front page, unless [domain] asks for a site's posts (Sync's "Explore domain"). See [InboxRoute] for [home]. */
@Serializable
data class FeedRoute(val subreddit: String? = null, val home: Boolean = false, val domain: String? = null) : NavKey

/** [commentId] opens just that comment's thread, with [context] parent comments above it. */
@Serializable
data class PostRoute(val postId: String, val commentId: String? = null, val context: Int = 0) : NavKey

/** [postId]: the post the media came from, for the info sheet's Comments button. */
@Serializable
data class ImageRoute(val urls: List<String>, val start: Int = 0, val postId: String? = null) : NavKey

/** See [ImageRoute] for [postId]. */
@Serializable
/** [url] can be a RedGifs page, resolved by the player; [fallback] is played if that fails. */
data class VideoRoute(val url: String, val loop: Boolean = false, val postId: String? = null, val fallback: String? = null) : NavKey

@Serializable
data class UserRoute(val name: String, val tab: UserWhere = UserWhere.OVERVIEW) : NavKey

/** [communities] opens on the Communities tab (Explore's "Search all subs"). */
@Serializable
data class SearchRoute(val query: String = "", val subreddit: String? = null, val communities: Boolean = false) : NavKey

/** [subreddit]'s posts with [flair]: Reddit filters by flair through a flair_name search in the community. */
fun flairSearch(subreddit: String, flair: String): SearchRoute =
    SearchRoute("flair_name:\"${flair.replace("\"", "")}\"", subreddit = subreddit)

/** Sync's Swipe mode over [subreddit]'s feed (null = front page). */
@Serializable
data class SwipeModeRoute(val subreddit: String? = null) : NavKey

/** Sync's Moderator screen for [subreddit]: modqueue, reports, spam, edited, unmoderated and comments. */
@Serializable
data class ModRoute(val subreddit: String) : NavKey

/** New-style mod mail; [subreddit] opens it narrowed to that community (null = every community you moderate). */
@Serializable
data class ModMailRoute(val subreddit: String? = null) : NavKey

/** One mod mail conversation, by Reddit's conversation [id]. */
@Serializable
data class ModMailConversationRoute(val id: String) : NavKey

/**
 * A new post. The rest prefill it, from something shared to Faden: a [title], a link post's [url], a text post's
 * [text], or the pictures of a photo or gallery post as content: [photos] URIs.
 */
@Serializable
data class SubmitRoute(
    val subreddit: String? = null,
    val title: String = "",
    val url: String = "",
    val text: String = "",
    val photos: List<String> = emptyList(),
) : NavKey

enum class ComposeMode { REPLY, EDIT, MESSAGE }

/**
 * The markdown editor. REPLY/EDIT act on [target] (a fullname); [quote] is shown above the editor for
 * context. MESSAGE uses [to] and [subject].
 */
@Serializable
data class ComposeRoute(
    val mode: ComposeMode,
    val target: String? = null,
    val quote: String? = null,
    val initial: String = "",
    val to: String = "",
    val subject: String = "",
) : NavKey

/** YouTube video links, which Faden can't play itself: they get a play badge and open in the YouTube app. */
object YouTube {
    private val VIDEO = Regex(
        """^https?://(?:(?:www\.|m\.|music\.)?youtube\.com/(?:watch\?(?:.*&)?v=|shorts/|live/|embed/)|youtu\.be/)[\w-]{6,}""",
        RegexOption.IGNORE_CASE,
    )

    fun isVideo(url: String): Boolean = VIDEO.containsMatchIn(url)
}

/** Maps URLs onto in-app screens, like Sync did for links in posts and comments. */
object RedditLinks {
    private val POST = Regex(
        """^https?://(?:[\w-]+\.)?reddit\.com/(?:r/[^/]+/)?comments/([a-z0-9]+)(?:/[^/?#]*/([a-z0-9]+))?""",
        RegexOption.IGNORE_CASE,
    )
    private val SHORT = Regex("""^https?://redd\.it/([a-z0-9]+)/?(?:[?#].*)?$""", RegexOption.IGNORE_CASE)
    private val GALLERY = Regex("""^https?://(?:[\w-]+\.)?reddit\.com/gallery/([a-z0-9]+)/?(?:[?#].*)?$""", RegexOption.IGNORE_CASE)
    /** A feed, optionally with its sort ("/r/pics/top"), which is dropped. */
    private val SUBREDDIT = Regex(
        """^https?://(?:[\w-]+\.)?reddit\.com/r/([\w+]+)(?:/(?:hot|new|top|rising|controversial|best))?/?(?:[?#].*)?$""",
        RegexOption.IGNORE_CASE,
    )
    private val USER = Regex("""^https?://(?:[\w-]+\.)?reddit\.com/(?:u|user)/([\w-]+)(?:/(\w+))?/?(?:[?#].*)?$""", RegexOption.IGNORE_CASE)
    private val FRONT_PAGE = Regex("""^https?://(?:[\w-]+\.)?reddit\.com/?(?:[?#].*)?$""", RegexOption.IGNORE_CASE)
    /** The app's Share links ("/r/pics/s/AbC123"), which only Reddit can turn into the permalink they stand for. */
    private val SHARE = Regex("""^https?://(?:[\w-]+\.)?reddit\.com/(?:r|u|user)/[\w-]+/s/[a-z0-9]+/?(?:[?#].*)?$""", RegexOption.IGNORE_CASE)
    private val REDDIT_HOST = Regex("""^https?://(?:(?:[\w-]+\.)?reddit\.com|redd\.it)(?:[/?#:].*)?$""", RegexOption.IGNORE_CASE)

    /** A Share link, to resolve with [io.github.radixtrator.resync.data.ShareLinks] before [route]. */
    fun isShareLink(url: String): Boolean = SHARE.matches(url)

    /** Pages on reddit.com or redd.it, which Faden is set up to open, so a browser has to be asked for by name. */
    fun isRedditPage(url: String): Boolean = REDDIT_HOST.matches(url)
    private val REDDIT_VIDEO = Regex("""^https?://v\.redd\.it/([a-z0-9]+)/?$""", RegexOption.IGNORE_CASE)
    private val IMAGE = Regex("""^https?://[^?#]+\.(?:jpe?g|png|gif|webp)(?:[?#].*)?$""", RegexOption.IGNORE_CASE)
    private val GIFV = Regex("""^(https?://[^?#]+)\.gifv(?:[?#].*)?$""", RegexOption.IGNORE_CASE)
    private val MP4 = Regex("""^https?://[^?#]+\.mp4(?:[?#].*)?$""", RegexOption.IGNORE_CASE)

    fun route(url: String): NavKey? {
        POST.find(url)?.let { m -> return PostRoute(m.groupValues[1], m.groupValues[2].ifEmpty { null }, context = if (m.groupValues[2].isEmpty()) 0 else 3) }
        SHORT.find(url)?.let { return PostRoute(it.groupValues[1]) }
        GALLERY.find(url)?.let { return PostRoute(it.groupValues[1]) }
        SUBREDDIT.find(url)?.let { return FeedRoute(it.groupValues[1]) }
        USER.find(url)?.let { m ->
            val section = m.groupValues[2]
            if (section.isEmpty()) return UserRoute(m.groupValues[1])
            // A profile section ("/user/x/submitted"); other pages under a profile aren't in the app.
            UserWhere.entries.firstOrNull { it.path.equals(section, ignoreCase = true) && !it.ownOnly }?.let { return UserRoute(m.groupValues[1], it) }
        }
        if (FRONT_PAGE.matches(url)) return FeedRoute()
        REDDIT_VIDEO.find(url)?.let { return VideoRoute("https://v.redd.it/${it.groupValues[1]}/HLSPlaylist.m3u8") }
        GIFV.find(url)?.let { return VideoRoute(it.groupValues[1] + ".mp4", loop = true) }
        if (Redgifs.id(url) != null) return VideoRoute(url, loop = true)
        if (Streamable.id(url) != null) return VideoRoute(url)
        if (MP4.matches(url)) return VideoRoute(url)
        if (IMAGE.matches(url)) return ImageRoute(listOf(url))
        return null
    }
}
