package ir.dbsgraphic.secondbrain

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import ir.dbsgraphic.secondbrain.core.designsystem.R as DsR
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbText
import ir.dbsgraphic.secondbrain.core.designsystem.theme.SecondBrainTheme

/** The life-verticals reachable from the Trackers hub. */
enum class TrackerDest(val label: String, val subtitle: String) {
    HABITS("عادت‌ها", "کارهای کوچک روزانه و زنجیره‌شان"),
    FINANCE("هزینه‌ها", "خرج‌ها و اقساط، با سررسید"),
    MEDICINE("داروها", "برنامه‌ی مصرف، دوز و تمدید"),
    GOALS("هدف‌ها", "آنچه می‌خواهی به آن برسی"),
}

/**
 * The fourth home tab: a calm hub that gathers the life-verticals behind one
 * door, so the primary pager stays the system's spine (Timeline / Inbox /
 * Projects). Each card opens its tracker full-screen.
 */
@Composable
fun TrackersHub(onOpen: (TrackerDest) -> Unit) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = space.xl),
    ) {
        SbText(text = "پیگیری‌ها", style = type.title)
        Spacer(Modifier.height(space.xs))
        SbText(
            text = "چیزهایی که در طول زمان دنبال می‌کنی.",
            style = type.body,
            color = colors.muted,
        )
        Spacer(Modifier.height(space.lg))

        TrackerDest.entries.forEach { dest ->
            TrackerCard(dest = dest, onClick = { onOpen(dest) })
            Spacer(Modifier.height(space.md))
        }
        Spacer(Modifier.height(space.lg))
    }
}

@Composable
private fun TrackerCard(dest: TrackerDest, onClick: () -> Unit) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SecondBrainTheme.shapes.large)
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            SbText(text = dest.label, style = type.bodyLarge)
            Spacer(Modifier.height(space.xs))
            SbText(text = dest.subtitle, style = type.monoSmall, color = colors.muted)
        }
        Image(
            painter = painterResource(DsR.drawable.ic_chevron),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.muted),
            modifier = Modifier.size(20.dp),
        )
    }
}
