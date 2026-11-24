package io.github.radixtrator.resync.data

import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.radixtrator.resync.i18n.t
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class ThemeMode(private val en: String) {
    SYSTEM("Follow system"), LIGHT("Light"), DARK("Dark"), BLACK("Black (AMOLED)");

    val label: String get() = t(en)
}

/** Sync's six feed layouts, in its Change view order. Names are persisted, so CARD/COMPACT keep theirs. */
enum class PostLayout(private val en: String) {
    LIST("List"),
    COMPACT("Compact"),
    SMALLER_CARDS("Smaller cards"),
    SMALL_CARDS("Small cards"),
    CARD("Cards"),
    SLIDES("Slides");

    val label: String get() = t(en)
}

/** Sync's "Base font size": scales every sp in the app. SMALLER is Sync's default on phones. */
enum class BaseFontSize(private val en: String, val scale: Float) {
    SMALLEST("Smallest", 0.8f),
    SMALLER("Smaller", 0.9f),
    NORMAL("Normal", 1.0f),
    LARGER("Larger", 1.1f),
    LARGEST("Largest", 1.25f);

    val label: String get() = t(en)
}

/**
 * The base palette when no accent is picked: Faden's own blue-and-orange palette, or Sync's sampled "Classic" colors. With an
 * accent picked both generate the same way from it, so the choice only shows with Default color.
 */
enum class ThemePalette { FADEN, CLASSIC }

/**
 * Typeface choices. Figtree (Faden's own) and Roboto (Sync's) are bundled; Product Sans can't be, so the rest are the
 * system's families.
 */
enum class Typeface(private val en: String) {
    FIGTREE("Figtree"),
    ROBOTO("Roboto"),
    SANS("Sans serif (system)"),
    SERIF("Serif"),
    MONOSPACE("Monospace");

    val label: String get() = t(en)
}
/**
 * Sync's per-view-type display options (Settings > View type, sync-reference/settings-catalogue.md "View type").
 * Each layout reads only the options its Sync screen offers; the rest keep their defaults. Every default is how
 * Faden's layouts looked before the option existed, which is mostly Sync's default too; the fields whose default
 * differs from Sync's say so. Sync shares a few keys between List, Compact and Smaller cards (`type_indicators`,
 * `compact_single_lines`, `thumbnail_align`); here every layout has its own.
 */
