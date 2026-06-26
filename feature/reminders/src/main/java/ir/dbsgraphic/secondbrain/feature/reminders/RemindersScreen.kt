package ir.dbsgraphic.secondbrain.feature.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbHairline
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbText
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbTextButton
import ir.dbsgraphic.secondbrain.core.designsystem.theme.SecondBrainTheme
import ir.dbsgraphic.secondbrain.core.designsystem.util.JalaliDate

@Composable
fun RemindersRoute(
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    viewModel: RemindersViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    RemindersScreen(items = items, onOpenItem = onOpenItem, onClear = viewModel::clear, onBack = onBack)
}

@Composable
fun RemindersScreen(
    items: List<Item>,
    onOpenItem: (String) -> Unit,
    onClear: (String) -> Unit,
    onBack: () -> Unit,
) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = space.xl),
    ) {
        Spacer(Modifier.height(space.lg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SbText(text = "یادآوری‌ها", style = type.title)
            SbTextButton(label = "بازگشت", onClick = onBack)
        }
        Spacer(Modifier.height(space.lg))

        if (items.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                SbText(text = "یادآوری‌ای نداری.", style = type.title)
                Spacer(Modifier.height(space.sm))
                SbText(
                    text = "برای هر چیزی که نباید فراموش شود، از صفحه‌ی جزئیاتش یک یادآوری بگذار.",
                    style = type.body,
                    color = colors.muted,
                )
            }
            return@Column
        }

        val now = System.currentTimeMillis()
        val todayKey = JalaliDate.dayKey(now)
        val overdue = items.filter { (it.reminderAt ?: 0) < now }
        val today = items.filter { (it.reminderAt ?: 0) >= now && JalaliDate.dayKey(it.reminderAt ?: 0) == todayKey }
        val upcoming = items.filter { (it.reminderAt ?: 0) >= now && JalaliDate.dayKey(it.reminderAt ?: 0) != todayKey }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            section("گذشته", overdue, onOpenItem, onClear)
            section("امروز", today, onOpenItem, onClear)
            section("پیش‌رو", upcoming, onOpenItem, onClear)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    title: String,
    list: List<Item>,
    onOpenItem: (String) -> Unit,
    onClear: (String) -> Unit,
) {
    if (list.isEmpty()) return
    item(key = "header-$title") { SectionHeader(title) }
    items(items = list, key = { it.id }) { item ->
        ReminderRow(item = item, onOpen = { onOpenItem(item.id) }, onClear = { onClear(item.id) })
        SbHairline()
    }
}

@Composable
private fun SectionHeader(title: String) {
    val space = SecondBrainTheme.spacing
    SbText(
        text = title,
        style = SecondBrainTheme.type.monoSmall,
        color = SecondBrainTheme.colors.muted,
        modifier = Modifier.padding(top = space.lg, bottom = space.sm),
    )
}

@Composable
private fun ReminderRow(item: Item, onOpen: () -> Unit, onClear: () -> Unit) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).clickable(onClick = onOpen)) {
            SbText(text = item.content, style = type.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(space.xs))
            SbText(
                text = reminderText(item.reminderAt),
                style = type.monoSmall,
                color = colors.accent,
            )
        }
        SbTextButton(label = "حذف", onClick = onClear, color = colors.muted)
    }
}

private fun reminderText(ms: Long?): String {
    if (ms == null) return ""
    return "${JalaliDate.formatDate(ms)} · ${JalaliDate.formatTime(ms)}"
}
