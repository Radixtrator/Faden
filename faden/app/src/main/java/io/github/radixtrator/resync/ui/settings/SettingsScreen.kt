@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package io.github.radixtrator.resync.ui.settings

import io.github.radixtrator.resync.ui.common.Symbols
import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import io.github.radixtrator.resync.ui.theme.THEME_SEEDS
import io.github.radixtrator.resync.ui.theme.ToneScheme
import io.github.radixtrator.resync.data.ThemePalette
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.radixtrator.resync.AppContainer
import io.github.radixtrator.resync.BuildConfig
import io.github.radixtrator.resync.data.Backup
import io.github.radixtrator.resync.data.BackupDocument
import io.github.radixtrator.resync.data.BackupFormatException
import io.github.radixtrator.resync.data.BackupPart
import io.github.radixtrator.resync.data.BaseFontSize
import io.github.radixtrator.resync.data.CommentCount
import io.github.radixtrator.resync.data.CommentSort
import io.github.radixtrator.resync.data.DataSaving
import io.github.radixtrator.resync.data.FilterType
import io.github.radixtrator.resync.data.Typeface
import io.github.radixtrator.resync.data.LiveRefresh
import io.github.radixtrator.resync.data.MessageCheck
import io.github.radixtrator.resync.data.NsfwMode
import io.github.radixtrator.resync.data.PostLayout
import io.github.radixtrator.resync.data.SwipeAction
import io.github.radixtrator.resync.data.ThemeMode
import io.github.radixtrator.resync.data.VideoQuality
import io.github.radixtrator.resync.data.Autoplay
import io.github.radixtrator.resync.notify.InboxWorker
import io.github.radixtrator.resync.notify.Notifications
import io.github.radixtrator.resync.ui.common.SwipeBack
import io.github.radixtrator.resync.ui.home.WhatsNewDialog
import io.github.radixtrator.resync.ui.navigation.CustomizeViewRoute
import androidx.navigation3.runtime.NavKey
import io.github.radixtrator.resync.ui.navigation.FilterEditorRoute
import io.github.radixtrator.resync.ui.navigation.LocalNavigator
import io.github.radixtrator.resync.ui.navigation.SetupRoute
import io.github.radixtrator.resync.ui.navigation.SettingsRoute
import io.github.radixtrator.resync.data.Settings
import io.github.radixtrator.resync.i18n.AppLanguage
import io.github.radixtrator.resync.i18n.I18n
import io.github.radixtrator.resync.i18n.t
import io.github.radixtrator.resync.ui.common.AppIcons
import io.github.radixtrator.resync.ui.navigation.ThemeManagementRoute
import io.github.radixtrator.resync.watch.WatchWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Sync's settings categories, grouped as on its root screen. Each opens its own page; [summary] is the root
 * screen's line under the title.
 */
enum class SettingsCategory(private val enGroup: String, private val enTitle: String, private val enSummary: String) {
    GENERAL("Appearance", "General", "Navigation, gestures, fonts & quick actions"),
    TABLET("Appearance", "Tablet", "Navigation rail, dual pane & tabbed comments"),
    THEME("Appearance", "Theme management", "Colors, saved themes & sharing"),
    DARK_MODE("Appearance", "Dark mode", "Light, dark or auto, and the dark overlay"),
    VIEW_TYPE("Appearance", "View type", "Customize how posts look"),
    COMMENTS("Appearance", "Comments", "Customize how comments look"),
    POSTS("Content", "Post options", "NSFW, flair, scores, swipe actions & more"),
    COMMENT_OPTIONS("Content", "Comment options", "Sorting, navigation, swipe actions & more"),
    MESSAGING("Content", "Messaging", "Message checks, watched communities & posts"),
    MEDIA("Content", "Media", "Closing the viewer & video playback"),
    LINKS("Content", "Link handling", "In-app browser & link cards"),
    HISTORY("Content", "History", "Marking as read & hiding read posts"),
    DATA("General", "Data options", "Data saving & video quality"),
    FILTERS("General", "Filters", "Post filters"),
    BACKUP("General", "Backup", "Backup & restore"),
    OTHER("Other", "Everything else", "Reddit API setup & app info");