@Serializable
data class ViewOptions(
    /** Slides: `slide_image_previews`, the big image/video/link preview. */
    val imagePreviews: Boolean = true,
    /** Slides and Cards: `slide_selftext_previews` / `card_selftext_previews`, a text post's first paragraph. */
    val selftextPreviews: Boolean = true,
    /** Slides: `slide_subreddit_thumbnail`, the community icon before "r/sub". */
    val subredditIcons: Boolean = true,
    /** Slides: `slide_selftext_border`, the outline around the text preview. */
    val selftextBorder: Boolean = true,
    /** Cards: `card_image_above`; off puts the preview under the title and meta. */
    val imageAboveTitle: Boolean = true,
    /** Small cards: `small_cards_thumbnail_align_left`; off keeps the thumbnail top-right. */
    val thumbnailLeft: Boolean = false,
    /** List and Compact: `compact_thumbnails`. */
    val thumbnails: Boolean = true,
    /** List, Compact and Smaller cards: `type_indicators`, the link/image/gallery/play icon on the right. */
    val typeIndicators: Boolean = true,
    /** List, Compact and Smaller cards: `compact_single_lines`, "Limit title to a single line". */
    val singleLineTitle: Boolean = false,
    /** List, Compact and Smaller cards: `thumbnail_align`, "Align thumbnail right"; the thumbnail goes to the row's end. */
    val thumbnailRight: Boolean = false,
    /**
     * Slides: `slide_image_full_height`, "Preview full height": the preview at the image's own aspect. Sync's default
     * is on; off keeps Faden's fixed 220 dp band.
     */
    val slideFullHeight: Boolean = false,
    /** Slides: `slide_full_width`, "Preview full width": the preview runs edge to edge, without margins or corners. */
    val slideFullWidth: Boolean = false,
    /** Slides: `slide_image_top`, "Preview before title": the preview opens the slide, above the subreddit and title. */
    val slideImageTop: Boolean = false,
    /**
     * Slides: `slide_thick_divider`, "Show a thick divider between posts": a band of the window's background between
     * slides. Off leaves a hairline instead.
     */
    val thickDivider: Boolean = true,
    /**
     * Cards: `card_full_width`, "Full width cards": no side gutter and square corners. As in Sync, it applies only
     * while the cards fall in a single column.
     */
    val cardFullWidth: Boolean = false,
    /**
     * Cards: `card_image_full_height`, "Full height". Sync's default is off; on keeps Faden's previews at the image's
     * own aspect, and off crops tall images to a 220 dp band.
     */
    val cardFullHeight: Boolean = true,
    /** Slides: `slide_buttons_save`, the bookmark button. */
    val saveButton: Boolean = true,
    /**
     * Cards and Small cards: `card_buttons_save` / `small_card_buttons_save`. Sync's default is on; off keeps Faden's
     * card footer (points, comments, ⋮). List, Compact and Smaller cards use it too, with Hide / Share / Mod tools, for
     * an optional button row under the post.
     */
    val cardSaveButton: Boolean = false,
    /**
     * Slides, Cards and Small cards: `slide_buttons_hide` / `card_buttons_hide` / `small_card_buttons_hide`, where the
     * list can hide posts.
     */
    val hideButton: Boolean = false,
    /**
     * Slides, Cards and Small cards: `slide_buttons_share` / `card_buttons_share` / `small_card_buttons_share`, sharing
     * the comments link.
     */
    val shareButton: Boolean = false,
    /**
     * Slides, Cards and Small cards: `slide_buttons_mod` / `card_buttons_mod` / `small_card_buttons_mod`, opening the
     * community's Moderator screen; shown only in communities you moderate. Sync's default is on; off keeps Faden's
     * button row as it was.
     */
    val modButton: Boolean = false,
    /** Slides: `slide_button_vote_right`, "Right aligned voting": the vote and comment pills move to the row's end. */
    val voteRight: Boolean = false,
    /** Slides: `slide_hide_all_buttons`, "Hide all buttons": no button row at all (⋮ stays on long press). */
    val hideAllButtons: Boolean = false,
    /**
     * Cards and Small cards: how many columns on a foldable's main (inner) screen, a tablet, or any window at least
     * [COVER_SCREEN_MAX_SW_DP] dp in its smallest width. [PostColumns.AUTO] keeps Faden's width-based tiling.
     * (The name predates [coverColumns], so settings saved with one override keep it for the big screen.)
     */
    val columns: PostColumns = PostColumns.AUTO,
    /** Cards and Small cards: the same for a foldable's cover screen, or any phone-sized window. */
    val coverColumns: PostColumns = PostColumns.AUTO,
    /**
     * Every layout: the size half of Sync's "Title font" picker (`slide_title`, `card_title`, `small_card_title`,
     * `smaller_card_title`, `compact_title`, `list_title`), reduced to three steps around the layout's own size.
     */
    val titleSize: TitleSize = TitleSize.NORMAL,
    /**
     * Every layout: the size half of Sync's "Description font" (`slide_description`, `card_description`,
     * `small_card_description`, `smaller_card_description`, `compact_description`, `list_description`), the meta line
     * under the title (and the points line in List, Compact and Smaller cards).
     */
    val descriptionSize: TitleSize = TitleSize.NORMAL,
    /** Slides and Cards: the size half of Sync's "Selftext font" (`slide_selftext`, `card_selftext`), the text preview. */
    val selftextSize: TitleSize = TitleSize.NORMAL,
    /**
     * Slides and Cards: this layout's own "Autoplay", overriding Data options › "Autoplay" (which [ViewAutoplay.DEFAULT]
     * follows). Not one of Sync's options.
     */
    val autoplay: ViewAutoplay = ViewAutoplay.DEFAULT,
) {
    companion object {
        /** The options of a layout nobody has customized, shared so reading them allocates nothing. */
        val DEFAULT = ViewOptions()
    }
}

