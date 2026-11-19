package io.github.radixtrator.resync.ui.settings

import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import io.github.radixtrator.resync.ui.theme.Tones
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * How strongly a switched-on row is tinted with the accent: a share of the primary color over the page. A light tint,
 * tuned on a phone; ContrastTest keeps its text readable in every theme (above 9:1 at 0.2).
 */
internal const val CHECKED_ROW_ALPHA = 0.2f

/**
 * A search result's row flashes this many times, each [FLASH_MS] in and [FLASH_MS] out (1.2 s in all; calm, not
 * hectic). Like every animation it follows the system's animator duration scale.
 */
private const val FLASHES = 1
private const val FLASH_MS = 600

/** After scrolling to the row, the page waits this long before the pulse, so it meets a reader who has found their feet. */
private const val PULSE_DELAY_MS = 200L

/**
 * Settings › search: the row a result jumped to ([target], its title as shown). The page scrolls it to the middle
 * of [scroll]'s viewport and it pulses the accent once, as Android's own settings do.
 */
internal class SettingsJump(val target: String?, val scroll: ScrollState, val onDone: () -> Unit) {
    var viewport: LayoutCoordinates? = null
}

internal val LocalSettingsJump = compositionLocalOf<SettingsJump?> { null }

/**
 * Provides the jump for a page whose rows scroll in [scroll]; put [viewport] on the scrolling container, before
 * its verticalScroll. [target] null jumps nowhere. It jumps once per visit to the page, not again on the way back.
 */
@Composable
internal fun ProvideSettingsJump(target: String?, scroll: ScrollState, content: @Composable (viewport: Modifier) -> Unit) {
    var done by rememberSaveable(target) { mutableStateOf(target == null) }
    val jump = remember(target, scroll) { SettingsJump(target.takeUnless { done }, scroll) { done = true } }
    CompositionLocalProvider(LocalSettingsJump provides jump) {
        content(Modifier.onGloballyPositioned { jump.viewport = it })
    }
}

/** What a row needs to take part in a jump: a [modifier] to place on it, and how far its flash is (0 to 1). */
internal class RowJump(val modifier: Modifier, val flash: () -> Float)

private val NoJump = RowJump(Modifier) { 0f }

/** The jump for the row titled [title]: scrolls it into the middle and flashes it, if it's the target. */
@Composable
internal fun rememberRowJump(title: String): RowJump {
    val jump = LocalSettingsJump.current
    if (jump == null || jump.target != title) return NoJump
    val flash = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    return remember(jump) {
        var started = false
        RowJump(
            Modifier.onGloballyPositioned { row ->
                val viewport = jump.viewport ?: return@onGloballyPositioned
                if (started || !row.isAttached || !viewport.isAttached) return@onGloballyPositioned
                started = true
                val rowTop = row.positionInRoot().y - viewport.positionInRoot().y
                val middle = (viewport.size.height - row.size.height) / 2f
                val target = (jump.scroll.value + rowTop - middle).roundToInt().coerceIn(0, jump.scroll.maxValue)
                scope.launch {
                    jump.scroll.animateScrollTo(target)
                    delay(PULSE_DELAY_MS)
                    repeat(FLASHES) {
                        flash.animateTo(1f, tween(FLASH_MS))
                        flash.animateTo(0f, tween(FLASH_MS))
                    }
                    jump.onDone()
                }
            },
        ) { flash.value }
    }
}

/** A settings row's colors: plain, or tinted while [checked] ([CHECKED_ROW_ALPHA]), and the full accent at [flash] 1. */
internal class RowTint(val container: Color, val content: Color)

/**
 * The tint for a row over [base] with [baseContent] text. Switched on, the page color takes [CHECKED_ROW_ALPHA] of the
 * primary, and the text is whichever of the plain text and onPrimary reads better on it. A flash blends that toward
 * the primary with onPrimary text.
 */