    val group: String get() = t(enGroup)
    val title: String get() = t(enTitle)
    val summary: String get() = t(enSummary)

    /** The page this category opens: its own, except View type, whose settings all live on Customize view. */
    fun route(currentLayout: PostLayout): NavKey =
        if (this == VIEW_TYPE) CustomizeViewRoute(currentLayout) else SettingsRoute(this)
}

/** Everything else › Support Faden. */
private const val DONATE_URL = "https://ko-fi.com/radixtrator"

/** The glyph beside each category on the root screen. */
private val SettingsCategory.icon: ImageVector
    get() = when (this) {
        SettingsCategory.GENERAL -> Symbols.Settings
        SettingsCategory.TABLET -> AppIcons.Tablet
        SettingsCategory.THEME -> AppIcons.Palette
        SettingsCategory.DARK_MODE -> AppIcons.Moon
        SettingsCategory.VIEW_TYPE -> AppIcons.ViewStack
        SettingsCategory.COMMENTS -> AppIcons.Comment
        SettingsCategory.POSTS -> AppIcons.Text
        SettingsCategory.COMMENT_OPTIONS -> AppIcons.Reply
        SettingsCategory.MESSAGING -> Symbols.Mail
        SettingsCategory.MEDIA -> AppIcons.Image
        SettingsCategory.LINKS -> AppIcons.Link
        SettingsCategory.HISTORY -> AppIcons.Clock
        SettingsCategory.DATA -> AppIcons.DataTransfer
        SettingsCategory.FILTERS -> AppIcons.Filter
        SettingsCategory.BACKUP -> AppIcons.Backup
        SettingsCategory.OTHER -> Symbols.Info
    }

/** Sync's settings: the categories at [category] null, else that category's page. */
@Composable
fun SettingsScreen(container: AppContainer, category: SettingsCategory? = null, highlight: String? = null) {
    val nav = LocalNavigator.current
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    // The root page's search: null when closed.
    var query by rememberSaveable { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }
    BackHandler(enabled = query != null) { query = null }

    SwipeBack(enabled = settings.swipeBack, onBack = nav::pop) {
        // The title collapses as the page scrolls; while searching, the bar is a plain one holding the field.
        val collapsing = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val pinned = TopAppBarDefaults.pinnedScrollBehavior()
        val scrollBehavior = if (query != null) pinned else collapsing
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                val back: @Composable () -> Unit = {
                    IconButton(onClick = { if (query != null) query = null else nav.pop() }) { Icon(Symbols.ArrowBack, t("Back")) }
                }
                val q = query
                if (q == null) {
                    LargeFlexibleTopAppBar(
                        title = { Text(category?.title ?: t("Settings")) },
                        navigationIcon = back,
                        actions = {
                            if (category == null) IconButton(onClick = { query = "" }) { Icon(Symbols.Search, t("Search settings")) }
                        },
                        scrollBehavior = collapsing,
                    )
                } else {
                    TopAppBar(
                        navigationIcon = back,
                        title = {
                            TextField(
                                value = q,
                                onValueChange = { query = it },
                                placeholder = { Text(t("Search settings")) },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                ),
                                // The placeholder isn't a label for TalkBack, so name the field.
                                modifier = Modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = t("Search settings") },
                            )
                            LaunchedEffect(Unit) { focus.requestFocus() }
                        },
                        actions = {
                            if (q.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Symbols.Close, t("Clear")) }
                        },
                        scrollBehavior = pinned,
                    )
                }
            },
        ) { padding ->
            SettingsColumn(padding, highlight, page = category?.name ?: "settings", bar = scrollBehavior.state) {
                val q = query
                if (category == null && q != null) {
                    SearchResults(q, settings.layout)
                } else if (category == null) {
                    SettingsCategory.entries.groupBy { it.group }.entries.forEachIndexed { i, (group, entries) ->
                        Section(group, divider = i > 0)
                        entries.forEach { entry ->
                            ListItem(
                                onClick = { nav.push(entry.route(settings.layout)) },
                                supportingContent = { Text(entry.summary) },
                                leadingContent = { Icon(entry.icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            ) { Text(entry.title) }
                        }
                    }
                } else {
                    CategoryRows(container, category, settings)
                }
            }
        }
    }
}

