package ir.dbsgraphic.secondbrain.feature.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbText
import ir.dbsgraphic.secondbrain.core.designsystem.theme.SecondBrainTheme
import ir.dbsgraphic.secondbrain.core.designsystem.util.JalaliDate

@Composable
fun TimelineRoute(viewModel: TimelineViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    TimelineScreen(items)
}

private sealed interface TlRow {
    val key: String
    data class DayHeader(val label: String, override val key: String) : TlRow
    data class Entry(val item: Item) : TlRow {
        override val key: String get() = item.id
    }
}

private fun buildRows(items: List<Item>): List<TlRow> {
    val rows = mutableListOf<TlRow>()
    var lastDay: String? = null
    for (item in items) {
        val day = JalaliDate.dayKey(item.createdAt)
        if (day != lastDay) {
            rows += TlRow.DayHeader(JalaliDate.formatDayHeader(item.createdAt), "header-$day")
            lastDay = day
        }
        rows += TlRow.Entry(item)
    }
    return rows
}

@Composable
fun TimelineScreen(items: List<Item>) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing

    if (items.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = space.xl),
            verticalArrangement = Arrangement.Center,
        ) {
            SbText(text = "خط زمان تو هنوز خالیه.", style = type.title)
            Spacer(Modifier.height(space.sm))
            SbText(
                text = "هر چه ثبت کنی، همین‌جا به ترتیب زمان می‌نشیند.",
                style = type.body,
                color = colors.muted,
            )
        }
        return
    }

    val rows = remember(items) { buildRows(items) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            ),
        contentPadding = PaddingValues(horizontal = space.xl, vertical = space.md),
    ) {
        items(items = rows, key = { it.key }) { row ->
            when (row) {
                is TlRow.DayHeader -> DayHeaderRow(row.label)
                is TlRow.Entry -> EntryRow(row.item)
            }
        }
    }
}

@Composable
private fun DayHeaderRow(label: String) {
    val colors = SecondBrainTheme.colors
    val space = SecondBrainTheme.spacing
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Spine(big = true)
        Column(modifier = Modifier.padding(start = space.md, top = space.lg, bottom = space.sm)) {
            SbText(text = label, style = SecondBrainTheme.type.caption, color = colors.accent)
        }
    }
}

@Composable
private fun EntryRow(item: Item) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Spine(big = false)
        Column(modifier = Modifier.padding(start = space.md, top = space.sm, bottom = space.lg)) {
            SbText(text = item.content, style = type.bodyLarge)
            Spacer(Modifier.height(space.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SbText(
                    text = JalaliDate.formatTime(item.createdAt),
                    style = type.monoSmall,
                    color = colors.accentSecondary,
                )
                Spacer(Modifier.width(space.sm))
                SbText(text = typeLabelFa(item.type), style = type.monoSmall, color = colors.muted)
            }
        }
    }
}

/** The vertical pine spine + a node. The spine runs full-height so days connect. */
@Composable
private fun Spine(big: Boolean) {
    val colors = SecondBrainTheme.colors
    Box(
        modifier = Modifier.width(28.dp).fillMaxHeight(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(colors.accentSecondary),
        )
        val node = if (big) 13.dp else 8.dp
        Box(
            modifier = Modifier
                .padding(top = if (big) 16.dp else 8.dp)
                .size(node)
                .clip(CircleShape)
                .background(if (big) colors.accent else colors.accentSecondary),
        )
    }
}

private fun typeLabelFa(type: String?): String = when (type) {
    "task" -> "کار"
    "note" -> "یادداشت"
    "idea" -> "ایده"
    "doc" -> "سند"
    else -> "ثبت‌نشده"
}
