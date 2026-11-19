package io.github.radixtrator.resync.ui.settings

import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.ScrollState
import android.os.Build
import android.view.RoundedCorner
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import io.github.radixtrator.resync.i18n.t
import io.github.radixtrator.resync.ui.common.AppIcons
import io.github.radixtrator.resync.ui.common.Symbols
import io.github.radixtrator.resync.ui.navigation.CustomizeViewRoute
import io.github.radixtrator.resync.ui.navigation.FilterEditorRoute
import io.github.radixtrator.resync.ui.navigation.LocalNavigator
import io.github.radixtrator.resync.ui.navigation.Navigator
import io.github.radixtrator.resync.ui.navigation.SettingsRoute
import io.github.radixtrator.resync.ui.navigation.ThemeManagementRoute
import io.github.radixtrator.resync.ui.theme.FadenAccents

/**
 * Settings › "View in feed": a quick look at the app with the settings just changed, without leaving the settings.
 * It takes the settings pages off the back stack ([stash]), which uncovers the very feed (or page) they were opened
 * from, where it was scrolled to; the pill or back puts them back ([close]), each scrolled to where it was ([positions]).
 */
internal object SettingsPeek {
    /** The settings pages taken off the back stack, bottom first, while peeking; null otherwise. */
    var stash by mutableStateOf<List<NavKey>?>(null)
        private set

    /** The back stack's size once the settings pages are off: the page the peek shows. */
    private var base = 0

    /** A settings page's scroll state, and that of its collapsing title bar if it has one. */
    class Page(val scroll: ScrollState, val bar: TopAppBarState?)

    /** Where a page was scrolled to, and how far its title bar had collapsed. */
    class Position(val scroll: Int, val barOffset: Float?, val barContentOffset: Float?)

    /** Each settings page that is showing, by name ([SettingsColumn]). */
    val showing = mutableMapOf<String, Page>()

    /** Where the showing pages were scrolled to when the peek opened; each page takes its own back as it returns. */
    val positions = mutableMapOf<String, Position>()

    /** Where the "← Settings" pill sits: false at the top (the default, chosen on a phone), true at the bottom. */
    var pillAtBottom = false

    val active: Boolean get() = stash != null

    fun open(nav: Navigator, backStackSize: () -> Int) {
        val pages = nav.takeWhile { it.isSettingsPage() }
        if (pages.isEmpty()) return
        base = backStackSize()
        positions.clear()
        showing.forEach { (name, page) ->
            positions[name] = Position(page.scroll.value, page.bar?.heightOffset, page.bar?.contentOffset)
        }
        stash = pages
    }

    /** Back to the settings: drops whatever was opened from the peek, then puts the settings pages back. */
    fun close(nav: Navigator, backStackSize: () -> Int) {
        val pages = stash ?: return
        nav.popWhile { backStackSize() > base }
        // The search's jump has been seen; the pages go back to where they were scrolled instead.
        nav.pushAll(pages.map { if (it is SettingsRoute) it.copy(highlight = null) else if (it is CustomizeViewRoute) it.copy(highlight = null) else it })
        stash = null
    }

    /** True while the peek shows the page it uncovered, not one opened from it: back returns to the settings. */
    fun atBase(backStackSize: Int) = active && backStackSize <= base
}

private fun NavKey.isSettingsPage() = this is SettingsRoute || this is CustomizeViewRoute || this is ThemeManagementRoute || this is FilterEditorRoute

/**
 * The categories whose settings change how Reddit's content looks or behaves in the app: all but Backup, Everything
 * else, and Messaging (whose checks run in the background, for notifications).
 */
internal val SettingsCategory.peekable: Boolean
    get() = this !in setOf(SettingsCategory.BACKUP, SettingsCategory.OTHER, SettingsCategory.MESSAGING)

/** Settings pages that offer "View in feed": every one but the [peekable]-less categories, sub-pages included. */
internal fun NavKey.offersPeek(): Boolean = when (this) {
    is SettingsRoute -> category?.peekable ?: true
    is CustomizeViewRoute, ThemeManagementRoute, is FilterEditorRoute -> true
    else -> false
}