internal fun rowTint(
    scheme: ColorScheme,
    base: Color,
    baseContent: Color,
    checked: Boolean,
    flash: Float,
    alpha: Float = CHECKED_ROW_ALPHA,
): RowTint {
    val resting = if (checked) scheme.primary.copy(alpha = alpha).compositeOver(base) else base
    val restingContent = if (!checked) {
        baseContent
    } else {
        listOf(baseContent, scheme.onPrimary).maxBy { Tones.contrast(it.toArgb(), resting.toArgb()) }
    }
    return RowTint(lerp(resting, scheme.primary, flash), lerp(restingContent, scheme.onPrimary, flash))
}

/** A tinted row's pill sits this far inside the row, so two tinted rows one under the other stay apart. */
private val PillInsetX = 8.dp
private val PillInsetY = 2.dp

/** Its corners: rounded, not fully round, which would crowd the text and the switch at its ends. */
private val PillCorner = 16.dp

/** How a settings row is drawn: its [colors], and a [modifier] that paints its pill behind it. */
internal class RowStyle(val colors: ListItemColors, val modifier: Modifier)

/**
 * A row that may be [checked] and may be flashing: switched on, a pill of [rowTint] inset in the row; flashing, the
 * same pill in the full accent (fading in over a row that's off). The text takes the tint's content color.
 */
@Composable
internal fun rowStyle(checked: Boolean, flash: Float): RowStyle {
    val defaults = ListItemDefaults.colors()
    if (!checked && flash == 0f) return RowStyle(defaults, Modifier)
    val scheme = MaterialTheme.colorScheme
    val tint = rowTint(scheme, scheme.surface, defaults.contentColor, checked, flash)
    val pill = if (checked) tint.container else scheme.primary.copy(alpha = flash)
    val colors = ListItemDefaults.colors(
        containerColor = Color.Transparent,
        contentColor = tint.content,
        supportingContentColor = tint.content,
        overlineContentColor = tint.content,
        trailingContentColor = tint.content,
        selectedContainerColor = Color.Transparent,
        selectedContentColor = tint.content,
        selectedSupportingContentColor = tint.content,
        selectedOverlineContentColor = tint.content,
        selectedTrailingContentColor = tint.content,
    )
    return RowStyle(
        colors,
        Modifier.drawBehind {
            val x = PillInsetX.toPx()
            val y = PillInsetY.toPx()
            val height = size.height - 2 * y
            drawRoundRect(pill, Offset(x, y), Size(size.width - 2 * x, height), CornerRadius(PillCorner.toPx()))
        },
    )
}

/** How much room a settings page leaves under its last row for "View in feed" ([SettingsPeekOverlay]). */
internal val ViewInFeedClearance = 88.dp

/**
 * A settings page's scrolling rows: [content] in a column that can jump to a row, with room for "View in feed". [page]
 * names it for the peek: while a peek is open the page is taken off the back stack, so it remembers where it was
 * scrolled to and goes back there when it returns ([SettingsPeek.positions]).
 */
@Composable
internal fun SettingsColumn(
    padding: PaddingValues,
    highlight: String?,
    page: String,
    bar: TopAppBarState? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scroll = rememberScrollState()
    DisposableEffect(page, scroll) {
        val showing = SettingsPeek.Page(scroll, bar)
        SettingsPeek.showing[page] = showing
        onDispose { if (SettingsPeek.showing[page] === showing) SettingsPeek.showing.remove(page) }
    }
    LaunchedEffect(page, scroll) {
        val saved = SettingsPeek.positions.remove(page) ?: return@LaunchedEffect
        // The title bar that had collapsed as the page scrolled comes back open; collapse it as far as it was.
        if (bar != null) {
            saved.barOffset?.let { bar.heightOffset = it }
            saved.barContentOffset?.let { bar.contentOffset = it }
        }
        // A page that has just come back doesn't know its height in its first frame (nothing to scroll yet), so wait
        // until it's as tall as it was. It may be shorter now, so not for long.
        withTimeoutOrNull(RESTORE_WAIT_MS) { snapshotFlow { scroll.maxValue }.first { it >= saved.scroll } }
        scroll.scrollTo(saved.scroll)
    }
    ProvideSettingsJump(highlight, scroll) { viewport ->
        Column(Modifier.padding(padding).fillMaxSize().then(viewport).verticalScroll(scroll)) {
            content()
            Spacer(Modifier.height(ViewInFeedClearance))
        }
    }
}

private const val RESTORE_WAIT_MS = 500L
