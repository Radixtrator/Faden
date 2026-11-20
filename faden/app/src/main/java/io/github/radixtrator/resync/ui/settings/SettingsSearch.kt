package io.github.radixtrator.resync.ui.settings

import androidx.navigation3.runtime.NavKey
import io.github.radixtrator.resync.data.FilterType
import io.github.radixtrator.resync.data.PostLayout
import io.github.radixtrator.resync.i18n.t
import io.github.radixtrator.resync.ui.navigation.CustomizeViewRoute
import io.github.radixtrator.resync.ui.navigation.FilterEditorRoute
import io.github.radixtrator.resync.ui.navigation.SettingsRoute
import io.github.radixtrator.resync.ui.navigation.SetupRoute
import io.github.radixtrator.resync.ui.navigation.ThemeManagementRoute

/**
 * One row the settings search can find: its [title] and [summary] as the row shows them, [place] for where it
 * lives ("Comment options"), and [open], the page it's on. [keywords] are other words people search for it by.
 */
data class SettingsEntry(
    val title: String,
    val summary: String?,
    val place: String,
    val open: (currentLayout: PostLayout) -> NavKey,
    val keywords: String = "",
)

/**
 * Every row on the settings pages, for searching them. The pages compose their rows directly, so this list is
 * kept beside them by hand: a row added to a page belongs here too.
 */
object SettingsIndex {
    private fun row(category: SettingsCategory, title: String, summary: String? = null, keywords: String = "") =
        SettingsEntry(title, summary, category.title, { SettingsRoute(category) }, keywords)

    private fun theme(title: String, summary: String? = null, keywords: String = "") =
        SettingsEntry(title, summary, t("Theme management"), { ThemeManagementRoute }, keywords)

    // Every view setting is on Customize view, which is the View type category's page.
    private fun view(title: String, summary: String? = null, keywords: String = "") =
        SettingsEntry(title, summary, t("View type"), { CustomizeViewRoute(it) }, "layout card list post ansicht karte liste pfosten customize anpassen $keywords")

    /** Plain words for each category, in English and everyday German (the German titles are playful). */
    private val SettingsCategory.keywords: String
        get() = when (this) {
            SettingsCategory.GENERAL -> "general allgemein"
            SettingsCategory.TABLET -> "foldable large screen faltbar großer bildschirm"
            SettingsCategory.THEME -> "colors design look farben aussehen"
            SettingsCategory.DARK_MODE -> "dark night light dunkel nacht hell"
            SettingsCategory.VIEW_TYPE -> "layout cards list ansicht darstellung karten liste"
            SettingsCategory.COMMENTS -> "comments look kommentare aussehen"
            SettingsCategory.POSTS -> "posts beiträge"
            SettingsCategory.COMMENT_OPTIONS -> "comments sort kommentare sortierung"
            SettingsCategory.MESSAGING -> "messages notifications inbox nachrichten benachrichtigungen posteingang"
            SettingsCategory.MEDIA -> "images videos gifs bilder medien"
            SettingsCategory.LINKS -> "links browser"
            SettingsCategory.HISTORY -> "read history gelesen verlauf"
            SettingsCategory.DATA -> "mobile data datenvolumen daten"
            SettingsCategory.FILTERS -> "filter hide block ausblenden blockieren"
            SettingsCategory.BACKUP -> "export import sicherung"
            SettingsCategory.OTHER -> "about info api über"
        }