/** A [ViewOptions.columns] choice; [count] null means Faden picks from the window width. */
enum class PostColumns(private val en: String, val count: Int?) {
    AUTO("Auto", null),
    ONE("1", 1),
    TWO("2", 2),
    THREE("3", 3),
    FOUR("4", 4);

    val label: String get() = t(en)
}

/**
 * Below this smallest width (dp) a window counts as a cover screen: phones and foldables' outer displays sit around
 * 320-420 dp, while unfolded inner screens and tablets are 600 dp or more whichever way they're held.
 */
const val COVER_SCREEN_MAX_SW_DP = 600

/** Whether a window whose smallest width is [smallestWidthDp] is a cover (phone-sized) screen. */
fun isCoverScreen(smallestWidthDp: Int): Boolean = smallestWidthDp < COVER_SCREEN_MAX_SW_DP

/** The [ViewOptions.columns] or [ViewOptions.coverColumns] choice for a cover screen or not. */
fun ViewOptions.columnsFor(coverScreen: Boolean): PostColumns = if (coverScreen) coverColumns else columns

/**
 * A Customize view font size ([ViewOptions.titleSize], [ViewOptions.descriptionSize], [ViewOptions.selftextSize]):
 * [scale] multiplies the layout's own size for that text. Named for the first of them; names are persisted.
 */
enum class TitleSize(private val en: String, val scale: Float) {
    SMALL("Small", 0.875f),
    NORMAL("Normal", 1f),
    LARGE("Large", 1.15f);

    val label: String get() = t(en)
}

enum class NsfwMode(private val en: String) {
    HIDE("Hide NSFW posts"), BLUR("Blur previews"), SHOW("Show");

    val label: String get() = t(en)
}

/**
 * Sync's "Periodically check for new messages". [minutes] null means no repeating check. WorkManager can't
 * repeat faster than every 15 minutes, so Sync's "Every 10 minutes" is 15 here.
 */
enum class MessageCheck(private val en: String, val minutes: Long?) {
    OFF("Off", null),
    MINUTES_15("Every 15 minutes", 15),
    MINUTES_30("Every 30 minutes", 30),
    HOUR("Every 60 minutes", 60),
    HOURS_3("Every 3 hours", 180),
    HOURS_6("Every 6 hours", 360),
    HOURS_12("Every 12 hours", 720),
    HOURS_24("Every 24 hours", 1440),
    ON_LAUNCH("On launch", null);

    val label: String get() = t(en)
}

/** Sync's "HD Reddit video quality": when v.redd.it videos open in HD rather than SD (the player's HD button switches). */
enum class VideoQuality(private val en: String) {
    ALWAYS("Always"), WIFI("WiFi only"), NEVER("Never");

    val label: String get() = t(en)
}

/**
 * Sync's "Autoplay" (Data options › Autoplay setup): videos and GIFs play, muted and looping, in the feed's card and
 * slide previews. [WIFI] goes by an unmetered connection; data saving stops it either way.
 */
enum class Autoplay(private val en: String) {
    ALWAYS("Always"), WIFI("WiFi only"), NEVER("Never");

    val label: String get() = t(en)
}

/**
 * One view's own "Autoplay" (Customize view for Cards and Slides, and Swipe mode): [DEFAULT] follows Data options ›
 * "Autoplay", the rest override it there, so autoplay can be on in one view only. Data saving still stops it.
 */
enum class ViewAutoplay(private val en: String, private val fixed: Autoplay?) {
    DEFAULT("As in Data options", null),
    ALWAYS("Always", Autoplay.ALWAYS),
    WIFI("WiFi only", Autoplay.WIFI),
    NEVER("Never", Autoplay.NEVER);

    /** What applies in this view, given Data options › "Autoplay". */
    fun resolve(global: Autoplay): Autoplay = fixed ?: global

    val label: String get() = t(en)
}

/**
 * Sync's "Data saving mode" (Data options): while it applies, feeds leave thumbnails and card previews unloaded
 * and Reddit videos open in SD. [MOBILE] is Sync's "Automagic": any metered connection.
 */
enum class DataSaving(private val en: String) {
    NEVER("Never"), MOBILE("On mobile data"), ALWAYS("Always");

    val label: String get() = t(en)
}

