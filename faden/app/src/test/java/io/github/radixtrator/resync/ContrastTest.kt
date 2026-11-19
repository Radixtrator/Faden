package io.github.radixtrator.resync

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import io.github.radixtrator.resync.data.ThemePalette
import io.github.radixtrator.resync.ui.theme.FadenColors
import io.github.radixtrator.resync.ui.theme.THEME_SEEDS
import io.github.radixtrator.resync.ui.theme.ToneScheme
import io.github.radixtrator.resync.ui.theme.Tones
import io.github.radixtrator.resync.ui.theme.fadenColorScheme
import io.github.radixtrator.resync.ui.settings.rowTint
import kotlin.test.Test
import kotlin.test.assertTrue

/** WCAG contrast of the colors the theme generates, over every seed, mode, intensity and Extra colorful. */
class ContrastTest {
    private fun Color.argb() = toArgb()

    private class Case(val name: String, val seed: Int?, val palette: ThemePalette)

    private val cases = buildList {
        add(Case("default", null, ThemePalette.FADEN))
        add(Case("classic", null, ThemePalette.CLASSIC))
        // Intensity and Extra colorful don't touch the default scheme, but Faden blue as a picked seed takes them.
        add(Case("faden blue seed", ToneScheme.FADEN_SEED, ThemePalette.FADEN))
        THEME_SEEDS.forEach { add(Case("seed %06X".format(it and 0xFFFFFF), it, ThemePalette.FADEN)) }
    }

    /** Failures of [check] over all cases and settings; Classic's are returned apart, it being Sync's fixed palette. */
    private fun failures(check: (scheme: androidx.compose.material3.ColorScheme) -> String?): Pair<List<String>, List<String>> {
        val bad = mutableListOf<String>()
        val classic = mutableListOf<String>()
        for (case in cases) for (mode in listOf("light", "dark", "black")) {
            for (intensity in listOf(0f, 1f, 2f)) for (extra in listOf(false, true)) {
                val scheme = fadenColorScheme(
                    dark = mode != "light", black = mode == "black", seed = case.seed, extra = extra,
                    intensity = intensity, palette = case.palette,
                )
                val problem = check(scheme) ?: continue
                val line = "${case.name} $mode intensity=$intensity extra=$extra: $problem"
                if (case.palette == ThemePalette.CLASSIC) classic += line else bad += line
            }
        }
        // Classic ignores intensity and Extra colorful, so report each of its failures once.
        return bad to classic.map { it.replace(Regex(" intensity=\\S+ extra=\\S+"), "") }.distinct()
    }

    private fun ratio(a: Color, b: Color) = Tones.contrast(a.argb(), b.argb())

    private fun need(label: String, a: Color, b: Color, min: Double): String? =
        ratio(a, b).let { if (it < min) "$label %.2f < $min".format(it) else null }

    @Test
    fun generatedSchemesMeetContrast() {
        val (bad, classic) = failures { s ->
            listOfNotNull(
                need("onPrimary/primary", s.onPrimary, s.primary, 4.5),
                need("onPrimaryContainer/primaryContainer", s.onPrimaryContainer, s.primaryContainer, 4.5),
                need("onSurface/surface", s.onSurface, s.surface, 7.0),
                need("onSurfaceVariant/surface", s.onSurfaceVariant, s.surface, 4.5),
                need("onSurfaceVariant/surfaceContainerLow", s.onSurfaceVariant, s.surfaceContainerLow, 4.5),
                need("primary/surface", s.primary, s.surface, 3.0),
            ).joinToString("; ").ifEmpty { null }
        }
        // Classic is Sync's fixed palette: its failures are how Sync shipped, so they're printed, not asserted.
        if (classic.isNotEmpty()) println("Classic (Sync's palette) contrast shortfalls:\n" + classic.joinToString("\n"))
        assertTrue(bad.isEmpty(), "Contrast failures:\n" + bad.joinToString("\n"))
    }