    /** Built on each read, so titles and summaries follow the current language. */
    val entries: List<SettingsEntry>
        get() =
            // The categories themselves, so "theme" or "backup" finds the page too.
            SettingsCategory.entries.map { category -> SettingsEntry(category.title, category.summary, t("Settings"), { layout -> category.route(layout) }, category.keywords) } + listOf(
                row(SettingsCategory.GENERAL, t("Language"), keywords = "language deutsch german english sprache locale translation übersetzung"),
                row(SettingsCategory.GENERAL, t("Drawer navigation"), t("A side drawer, opened from your avatar, replaces the bottom bar"), "menu sidebar menü seitenleiste hamburger bottom bar navigation leiste unten"),
                row(SettingsCategory.GENERAL, t("Confirm exit"), t("Ask before back closes the app"), "quit close beenden schließen exit leave verlassen bestätigen nachfragen"),
                row(SettingsCategory.GENERAL, t("Slide to go back"), t("Drag a page to the right to close it"), "gesture swipe back geste wischen fingerfegen zurück back zurückwischen"),
                row(SettingsCategory.GENERAL, t("Typeface"), keywords = "font schrift schriftart font family schriftfamilie"),
                row(SettingsCategory.GENERAL, t("Base font size"), keywords = "text size bigger smaller textgröße größer kleiner font zoom accessibility schrift größe lesbarkeit barrierefrei"),

                row(SettingsCategory.TABLET, t("Tablet mode"), t("On wide screens, a navigation rail replaces the bottom bar"), "foldable faltbar klapphandy tablet large screen großer bildschirm"),
                row(SettingsCategory.TABLET, t("Navigation on the right"), t("Move the bottom bar to a rail on the right edge, for one-handed use"), "foldable rail faltbar leiste one-handed left right einhändig links rechts"),
                row(SettingsCategory.TABLET, t("Dual pane (landscape)"), t("With Tablet mode on: comments next to posts in landscape"), "split two geteilt zwei landscape columns querformat spalten"),
                row(SettingsCategory.TABLET, t("Dual pane (portrait)"), t("With Tablet mode on: the same in portrait, on wide screens"), "split two geteilt zwei portrait columns hochformat spalten"),
                row(SettingsCategory.TABLET, t("Tabbed comments"), t("With Tablet mode on: a post and its comments as tabs"), keywords = "tabs reiter"),

                row(SettingsCategory.THEME, t("Material You colors"), t("Use your wallpaper's colors"), "dynamic color wallpaper dynamisch farbe hintergrundbild monet system colors systemfarben"),
                row(SettingsCategory.THEME, t("Theme color"), keywords = "accent color akzent farbe primary hauptfarbe"),
                row(SettingsCategory.THEME, t("Extra colorful"), t("Tint backgrounds and cards with the accent too"), keywords = "tint background bunt farbig hintergrund"),
                theme(t("Saved themes"), t("Preview, save, share and load themes"), keywords = "design themes designs speichern"),
                theme(t("Color intensity"), keywords = "accent akzent saturation intensität sättigung"),
                theme(t("Save current theme")),
                theme(t("View palette"), t("The colors the theme in use is built from")),
                theme(t("Share theme"), t("Send the theme you're using as a code to paste")),
                theme(t("Load theme"), t("Paste a shared theme code"), "import importieren"),

                row(SettingsCategory.DARK_MODE, t("Theme"), t("Light, dark, black or follow the system"), "night amoled nacht schwarz dark light auto system dunkel hell automatisch dunkelmodus"),
                row(SettingsCategory.DARK_MODE, t("Dark overlay"), t("Dim the whole app, for reading at night"), "dim brightness abdunkeln helligkeit night filter nachtfilter dunkler"),
                row(SettingsCategory.DARK_MODE, t("Overlay darkness"), t("With Dark overlay on"), "dim brightness abdunkeln helligkeit"),

                view(t("Default post layout"), keywords = "card list compact slides karte liste kompakt folien view ansicht darstellung"),
                view(t("Post media preview size"), keywords = "image cropped bild zugeschnitten thumbnail preview vorschau größe miniatur"),
                view(t("Autoplay"), t("Per view: Cards, Slides and Swipe mode"), keywords = "video gif play abspielen swipe fingerfeger wifi wlan"),

                row(SettingsCategory.COMMENTS, t("Show user flair"), keywords = "badge tag abzeichen nutzer"),
                row(SettingsCategory.COMMENTS, t("Show profile pictures"), t("Authors' avatars in comment headers"), "avatar profilbild"),
                row(SettingsCategory.COMMENTS, t("Mark new comments"), t("Label comments posted since you last opened the thread"), "unread label ungelesen markierung highlight new neu hervorheben"),
                row(SettingsCategory.COMMENTS, t("Hide AutoMod comments"), t("And the replies to them"), "bot automoderator truggenosse moderator ausblenden"),
                row(SettingsCategory.COMMENTS, t("Live comments refresh rate"), keywords = "sports mode sportmodus live refresh interval auto-refresh aktualisieren intervall"),

                row(SettingsCategory.POSTS, t("NSFW posts"), keywords = "blur adult verwischen nsfa erwachsene 18+ explicit porn anstößig ab 18 unscharf"),
                row(SettingsCategory.POSTS, t("Blur NSFW only on Frontpage, Popular & All"), t("With NSFW posts set to blur"), keywords = "nsfw blur unscharf verwischen"),
                row(SettingsCategory.POSTS, t("Blur spoiler previews"), keywords = "blur unscharf verwischen"),
                row(SettingsCategory.POSTS, t("Show post flair"), t("Flair chips on posts in feeds"), keywords = "badge tag abzeichen"),
                row(SettingsCategory.POSTS, t("Show post author"), t("In feeds"), keywords = "username author autor nutzername"),
                row(SettingsCategory.POSTS, t("Ask to keep a new sort"), t("When you change a feed's sort, ask whether to always open it that way"), "always default hot new top immer standard heiß neu spitze sortierung sort sortieren"),
                row(SettingsCategory.POSTS, t("Display short scores"), t("10.6k instead of 10,600"), "points karma punkte upvotes votes abkürzen stimmen"),
                row(SettingsCategory.POSTS, t("Swipe posts"), t("Drag a post left; drag further for the next action"), "gesture geste wischen fingerfeger swipe actions upvote save"),
                row(SettingsCategory.POSTS, t("Post swipe actions"), t("First, second and third swipe action"), "gesture geste wischen fingerfeger"),

                row(SettingsCategory.COMMENT_OPTIONS, t("Default comment sort"), keywords = "best top new beste spitze neu sortierung sort order reihenfolge sortieren"),
                row(SettingsCategory.COMMENT_OPTIONS, t("Collapse all threads by default"), t("The first time you open a post"), "fold einklappen falten collapse threads zuklappen"),
                row(SettingsCategory.COMMENT_OPTIONS, t("Initial comment count"), keywords = "load laden anzahl number how many zahl wie viele"),
                row(SettingsCategory.COMMENT_OPTIONS, t("Tap to collapse"), t("Tap a comment to collapse it and hold it for options, instead of the other way round"), "hold long press fold halten lang drücken einklappen"),
                row(SettingsCategory.COMMENT_OPTIONS, t("Comment navigation buttons"), t("Jump between top-level comments"), "next previous nächster vorheriger jump arrows springen pfeile"),
                row(SettingsCategory.COMMENT_OPTIONS, t("Volume key navigation"), t("Volume buttons jump between top-level comments"), "volume lautstärke taste button knopf next previous nächster vorheriger"),
                row(SettingsCategory.COMMENT_OPTIONS, t("Remember comment position"), t("Reopen posts where you left off, threads folded as you left them"), "scroll scrollen position resume bookmark fortsetzen merken lesezeichen"),
                row(SettingsCategory.COMMENT_OPTIONS, t("Swipe comments"), t("Drag a comment left; drag further for the next action"), "gesture geste wischen fingerfeger"),
                row(SettingsCategory.COMMENT_OPTIONS, t("Comment swipe actions"), t("First, second and third swipe action"), "gesture geste wischen fingerfeger"),

                row(SettingsCategory.MESSAGING, t("Periodically check for new messages"), keywords = "inbox notifications innenschachtel benachrichtigungen poll interval messages nachrichten abrufen"),
                row(SettingsCategory.MESSAGING, t("Check now"), t("Look for new messages, posts and comments now"), "inbox notifications innenschachtel benachrichtigungen"),
                row(SettingsCategory.MESSAGING, t("Watched communities"), t("A notification for each new post"), "notifications subreddit benachrichtigungen unterlases follow abonnieren beobachten"),
                row(SettingsCategory.MESSAGING, t("Watched posts"), t("New comments are checked every hour"), "notifications benachrichtigungen follow beobachten folgen"),

                row(SettingsCategory.MEDIA, t("Close images by"), t("Single tap, or swiping up or down"), "viewer betrachter bildansicht close tap swipe hold bar schließen tippen wischen halten leiste"),
                row(SettingsCategory.MEDIA, t("Close videos by"), t("Swiping up or down, or a single tap"), "player video tap tippen schließen"),
                row(SettingsCategory.MEDIA, t("Swipe to close"), t("Fling videos and images to close them"), "viewer betrachter bildansicht fling close schnippen schließen"),
                row(SettingsCategory.MEDIA, t("Always show download button"), t("Keep a download button on screen while viewing an image"), "save speichern herunterladen download viewer betrachter bildansicht"),
                row(SettingsCategory.MEDIA, t("Hold to preview"), t("Hold a picture in a feed to see it full screen until you let go"), "peek vorschau long press lange drücken halten"),
                row(SettingsCategory.MEDIA, t("Unblur NSFW previews"), t("With Hold to preview on: holding a blurred NSFW picture shows it unblurred"), "peek vorschau"),
                row(SettingsCategory.MEDIA, t("Start videos muted"), keywords = "sound audio ton stumm mute silent lautlos"),
                row(SettingsCategory.MEDIA, t("Display seek bar"), keywords = "video progress scrub fortschritt spulen"),
                row(SettingsCategory.MEDIA, t("Display speed controls"), keywords = "video playback wiedergabe geschwindigkeit speed tempo schneller langsamer"),

                row(SettingsCategory.LINKS, t("Open links in the app"), t("Custom Tabs instead of your browser app"), "browser chrome brauser custom tabs in-app webview"),
                row(SettingsCategory.LINKS, t("Link cards"), t("Number links and list them under posts and comments"), keywords = "links karten nummern"),
                row(SettingsCategory.LINKS, t("Pictures in comments"), t("Show GIFs and images under the comments that post or link them, instead of their links"), "gif image photo inline preview bild foto vorschau"),
                row(SettingsCategory.LINKS, t("Open Reddit links in Faden"), t("Links to reddit.com and redd.it from other apps open here"), "default supported web standard unterstützt lases deep link open with öffnen mit"),

                row(SettingsCategory.HISTORY, t("Mark posts as read on scroll"), t("Posts you scroll past in a feed count as read"), keywords = "read history gelesen verlauf scrollen"),
                row(SettingsCategory.HISTORY, t("Hide read posts"), t("Leave out posts you have already opened when a feed loads"), keywords = "read history gelesen verlauf ausblenden verstecken"),

                row(SettingsCategory.DATA, t("Data saving mode"), keywords = "mobile metered mobil getaktet datensparen data saver datenvolumen sparen"),
                row(SettingsCategory.DATA, t("HD Reddit video quality"), keywords = "wifi wlan quality resolution qualität auflösung 720p 1080p"),
                row(SettingsCategory.DATA, t("Autoplay"), t("Videos and GIFs play muted in Cards, Slides and Swipe mode, one at a time, unless data saving is on. Customize view can override it per view"), "wifi wlan feed futter play abspielen gif video"),

                row(SettingsCategory.BACKUP, t("Backup now"), t("Save a backup file wherever you choose"), "export exportieren rückhoch sicherung"),
                row(SettingsCategory.BACKUP, t("Restore from backup"), t("Replace this device's data with a backup file"), "import importieren rückhoch wiederherstellen"),

                SettingsEntry(t("Reddit API"), t("Client ID, redirect URI, user agent"), SettingsCategory.OTHER.title, { SetupRoute }, "login key anmelden schlüssel"),
                row(SettingsCategory.OTHER, t("Version"), keywords = "about über info versionsnummer"),
                row(SettingsCategory.OTHER, t("What's new"), t("What changed in each version"), "changelog release notes update änderungen neuigkeiten aufdatum"),
                row(SettingsCategory.OTHER, t("Check for updates"), t("Say when a new version is out on GitHub"), "release version aufdatum aktualisierung update new version neue version"),
                row(SettingsCategory.OTHER, t("Support Faden"), t("Donate on Ko-fi. Faden stays free either way"), "donate donation tip kofi ko-fi coffee spende spenden kaffee unterstützen"),
            ) +
                FilterType.entries.map { type -> SettingsEntry(type.title, type.summary, SettingsCategory.FILTERS.title, { FilterEditorRoute(type) }, "hide block ausblenden blockieren") } +
                ViewOption.entries.distinctBy { it.title }.map { view(it.title, it.summary, it.keywords) } +
                ViewFontSize.entries.map { view(it.title) } +
                listOf(view(t("Columns on main screen")), view(t("Columns on cover screen")))