/** Sync's "Initial comment count": Reddit's `limit` for a post's first page of comments (500 is Reddit's ceiling). */
enum class CommentCount(private val en: String, val limit: Int) {
    C50("50", 50),
    C75("75", 75),
    C100("100", 100),
    C200("200", 200),
    ALL("All (up to 500)", 500);

    val label: String get() = t(en)
}

/**
 * Sync's "Live comments refresh rate" (`live_comments_refresh`): how often sports mode reloads the comments, with Sync's
 * nine choices. A reload still loading when the next is due is skipped, so even 2 seconds doesn't pile requests up.
 */
enum class LiveRefresh(private val en: String, val seconds: Int) {
    S2("Every 2 seconds", 2),
    S5("Every 5 seconds", 5),
    S10("Every 10 seconds", 10),
    S15("Every 15 seconds", 15),
    S20("Every 20 seconds", 20),
    S25("Every 25 seconds", 25),
    S30("Every 30 seconds", 30),
    S45("Every 45 seconds", 45),
    S60("Every 60 seconds", 60);

    val label: String get() = t(en)
}

/** Sync's "Post media preview size" for the image or video above a post's comments. */
enum class MediaPreviewSize(private val en: String) {
    CROPPED("Cropped height"), FULL("Full height");

    val label: String get() = t(en)
}

/** What a swipe stage does. Sync's post and comment swipe lists, merged; [posts]/[comments] say where each applies. */
enum class SwipeAction(private val en: String, val posts: Boolean, val comments: Boolean) {
    UPVOTE("Upvote", posts = true, comments = true),
    DOWNVOTE("Downvote", posts = true, comments = true),
    SAVE("Save", posts = true, comments = true),
    PROFILE("Profile", posts = true, comments = true),
    SHARE("Share", posts = true, comments = true),
    HIDE("Hide", posts = true, comments = false),
    MARK_READ("Mark as read", posts = true, comments = false),
    REPLY("Reply", posts = false, comments = true);

    val label: String get() = t(en)
}