/** The FAB on a settings page that opens the peek: the orange of Faden's other FABs, with its label spelled out. */
@Composable
private fun ViewInFeedButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = FloatingActionButtonDefaults.extendedFabShape,
        color = FadenAccents.fabContainer,
        contentColor = FadenAccents.onFab,
        shadowElevation = 6.dp,
        modifier = modifier,
        onClick = onClick,
    ) {
        Row(
            Modifier.height(56.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(AppIcons.ViewStack, null)
            Text(t("View in feed"), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * Over the whole app: "View in feed" on every settings page that offers it ([offersPeek]); and while peeking, on every
 * page (the uncovered feed, and posts or media opened from it), a frame in the FAB orange like a shared screen and a
 * pill saying where back leads, "← Settings" (not the page or the setting, which are easy to forget; that you came
 * from the settings isn't). The pill returns to the settings from anywhere; back does once on the uncovered page.
 */
@Composable
fun SettingsPeekOverlay(backStack: List<NavKey>) {
    val nav = LocalNavigator.current
    if (backStack.lastOrNull()?.offersPeek() == true) {
        Box(Modifier.fillMaxSize()) {
            ViewInFeedButton(
                onClick = { SettingsPeek.open(nav) { backStack.size } },
                modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(16.dp),
            )
        }
    }
    // Positions no returning page has claimed (the stack was reset meanwhile) shouldn't surprise a later visit.
    LaunchedEffect(SettingsPeek.active) {
        if (!SettingsPeek.active) {
            delay(2000)
            SettingsPeek.positions.clear()
        }
    }
    if (!SettingsPeek.active) return
    val close = { SettingsPeek.close(nav) { backStack.size } }
    // Composed after NavDisplay, so it gets back before the page does.
    BackHandler(enabled = SettingsPeek.atBase(backStack.size)) { close() }
    Box(Modifier.fillMaxSize()) {
        PeekFrame()
        if (SettingsPeek.pillAtBottom) {
            BottomPill(t("Settings"), onClick = close, modifier = Modifier.align(Alignment.BottomCenter))
        } else {
            TopPill(t("Settings"), onClick = close, modifier = Modifier.align(Alignment.TopCenter))
        }
    }
}

/** The frame's width; thick enough to stay visible where the screen's rounded corners cut into it. */
private val PEEK_FRAME = 8.dp

/**
 * The frame around the whole screen, in the orange of Faden's FABs (so it reads as "View in feed" carried on), following
 * the display's rounded corners where Android reports them (Android 12+), drawn over the app without taking touches.
 */
@Composable
private fun PeekFrame() {
    val view = LocalView.current
    val density = LocalDensity.current
    val corner = remember(view) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@remember 0.dp
        val radius = view.rootWindowInsets?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius ?: 0
        with(density) { radius.toDp() }
    }
    Box(Modifier.fillMaxSize().border(PEEK_FRAME, FadenAccents.fabContainer, RoundedCornerShape(corner)))
}

/** At the top, below the status bar and any camera: large, so it's the first thing seen. */
@Composable
private fun TopPill(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = FadenAccents.fabContainer,
        contentColor = FadenAccents.onFab,
        shadowElevation = 6.dp,
        modifier = modifier.statusBarsPadding().padding(top = 12.dp),
    ) {
        Row(
            Modifier.clickable(onClickLabel = t("Back to settings"), onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Symbols.ArrowBack, null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 240.dp))
        }
    }
}

/** At the bottom, above the navigation bar, in the frame's FAB orange. */
@Composable
private fun BottomPill(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = FadenAccents.fabContainer,
        contentColor = FadenAccents.onFab,
        shadowElevation = 6.dp,
        modifier = modifier.navigationBarsPadding().padding(bottom = 20.dp),
    ) {
        Row(
            Modifier.clickable(onClickLabel = t("Back to settings"), onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Symbols.ArrowBack, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 220.dp))
        }
    }
}