    /** Plain words for the Customize view switches, in English and everyday German. */
    private val ViewOption.keywords: String
        get() = when (this) {
            ViewOption.IMAGE_PREVIEWS, ViewOption.SLIDE_FULL_HEIGHT, ViewOption.SLIDE_FULL_WIDTH, ViewOption.SLIDE_IMAGE_TOP ->
                "picture image bild vorschau"
            ViewOption.SELFTEXT_PREVIEWS, ViewOption.SELFTEXT_BORDER -> "text body inhalt rahmen border"
            ViewOption.SUBREDDIT_ICONS -> "community icon symbol"
            ViewOption.IMAGE_ABOVE_TITLE -> "picture order bild reihenfolge oben"
            ViewOption.THUMBNAIL_LEFT, ViewOption.THUMBNAIL_RIGHT, ViewOption.THUMBNAILS -> "thumbnail miniatur vorschaubild"
            ViewOption.TYPE_INDICATORS -> "icon badge symbol"
            ViewOption.SINGLE_LINE_TITLE -> "one line einzeilig kürzen"
            ViewOption.THICK_DIVIDER -> "separator line trennlinie trenner"
            ViewOption.CARD_FULL_WIDTH, ViewOption.CARD_FULL_HEIGHT -> "card size karte größe breite höhe"
            ViewOption.SAVE_BUTTON, ViewOption.CARD_SAVE_BUTTON, ViewOption.HIDE_BUTTON, ViewOption.SHARE_BUTTON,
            ViewOption.MOD_BUTTON, ViewOption.HIDE_ALL_BUTTONS -> "button action knopf aktion"
            ViewOption.VOTE_RIGHT -> "vote arrows upvote abstimmen pfeile"
        }

    /**
     * The entries matching every word of [query], anywhere in their title, summary, place or keywords; ones whose
     * title has the words come first, then ones whose title starts with the query.
     */
    fun search(query: String, from: List<SettingsEntry> = entries): List<SettingsEntry> {
        val words = query.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()
        val q = query.trim().lowercase()
        return from
            .filter { e ->
                val haystack = listOfNotNull(e.title, e.summary, e.place, e.keywords).joinToString(" ").lowercase()
                words.all { it in haystack }
            }
            .sortedWith(
                compareBy<SettingsEntry> { e -> if (words.all { it in e.title.lowercase() }) 0 else 1 }
                    .thenBy { e -> if (e.title.lowercase().startsWith(q)) 0 else 1 },
            )
    }
}