/** Settings rows matching [query]; tapping one opens the page it's on. */
@Composable
private fun SearchResults(query: String, layout: PostLayout) {
    val nav = LocalNavigator.current
    if (query.isBlank()) return
    val results = remember(query) { SettingsIndex.search(query) }
    if (results.isEmpty()) {
        Text(
            t("No settings match \"{0}\"", query.trim()),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
        return
    }
    results.forEach { entry ->
        ListItem(
            // The row itself scrolls into view and flashes, on the pages that can.
            onClick = {
                nav.push(
                    when (val route = entry.open(layout)) {
                        is SettingsRoute -> route.copy(highlight = entry.title)
                        is CustomizeViewRoute -> route.copy(highlight = entry.title)
                        else -> route
                    },
                )
            },
            overlineContent = { Text(entry.place) },
            supportingContent = entry.summary?.let { { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis) } },
        ) { Text(entry.title) }
    }
}

@Composable
private fun CategoryRows(container: AppContainer, category: SettingsCategory, settings: Settings) {
    val nav = LocalNavigator.current
    val update = container.settings::update
    val context = LocalContext.current
    when (category) {
        SettingsCategory.GENERAL -> {
            Section(t("Language"), divider = false)
            Choice(t("Language"), AppLanguage.entries, settings.language, { if (it == AppLanguage.SYSTEM) t("System default") else it.nativeName }) { v ->
                update { it.copy(language = v) }
                I18n.use(v)
                // Screens read their text as they're built, so a restart shows the new language everywhere.
                (context as? android.app.Activity)?.recreate()
            }
            Section(t("Navigation"))
            Toggle(t("Drawer navigation"), t("A side drawer, opened from your avatar, replaces the bottom bar"), settings.drawerNavigation) { v -> update { it.copy(drawerNavigation = v) } }
            Toggle(t("Confirm exit"), t("Ask before back closes the app"), settings.confirmExit) { v -> update { it.copy(confirmExit = v) } }
            Section(t("Gestures"))
            Toggle(t("Slide to go back"), t("Drag a page to the right to close it"), settings.swipeBack) { v -> update { it.copy(swipeBack = v) } }
            Section(t("Fonts"))
            Choice(t("Typeface"), Typeface.entries, settings.typeface, Typeface::label) { v -> update { it.copy(typeface = v) } }
            Choice(t("Base font size"), BaseFontSize.entries, settings.baseFontSize, BaseFontSize::label) { v ->
                update { it.copy(baseFontSize = v) }
                // The scale lives in the activity's configuration, which only a restart rebuilds.
                (context as? android.app.Activity)?.recreate()
            }
        }
        SettingsCategory.TABLET -> {
            Toggle(t("Tablet mode"), t("On wide screens, a navigation rail replaces the bottom bar"), settings.tabletMode) { v -> update { it.copy(tabletMode = v) } }
            Toggle(
                t("Navigation on the right"),
                t("On wide screens, like a foldable's inner screen, move the bottom bar to a rail on the right edge for one-handed use"),
                settings.navRailRight,
            ) { v -> update { it.copy(navRailRight = v) } }
            // Sync's Tablet › Options: the dual pane per orientation, on windows wide enough for two panes.
            if (settings.tabletMode) {
                Toggle(t("Dual pane (landscape)"), t("Show comments next to posts in landscape"), settings.dualPaneLandscape) { v -> update { it.copy(dualPaneLandscape = v) } }
                Toggle(t("Dual pane (portrait)"), t("The same in portrait, on wide screens"), settings.dualPanePortrait) { v -> update { it.copy(dualPanePortrait = v) } }
                Toggle(t("Tabbed comments"), t("Show a post and its comments as tabs"), settings.tabbedComments) { v -> update { it.copy(tabbedComments = v) } }
            }
        }
        SettingsCategory.THEME -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Toggle(t("Material You colors"), t("Use your wallpaper's colors"), settings.dynamicColor) { v -> update { it.copy(dynamicColor = v) } }
            }
            if (!(settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)) {
                Text(t("Theme color"), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp, top = 12.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()).selectableGroup().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (listOf<Int?>(null) + THEME_SEEDS).forEach { seed ->
                        // "Default" (no seed) is Faden's own blue palette; Classic lives in Theme management.
                        val selected = settings.seedColor == seed && (seed != null || settings.palette == ThemePalette.FADEN)
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(seed ?: ToneScheme.FADEN_SEED))
                                .border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                .semantics { contentDescription = seed?.let { c -> t("Theme color {0}", "#%06X".format(c and 0xFFFFFF)) } ?: t("Default theme color") }
                                .selectable(selected = selected, role = Role.RadioButton) {
                                    update { it.copy(seedColor = seed, palette = if (seed == null) ThemePalette.FADEN else it.palette) }
                                },
                        )
                    }
                }
                if (settings.seedColor != null) {
                    Toggle(t("Extra colorful"), t("Tint backgrounds and cards with the accent too"), settings.extraColorful) { v -> update { it.copy(extraColorful = v) } }
                }
            }
            ListItem(
                onClick = { nav.push(ThemeManagementRoute) },
                supportingContent = { Text(t("Preview, save, share and load themes")) },
            ) { Text(t("Saved themes")) }
        }
        SettingsCategory.DARK_MODE -> {
            Choice(t("Theme"), ThemeMode.entries, settings.theme, ThemeMode::label) { v -> update { it.copy(theme = v) } }
            DarkOverlayRows(settings, update)
        }
        // View type has no page of its own: its row opens Customize view (see [SettingsCategory.route]), which holds
        // every view setting.
        SettingsCategory.VIEW_TYPE -> Unit
        SettingsCategory.COMMENTS -> {
            Toggle(t("Show user flair"), null, settings.commentFlair) { v -> update { it.copy(commentFlair = v) } }
            Toggle(t("Show profile pictures"), t("Authors' avatars in comment headers"), settings.profilePictures) { v -> update { it.copy(profilePictures = v) } }
            Toggle(t("Mark new comments"), t("Label comments posted since you last opened the thread"), settings.markNewComments) { v -> update { it.copy(markNewComments = v) } }
            Toggle(t("Hide AutoMod comments"), t("And the replies to them"), settings.hideAutoMod) { v -> update { it.copy(hideAutoMod = v) } }
            Choice(t("Live comments refresh rate"), LiveRefresh.entries, settings.liveRefresh, LiveRefresh::label) { v -> update { it.copy(liveRefresh = v) } }
        }
        SettingsCategory.POSTS -> {
            Choice(t("NSFW posts"), NsfwMode.entries, settings.nsfw, NsfwMode::label) { v -> update { it.copy(nsfw = v) } }
            if (settings.nsfw == NsfwMode.BLUR) {
                Toggle(t("Blur NSFW only on Frontpage, Popular & All"), t("Show NSFW previews unblurred everywhere else"), settings.blurNsfwOnlyFrontpageAndAll) { v ->
                    update { it.copy(blurNsfwOnlyFrontpageAndAll = v) }
                }
            }
            Toggle(t("Blur spoiler previews"), null, settings.blurSpoilers) { v -> update { it.copy(blurSpoilers = v) } }
            Toggle(t("Show post flair"), t("Flair chips on posts in feeds"), settings.showPostFlair) { v -> update { it.copy(showPostFlair = v) } }
            Toggle(t("Show post author"), t("In feeds"), settings.showPostAuthor) { v -> update { it.copy(showPostAuthor = v) } }
            Toggle(t("Display short scores"), t("10.6k instead of 10,600"), settings.shortScores) { v -> update { it.copy(shortScores = v) } }
            Toggle(t("Ask to keep a new sort"), t("When you change a feed's sort, ask whether to always open it that way"), settings.askToKeepSort) { v ->
                update { it.copy(askToKeepSort = v) }
            }
            Section(t("Swipe actions"))
            Toggle(t("Swipe posts"), t("Drag a post left; drag further for the next action"), settings.postSwipe) { v -> update { it.copy(postSwipe = v) } }
            if (settings.postSwipe) {
                SwipeStageChoices(settings.postSwipeStages, SwipeAction.entries.filter { it.posts }) { stages -> update { it.copy(postSwipeActions = stages) } }
            }
        }
        SettingsCategory.COMMENT_OPTIONS -> {
            Choice(t("Default comment sort"), CommentSort.entries, settings.commentSort, CommentSort::label) { v -> update { it.copy(commentSort = v) } }
            Toggle(t("Collapse all threads by default"), t("The first time you open a post"), settings.collapseAllByDefault) { v -> update { it.copy(collapseAllByDefault = v) } }
            Choice(t("Initial comment count"), CommentCount.entries, settings.commentCount, CommentCount::label) { v -> update { it.copy(commentCount = v) } }
            Toggle(
                t("Tap to collapse"),
                if (settings.tapToCollapse) t("Tap a comment to collapse it; hold it for options") else t("Hold a comment to collapse it; tap it for options"),
                settings.tapToCollapse,
            ) { v -> update { it.copy(tapToCollapse = v) } }
            Toggle(t("Comment navigation buttons"), t("Jump between top-level comments"), settings.commentNavigation) { v -> update { it.copy(commentNavigation = v) } }
            Toggle(t("Volume key navigation"), t("Volume buttons jump between top-level comments"), settings.volumeKeyNavigation) { v -> update { it.copy(volumeKeyNavigation = v) } }
            Toggle(t("Remember comment position"), t("Reopen posts where you left off, threads folded as you left them"), settings.rememberCommentPosition) { v -> update { it.copy(rememberCommentPosition = v) } }
            Section(t("Swipe actions"))
            Toggle(t("Swipe comments"), t("Drag a comment left; drag further for the next action"), settings.commentSwipe) { v -> update { it.copy(commentSwipe = v) } }
            if (settings.commentSwipe) {
                SwipeStageChoices(settings.commentSwipeStages, SwipeAction.entries.filter { it.comments }) { stages -> update { it.copy(commentSwipeActions = stages) } }
            }
        }
        SettingsCategory.MESSAGING -> MessagingRows(container, settings)
        SettingsCategory.MEDIA -> {
            Choice(t("Close images by"), listOf(true, false), settings.tapToClose, { if (it) t("Single tap") else t("Swiping up or down") }) { v -> update { it.copy(tapToClose = v) } }
            // Default first, unlike images: videos close by swiping unless you pick a tap.
            Choice(t("Close videos by"), listOf(false, true), settings.tapToCloseVideos, { if (it) t("Single tap") else t("Swiping up or down") }) { v -> update { it.copy(tapToCloseVideos = v) } }
            // Images that close by swiping always do; the toggle covers the ones that close by a tap, and videos.
            Toggle(
                t("Swipe to close"),
                when {
                    settings.tapToClose && settings.tapToCloseVideos -> t("Fling images and videos down to close them")
                    settings.tapToClose -> t("Fling videos up or down, and images down, to close them")
                    settings.tapToCloseVideos -> t("Fling videos down to close them")
                    else -> t("Fling videos up or down to close them")
                },
                settings.swipeToClose,
            ) { v -> update { it.copy(swipeToClose = v) } }
            Toggle(t("Hold to preview"), t("Hold a picture in a feed to see it full screen until you let go"), settings.holdToPreview) { v -> update { it.copy(holdToPreview = v) } }
            if (settings.holdToPreview) {
                Toggle(t("Unblur NSFW previews"), t("Holding a blurred NSFW picture shows it unblurred"), settings.peekUnblursNsfw) { v -> update { it.copy(peekUnblursNsfw = v) } }
            }
            Toggle(t("Start videos muted"), null, settings.muteVideos) { v -> update { it.copy(muteVideos = v) } }
            Toggle(t("Display seek bar"), null, settings.videoSeekBar) { v -> update { it.copy(videoSeekBar = v) } }
            Toggle(t("Display speed controls"), null, settings.videoSpeedControls) { v -> update { it.copy(videoSpeedControls = v) } }
        }
        SettingsCategory.LINKS -> {
            Toggle(t("Open links in the app"), t("Custom Tabs instead of your browser app"), settings.inAppBrowser) { v -> update { it.copy(inAppBrowser = v) } }
            Toggle(t("Link cards"), t("Number links and list them under posts and comments"), settings.linkCards) { v -> update { it.copy(linkCards = v) } }
            Toggle(t("Pictures in comments"), t("Show GIFs and images under the comments that post or link them, instead of their links"), settings.commentGifs) { v -> update { it.copy(commentGifs = v) } }
            // Faden can't verify reddit.com as its own, so Android 12+ leaves its links off until they're allowed here.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ListItem(
                    onClick = {
                        val intent = Intent(android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, "package:${context.packageName}".toUri())
                        runCatching { context.startActivity(intent) }.onFailure {
                            Toast.makeText(context, t("Couldn't open the app's link settings"), Toast.LENGTH_SHORT).show()
                        }
                    },
                    supportingContent = { Text(t("Under \"Open by default\", add reddit.com and redd.it, so links from other apps open here")) },
                ) { Text(t("Open Reddit links in Faden")) }
            }
        }
        SettingsCategory.HISTORY -> {
            Toggle(t("Mark posts as read on scroll"), t("Posts you scroll past in a feed count as read"), settings.markReadOnScroll) { v -> update { it.copy(markReadOnScroll = v) } }
            Toggle(t("Hide read posts"), t("Leave out posts you have already opened when a feed loads"), settings.hideRead) { v -> update { it.copy(hideRead = v) } }
        }
        SettingsCategory.DATA -> {
            Choice(t("Data saving mode"), DataSaving.entries, settings.dataSaving, DataSaving::label) { v -> update { it.copy(dataSaving = v) } }
            Text(
                t("Data saving leaves feed thumbnails and previews unloaded and opens Reddit videos in SD"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            Choice(t("HD Reddit video quality"), VideoQuality.entries, settings.hdVideo, VideoQuality::label) { v -> update { it.copy(hdVideo = v) } }
            Choice(t("Autoplay"), Autoplay.entries, settings.autoplay, Autoplay::label) { v -> update { it.copy(autoplay = v) } }
            Text(
                t("Videos and GIFs play muted in Cards, Slides and Swipe mode, one at a time, unless data saving is on. Customize view can override it per view"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        SettingsCategory.FILTERS -> {
            // Sync's Post filters: a row per list, each opening its editor. Post ⋮ → Filter… adds to them too.
            val filters by container.filters.filters.collectAsStateWithLifecycle()
            FilterType.entries.forEach { type ->
                val count = type.of(filters).size
                ListItem(
                    onClick = { nav.push(FilterEditorRoute(type)) },
                    supportingContent = { Text(if (count == 0) type.summary else "${type.summary} · $count") },
                ) { Text(type.title) }
            }
        }
        SettingsCategory.BACKUP -> BackupSection(container)
        SettingsCategory.OTHER -> {
            ListItem(
                onClick = { nav.push(SetupRoute) },
                supportingContent = { Text(t("Client ID, redirect URI, user agent")) },
            ) { Text(t("Reddit API")) }
            var notesOpen by remember { mutableStateOf(false) }
            ListItem(
                supportingContent = { Text(BuildConfig.VERSION_NAME) },
            ) { Text(t("Version")) }
            ListItem(
                onClick = { notesOpen = true },
                supportingContent = { Text(t("What changed in each version")) },
            ) { Text(t("What's new")) }
            Toggle(t("Check for updates"), t("Say when a new version is out on GitHub"), settings.checkForUpdates) { v -> update { it.copy(checkForUpdates = v) } }
            ListItem(
                onClick = { nav.openExternal(DONATE_URL) },
                supportingContent = { Text(t("Donate on Ko-fi. Faden stays free either way")) },
            ) { Text(t("Support Faden")) }
            if (notesOpen) WhatsNewDialog(t("What's new"), container.whatsNew, onDismiss = { notesOpen = false })
        }
    }
}

/** Sync's Messaging: the message check, and the communities and posts being watched for new posts and comments. */
@Composable
private fun MessagingRows(container: AppContainer, settings: Settings) {
    val update = container.settings::update
    val watched by container.watch.watched.collectAsStateWithLifecycle()
    val watchedPosts by container.watch.posts.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Toast.makeText(context, t("Allow notifications to hear about new messages"), Toast.LENGTH_SHORT).show()
    }
    Choice(t("Periodically check for new messages"), MessageCheck.entries, settings.messageCheck, MessageCheck::label) { v ->
        update { it.copy(messageCheck = v) }
        InboxWorker.schedule(context, v)
        if (v != MessageCheck.OFF && !Notifications.canNotify(context)) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    if (settings.messageCheck != MessageCheck.OFF) {
        ListItem(
            onClick = { InboxWorker.checkNow(context) },
            supportingContent = { Text(t("Look for new messages without waiting for the next check")) },
        ) { Text(t("Check now")) }
    }

    Section(t("Watched communities"))
    if (watched.isEmpty()) {
        ListItem(
            supportingContent = { Text(t("Tap Watch on a community to get a notification for each new post")) },
        ) { Text(t("None yet")) }
    }
    if (watched.isNotEmpty() || watchedPosts.isNotEmpty()) {
        ListItem(
            onClick = { WatchWorker.checkNow(context) },
            supportingContent = { Text(t("Look for new posts and comments without waiting for the hourly check")) },
        ) { Text(t("Check now")) }
    }
    watched.values.sortedBy { it.name.lowercase() }.forEach { community ->
        ListItem(
            supportingContent = { Text(t("Checked every hour")) },
            trailingContent = {
                IconButton(onClick = { container.setWatching(community.name, false) }) {
                    Icon(Symbols.Close, t("Stop watching r/{0}", community.name))
                }
            },
        ) { Text("r/${community.name}") }
    }
    if (watchedPosts.isNotEmpty()) {
        Section(t("Watched posts"))
        watchedPosts.values.sortedBy { it.title.lowercase() }.forEach { post ->
            ListItem(
                supportingContent = { Text(t("r/{0} · new comments are checked every hour", post.subreddit)) },
                trailingContent = {
                    IconButton(onClick = { container.unwatchPost(post.id) }) { Icon(Symbols.Close, t("Stop watching")) }
                },
            ) { Text(post.title, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        }
    }
}

/** Relaunches the app in a fresh process, so every store reloads from preferences. */
internal fun restartApp(context: Context) {
    val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
    launch?.component?.let { context.startActivity(Intent.makeRestartActivityTask(it)) }
    Runtime.getRuntime().exit(0)
}

/** Sync's "First / Second / Third swipe action" rows. */
@Composable
private fun SwipeStageChoices(stages: List<SwipeAction>, options: List<SwipeAction>, onChange: (List<SwipeAction>) -> Unit) {
    listOf(t("First swipe action"), t("Second swipe action"), t("Third swipe action")).forEachIndexed { i, name ->
        Choice(name, options, stages[i], SwipeAction::label) { v ->
            onChange(stages.toMutableList().also { it[i] = v })
        }
    }
}

/** A settings group heading in the accent color, under a divider ([divider] false at the top of a page). */
@Composable
internal fun Section(title: String, divider: Boolean = true) {
    if (divider) HorizontalDivider(Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
    Text(
        title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

/** A switch row; tapping anywhere on it flips the switch. */
@Composable
internal fun Toggle(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val jump = rememberRowJump(title)
    val style = rowStyle(checked, jump.flash())
    // One target with toggle semantics: the row is the toggleable, the switch only shows the state. Switched on, the
    // row takes a tint of the accent, so what's on stands out while scanning a page.
    ListItem(
        checked = checked,
        onCheckedChange = onChange,
        modifier = jump.modifier.then(style.modifier),
        colors = style.colors,
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
    ) { Text(title) }
}

/** A row showing [selected]; tapping it opens a dialog of radio [options]. */
@Composable
internal fun <T> Choice(title: String, options: List<T>, selected: T, label: (T) -> String, onPick: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val jump = rememberRowJump(title)
    val style = rowStyle(checked = false, flash = jump.flash())
    ListItem(
        onClick = { open = true },
        modifier = jump.modifier.then(style.modifier),
        colors = style.colors,
        supportingContent = { Text(label(selected)) },
    ) { Text(title) }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(title) },
            text = {
                Column {
                    options.forEach { option ->
                        ListItem(
                            selected = option == selected,
                            onClick = {
                                open = false
                                onPick(option)
                            },
                            leadingContent = { RadioButton(selected = option == selected, onClick = null) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent, selectedContainerColor = Color.Transparent),
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                        ) { Text(label(option)) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text(t("Close")) } },
        )
    }
}