    @Test
    fun switchedOnAndFlashingSettingsRowsStayReadable() {
        // A switched-on settings row is tinted with CHECKED_ROW_ALPHA of the primary; a search result flashes the full
        // primary. Both have to keep their text readable.
        val (bad, classic) = failures { s ->
            val on = rowTint(s, s.surface, s.onSurface, checked = true, flash = 0f)
            val flashing = rowTint(s, s.surface, s.onSurface, checked = true, flash = 1f)
            listOfNotNull(
                need("switched-on row text", on.content, on.container, 4.5),
                need("flashing row text", flashing.content, flashing.container, 4.5),
            ).joinToString().ifEmpty { null }
        }
        assertTrue(bad.isEmpty(), bad.joinToString("\n"))
        assertTrue(classic.isEmpty(), "Classic: " + classic.joinToString("\n"))
    }

    @Test
    fun defaultPaletteMeetsContrastIncludingTertiary() {
        for (dark in listOf(false, true)) {
            val s = fadenColorScheme(dark, black = false, seed = null, extra = false)
            val mode = if (dark) "dark" else "light"
            val checks = listOfNotNull(
                need("onPrimary/primary", s.onPrimary, s.primary, 4.5),
                need("onPrimaryContainer/primaryContainer", s.onPrimaryContainer, s.primaryContainer, 4.5),
                need("onTertiary/tertiary", s.onTertiary, s.tertiary, 4.5),
                need("onTertiaryContainer/tertiaryContainer", s.onTertiaryContainer, s.tertiaryContainer, 4.5),
                need("onSurface/surfaceContainerLow", s.onSurface, s.surfaceContainerLow, 7.0),
                need("onSurfaceVariant/surfaceContainerLow", s.onSurfaceVariant, s.surfaceContainerLow, 4.5),
                need("primary/surfaceContainerLow", s.primary, s.surfaceContainerLow, 3.0),
            )
            assertTrue(checks.isEmpty(), "default $mode: " + checks.joinToString("; "))
        }
    }

    /** A downvoted score must not read as accent text: the Downvote color keeps its distance from the default primary. */
    @Test
    fun downvoteIsDistinctFromThePrimary() {
        for (dark in listOf(false, true)) {
            val s = fadenColorScheme(dark, black = false, seed = null, extra = false)
            val mode = if (dark) "dark" else "light"
            val distance = Tones.deltaEOk(FadenColors.Downvote.argb(), s.primary.argb())
            assertTrue(distance >= 0.08, "downvote vs primary ($mode): ΔE_ok %.3f".format(distance))
            // 4.5:1 on a dark card, 3:1 (non-text contrast: a vote tint beside an icon) on the light card.
            val onCard = ratio(FadenColors.Downvote, s.surfaceContainerLow)
            assertTrue(onCard >= if (dark) 4.5 else 3.0, "downvote on $mode card: %.2f".format(onCard))
        }
    }

    @Test
    fun depthRailsStandOutOnCardsAndWindow() {
        for (dark in listOf(false, true)) {
            val scheme = fadenColorScheme(dark, black = false, seed = null, extra = false)
            val mode = if (dark) "dark" else "light"
            FadenColors.Depth.forEachIndexed { depth, rail ->
                for ((name, ground) in listOf("card" to scheme.surfaceContainerLow, "window" to scheme.background)) {
                    val ratio = ratio(rail, ground)
                    assertTrue(ratio >= 3.0, "rail $depth vs $mode $name: %.2f".format(ratio))
                }
            }
        }
        assertTrue(FadenColors.Depth.size == 5)
    }

    @Test
    fun depthRailsAreDistinct() {
        val rails = FadenColors.Depth
        for (i in rails.indices) for (j in i + 1 until rails.size) {
            val d = Tones.deltaEOk(rails[i].argb(), rails[j].argb())
            assertTrue(d >= 0.08, "rails $i and $j: ΔE_ok %.3f".format(d))
        }
        // The first rail is the upvote orange's hue.
        val first = Tones.lchOf(rails[0].argb()).hue
        val orange = Tones.lchOf(FadenColors.Upvote.argb()).hue
        assertTrue(kotlin.math.abs(first - orange) < 10f, "first rail hue $first vs upvote $orange")
    }

    @Test
    fun fadenSeedMatchesDefaultScheme() {
        for (dark in listOf(false, true)) {
            val generated = ToneScheme.faden(dark)
            val default = fadenColorScheme(dark, black = false, seed = null, extra = false)
            assertTrue(generated.primary == default.primary.argb() && generated.background == default.background.argb())
        }
    }
}
