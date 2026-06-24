package ir.dbsgraphic.secondbrain.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// ─── Pine Editorial palette (art-directed) ───────────────────────────────
// Light is warm paper with Deep Pine as the accent. Dark is warm ink where the
// signature warms to DBS Ember, with pine kept as a quiet secondary.

// Light — warm paper
internal val PaperBg = Color(0xFFF3EEE5)
internal val PaperSurface = Color(0xFFF8F3EA)
internal val PaperText = Color(0xFF221C17)
internal val PaperMuted = Color(0xFF776D60)
internal val PaperHairline = Color(0xFFD9CFC0)

// Dark — warm ink
internal val InkBg = Color(0xFF131212)
internal val InkSurface = Color(0xFF1F1D1C)
internal val InkText = Color(0xFFEBE6E1)
internal val InkMuted = Color(0xFF9A9187)
internal val InkHairline = Color(0xFF2A2624)

// Accents
internal val Pine = Color(0xFF1F6F5C)
internal val Ember = Color(0xFFD98E3C)

/**
 * Semantic color roles the UI reads from. Components reference these, never raw
 * hex — so a palette swap never touches feature code (Constitution §16).
 */
@Immutable
data class SbColors(
    val background: Color,
    val surface: Color,
    val text: Color,
    val muted: Color,
    val hairline: Color,
    val accent: Color,
    val accentSecondary: Color,
    val onAccent: Color,
    val isDark: Boolean,
)

val PineEditorialLight = SbColors(
    background = PaperBg,
    surface = PaperSurface,
    text = PaperText,
    muted = PaperMuted,
    hairline = PaperHairline,
    accent = Pine,
    accentSecondary = Pine,
    onAccent = PaperSurface,
    isDark = false,
)

val PineEditorialDark = SbColors(
    background = InkBg,
    surface = InkSurface,
    text = InkText,
    muted = InkMuted,
    hairline = InkHairline,
    accent = Ember,
    accentSecondary = Pine,
    onAccent = InkBg,
    isDark = true,
)