@Serializable
data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    /** Faden's blue palette or Sync's Classic one; only matters while no [seedColor] is picked. */
    val palette: ThemePalette = ThemePalette.FADEN,
    /** A hand-picked accent (ARGB) the whole palette is generated from, instead of the [palette]. Ignored with Material You colors. */
    val seedColor: Int? = null,
    /** Sync's "extra colorful" tweak: backgrounds and cards take more of the accent's hue. */
    val extraColorful: Boolean = false,
    /**
     * Theme management › "Color intensity" (`monet_color_intensity`, 0–2 in 0.2 steps): scales the hand-picked
     * accent's saturation, so 0 is grey and 2 twice as vivid. Only applies with a [seedColor] (Sync's Manual mode).
     */
    val colorIntensity: Float = 1.0f,
    /** Sync's "Dark overlay" (`dark_overlay`): a black layer over the whole app, a night-reading dimmer. */
    val darkOverlay: Boolean = false,
    /** How dark [darkOverlay] makes the app: the black layer's alpha, [DARK_OVERLAY_MIN]–[DARK_OVERLAY_MAX]. */
    val darkOverlayLevel: Float = 0.4f,
    val layout: PostLayout = PostLayout.CARD,
    val nsfw: NsfwMode = NsfwMode.BLUR,
    val blurSpoilers: Boolean = true,
    /**
     * With [nsfw] on [NsfwMode.BLUR], blur NSFW previews only in the Frontpage, r/popular and r/all feeds; communities,
     * profiles, search and the rest show them as is.
     */
    val blurNsfwOnlyFrontpageAndAll: Boolean = false,
    /** Sync's "Hide read": posts you had already opened when a feed loaded are left out of it. */
    val hideRead: Boolean = false,
    val commentSort: CommentSort = CommentSort.BEST,
    val postMediaPreview: MediaPreviewSize = MediaPreviewSize.CROPPED,
    val swipeBack: Boolean = true,
    /** Sync's Tablet mode: on wide screens, a navigation rail replaces the bottom bar. */
    val tabletMode: Boolean = false,
    /**
     * Tablet › "Navigation on the right": on wide screens (a foldable's inner screen, a tablet) the Posts / Explore /
     * Inbox bar becomes a rail down the right edge, within reach of the thumb. Works with or without Tablet mode.
     */
    val navRailRight: Boolean = false,
    /** Tablet › "Dual pane (landscape)" (`tablet_dual_pane_landscape`): a post opens beside its feed in landscape. */
    val dualPaneLandscape: Boolean = true,
    /** Tablet › "Dual pane (portrait)" (`tablet_dual_pane_portrait`): the same in portrait, on wide enough windows. */
    val dualPanePortrait: Boolean = false,
    /**
     * Tablet › "Tabbed comments" (`tablet_tabs`): with Tablet mode on, a post page splits into Post and Comments tabs
     * instead of one scroll. Sync's default is on; off keeps Faden's single scroll.
     */
    val tabbedComments: Boolean = false,
    /**
     * General › "Confirm exit" (`exit_preference`): back on the last page asks before closing the app, as Sync does by
     * default. The dialog's "Don't ask again" turns it off.
     */
    val confirmExit: Boolean = true,
    /** Theme management's saved themes, in the order they were saved. */
    val savedThemes: List<SavedTheme> = emptyList(),
    /**
     * Sync's "Use drawer navigation" (`use_drawer`): home pages lose the bottom bar and the toolbar avatar
     * opens a side drawer instead. Tablet mode's rail still wins on wide screens.
     */
    val drawerNavigation: Boolean = false,
    val baseFontSize: BaseFontSize = BaseFontSize.SMALLER,
    val typeface: Typeface = Typeface.FIGTREE,
    /** General › "Language": the app's text, applied at launch and when changed (see [io.github.radixtrator.resync.i18n.I18n]). */
    val language: io.github.radixtrator.resync.i18n.AppLanguage = io.github.radixtrator.resync.i18n.AppLanguage.SYSTEM,
    /** Sync's "Action visibility": labels of More actions buttons switched off. */
    val hiddenActions: Set<String> = emptySet(),
    val inAppBrowser: Boolean = true,
    val muteVideos: Boolean = false,
    val hdVideo: VideoQuality = VideoQuality.NEVER,
    /** Sync's comment navigation bar: previous/next top-level comment buttons. Off by default, as in Sync. */
    val commentNavigation: Boolean = false,
    /**
     * Comment options › "Volume key navigation" (Sync's `comments_volume_nav`): on a post page, volume down jumps to the
     * next top-level comment and volume up to the previous one, instead of changing the volume. Off by default, as in Sync.
     */
    val volumeKeyNavigation: Boolean = false,
    /** Reopening a post returns to where you were, with the same threads folded. */
    val rememberCommentPosition: Boolean = true,
    /** Footnote numbers on off-Reddit links, with a card per link under the text. */
    val linkCards: Boolean = true,
    /** GIFs in comments (Giphy picks, uploads, links to GIFs) play under the comment's text. */
    val commentGifs: Boolean = true,
    /** Sync's "Show profile pictures": authors' avatars in comment headers instead of their initial. */
    val profilePictures: Boolean = true,
    /** A "New" label on comments posted since you last opened the thread. */
    val markNewComments: Boolean = true,
    /** Off: hold a comment to collapse it, tap it for its action bar. On: tap collapses, hold opens the bar. */
    val tapToCollapse: Boolean = false,
    /** Ask GitHub now and then whether a newer release is out, and say so. */
    val checkForUpdates: Boolean = true,
    val messageCheck: MessageCheck = MessageCheck.HOUR,
    /** Sync's extended swipe: drag left, and each further step arms the next of these three. */
    val postSwipe: Boolean = true,
    val postSwipeActions: List<SwipeAction> = DEFAULT_POST_SWIPES,
    val commentSwipe: Boolean = true,
    val commentSwipeActions: List<SwipeAction> = DEFAULT_COMMENT_SWIPES,
    /** Post options › Flair: flair chips on posts in feeds (Sync's "Hide post flair" is this off). */
    val showPostFlair: Boolean = true,
    /** Post options › "Show post author" in feed items' meta line. */
    val showPostAuthor: Boolean = true,
    /** Post options › "Display short scores": 10.6k rather than 10,600 in feeds. */
    val shortScores: Boolean = true,
    /** History › "Mark posts as read on scroll": posts scrolled past in a feed count as read. */
    val markReadOnScroll: Boolean = false,
    /** Comments › "Hide AutoMod comments", replies included. */
    val hideAutoMod: Boolean = false,
    /** Comments › "Show user flair" in comment headers. */
    val commentFlair: Boolean = true,
    /** Comment options › "Collapse all threads by default" on a post's first open. */
    val collapseAllByDefault: Boolean = false,
    /** Comment options › "Initial comment count". 200 is what Faden always asked for (Sync's default is 75). */
    val commentCount: CommentCount = CommentCount.C200,
    /** Comments › "Live comments refresh rate": sports mode's reload interval. 15 s is Sync's default. */
    val liveRefresh: LiveRefresh = LiveRefresh.S15,
    /** The comment reader's ⋮ › "Read replies": read every loaded comment, not just the top-level ones. */
    val readerReplies: Boolean = false,
    /** The comment reader's ⋮ speed: TextToSpeech's speech rate, 1.0 being the engine's normal pace. */
    val readerSpeechRate: Float = 1.0f,
    /** Settings › search: the last searches a result was picked from, newest first ([rememberSettingsSearch]). */
    val recentSettingsSearches: List<String> = emptyList(),
    /**
     * Media › "Close images by": a single tap (true; swiping up then slides in the control bar, see [holdForBar]) or
     * swiping up or down (false; a tap then shows and hides the bar).
     */
    val tapToClose: Boolean = true,
    /**
     * Media › "Close images by": "Single tap, hold for the bar", with [tapToClose]. A tap, Back and, with [swipeToClose],
     * a fling up or down close the image whether the bar is showing or not, and holding the picture shows and hides
     * the bar.
     */
    val holdForBar: Boolean = false,
    /**
     * Media › "Close videos by": a single tap (true; so do Back and, with [swipeToClose], a fling up or down, and holding
     * the video hides and shows the controls) or swiping up or down (false, the default; a tap shows and hides the controls).
     */
    val tapToCloseVideos: Boolean = false,
    /**
     * Media › "Swipe to close": videos with a fling up or down, and images that close by a tap with a downward one (up
     * or down with [holdForBar]).
     */
    val swipeToClose: Boolean = true,
    /** Media › "Always show download button": the image viewer keeps a download button on screen, not only in its bar. */
    val imageDownloadButton: Boolean = false,
    /** Media › "Hold to preview": holding a post's picture in a feed shows it full screen until the finger lifts. */
    val holdToPreview: Boolean = true,
    /** Media › "Unblur NSFW previews": with [holdToPreview], holding a blurred NSFW picture shows it unblurred. */
    val peekUnblursNsfw: Boolean = false,
    /** Media › Video / GIF playback: "Display seek bar". */
    val videoSeekBar: Boolean = true,
    /** Media › Video / GIF playback: "Display speed controls" (in the player's ⋮ panel). */
    val videoSpeedControls: Boolean = true,
    /** Data options › "Data saving mode". Never is how Faden always behaved (Sync's default is Automagic). */
    val dataSaving: DataSaving = DataSaving.NEVER,
    /** Data options › "Autoplay". Never is how Faden always behaved (Sync's default is WiFi only). */
    val autoplay: Autoplay = Autoplay.NEVER,
    /** Customize view › Swipe mode "Autoplay": Swipe mode's own override of [autoplay]. */
    val swipeAutoplay: ViewAutoplay = ViewAutoplay.DEFAULT,
    /** Per-feed overrides of [layout], keyed by [feedKey], like Sync's per-subreddit view. */
    val feedLayouts: Map<String, PostLayout> = emptyMap(),
    /** Sync's Customize view choices, per layout; a layout missing here uses the [ViewOptions] defaults. */
    val viewOptions: Map<PostLayout, ViewOptions> = emptyMap(),
    /** Sorts saved from the sort sheet ("Always use Hot here?"), keyed by [feedKey]; other feeds open on Reddit's default. */
    val feedSorts: Map<String, Sort> = emptyMap(),
    /** Whether picking a sort asks to keep it for the feed. Turned off after the first answer; Post options turns it back on. */
    val askToKeepSort: Boolean = true,
) {
    fun layoutFor(subreddit: String?): PostLayout = feedLayouts[feedKey(subreddit)] ?: layout

    fun sortFor(subreddit: String?): Sort? = feedSorts[feedKey(subreddit)]

    fun viewOptionsFor(layout: PostLayout): ViewOptions = viewOptions[layout] ?: ViewOptions.DEFAULT

    /**
     * Whether Tablet mode's dual pane shows in a window [widthDp] wide, held [landscape] or not: Sync's per-orientation
     * "Dual pane" switches, as long as the window fits two panes ([DUAL_PANE_MIN_WIDTH_DP]).
     */
    fun dualPane(landscape: Boolean, widthDp: Int): Boolean =
        tabletMode && (if (landscape) dualPaneLandscape else dualPanePortrait) && widthDp >= DUAL_PANE_MIN_WIDTH_DP

    /** The three post swipe stages, repairing a stored list that isn't three valid entries. */
    val postSwipeStages: List<SwipeAction>
        get() = postSwipeActions.takeIf { l -> l.size == 3 && l.all { it.posts } } ?: DEFAULT_POST_SWIPES

    val commentSwipeStages: List<SwipeAction>
        get() = commentSwipeActions.takeIf { l -> l.size == 3 && l.all { it.comments } } ?: DEFAULT_COMMENT_SWIPES

    companion object {
        // Sync's defaults: posts upvote, downvote, hide; comments upvote, save, reply.
        val DEFAULT_POST_SWIPES = listOf(SwipeAction.UPVOTE, SwipeAction.DOWNVOTE, SwipeAction.HIDE)
        val DEFAULT_COMMENT_SWIPES = listOf(SwipeAction.UPVOTE, SwipeAction.SAVE, SwipeAction.REPLY)

        /** Front page is ""; subreddits and multis by lowercase name. */
        fun feedKey(subreddit: String?): String = subreddit?.lowercase().orEmpty()

        /**
         * The narrowest window (dp) that gets the dual pane: two 360 dp panes, the feed's minimum and a phone-sized
         * post. Most portrait tablets (600-839 dp) clear it, for "Dual pane (portrait)"; phones only do held sideways.
         */
        const val DUAL_PANE_MIN_WIDTH_DP = 720

        const val DARK_OVERLAY_MIN = 0.1f
        const val DARK_OVERLAY_MAX = 0.7f

        /** Sync's Color intensity slider range. */
        const val COLOR_INTENSITY_MAX = 2.0f
    }
}

