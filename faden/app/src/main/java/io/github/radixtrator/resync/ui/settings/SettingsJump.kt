package io.github.radixtrator.resync.ui.settings

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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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

/** A flashing row's colors: its pill ([container]) and text ([content]). */
internal class RowTint(val container: Color, val content: Color)

/** The colors of a row over [base] with [baseContent] text at [flash] (0 to 1): blended toward the primary with onPrimary text. */
internal fun rowTint(scheme: ColorScheme, base: Color, baseContent: Color, flash: Float): RowTint =
    RowTint(lerp(base, scheme.primary, flash), lerp(baseContent, scheme.onPrimary, flash))

/** The flash's pill sits this far inside the row, so it clears the neighboring rows. */
private val PillInsetX = 8.dp
private val PillInsetY = 2.dp

/** Its corners: rounded, not fully round, which would crowd the text and the switch at its ends. */
private val PillCorner = 16.dp

/** How a settings row is drawn: its [colors], and a [modifier] that paints its pill behind it. */
internal class RowStyle(val colors: ListItemColors, val modifier: Modifier)

/**
 * A row that may be flashing: a pill of the accent fades in behind it and its text turns onPrimary. A checked
 * ListItem's container stays transparent either way; the switch already shows the state.
 */
@Composable
internal fun rowStyle(flash: Float): RowStyle {
    val plain = ListItemDefaults.colors(selectedContainerColor = Color.Transparent)
    if (flash == 0f) return RowStyle(plain, Modifier)
    val scheme = MaterialTheme.colorScheme
    val tint = rowTint(scheme, scheme.surface, plain.contentColor, flash)
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
    val pill = scheme.primary.copy(alpha = flash)
    return RowStyle(
        colors,
        Modifier.drawBehind {
            val x = PillInsetX.toPx()
            val y = PillInsetY.toPx()
            drawRoundRect(pill, Offset(x, y), Size(size.width - 2 * x, size.height - 2 * y), CornerRadius(PillCorner.toPx()))
        },
    )
}