/** Persists [Settings] as one JSON blob; unknown/removed fields are ignored on load. */
/** The saved base font scale, read before the container exists (MainActivity applies it in attachBaseContext). */
fun savedBaseFontScale(prefs: SharedPreferences): Float =
    prefs.getString("settings.v1", null)
        ?.let { runCatching { Json { ignoreUnknownKeys = true }.decodeFromString(Settings.serializer(), it).baseFontSize.scale }.getOrNull() }
        ?: BaseFontSize.SMALLER.scale

class SettingsStore(private val prefs: SharedPreferences) {
    private val json = Json { ignoreUnknownKeys = true }
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    fun update(change: (Settings) -> Settings) {
        val next = change(_settings.value)
        _settings.value = next
        prefs.edit { putString(KEY, json.encodeToString(Settings.serializer(), next)) }
    }

    private fun load(): Settings = prefs.getString(KEY, null)
        ?.let { runCatching { json.decodeFromString(Settings.serializer(), it) }.getOrNull() }
        ?: Settings()

    private companion object {
        const val KEY = "settings.v1"
    }
}

/** How many settings searches [Settings.recentSettingsSearches] keeps. */
const val RECENT_SETTINGS_SEARCHES = 5

/** [query] as the newest recent settings search: trimmed, once (whatever its case), at most [RECENT_SETTINGS_SEARCHES]. */
fun Settings.rememberSettingsSearch(query: String): Settings {
    val q = query.trim()
    if (q.isEmpty()) return this
    val recents = (listOf(q) + recentSettingsSearches.filterNot { it.equals(q, ignoreCase = true) }).take(RECENT_SETTINGS_SEARCHES)
    return copy(recentSettingsSearches = recents)
}
