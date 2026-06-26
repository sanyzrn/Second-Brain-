package ir.dbsgraphic.secondbrain.feature.habits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbHairline
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbText
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbTextField
import ir.dbsgraphic.secondbrain.core.designsystem.theme.HairlineWidth
import ir.dbsgraphic.secondbrain.core.designsystem.theme.SecondBrainTheme
import ir.dbsgraphic.secondbrain.core.designsystem.util.toPersianDigits

@Composable
fun HabitsRoute(
    onOpenItem: (String) -> Unit,
    viewModel: HabitsViewModel = hiltViewModel(),
) {
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    HabitsScreen(
        habits = habits,
        onCreate = viewModel::create,
        onToggle = viewModel::toggle,
        onOpenItem = onOpenItem,
    )
}

@Composable
fun HabitsScreen(
    habits: List<HabitUi>,
    onCreate: (String) -> Unit,
    onToggle: (String) -> Unit,
    onOpenItem: (String) -> Unit,
) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            )
            .padding(horizontal = space.xl),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (habits.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                    SbText(text = "هنوز عادتی نساخته‌ای.", style = type.title)
                    Spacer(Modifier.height(space.sm))
                    SbText(
                        text = "یک کار کوچک روزانه را همین پایین اضافه کن و هر روز بزنش.",
                        style = type.body,
                        color = colors.muted,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = habits, key = { it.item.id }) { habit ->
                        HabitRow(
                            ui = habit,
                            onToggle = { onToggle(habit.item.id) },
                            onOpen = { onOpenItem(habit.item.id) },
                        )
                        SbHairline()
                    }
                }
            }
        }

        AddBar(onCreate = onCreate)
        Spacer(Modifier.height(space.md))
    }
}

@Composable
private fun HabitRow(ui: HabitUi, onToggle: () -> Unit, onOpen: () -> Unit) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val circle = Modifier.size(28.dp).clip(CircleShape)
        Box(
            modifier = if (ui.doneToday) {
                circle.background(colors.accent)
            } else {
                circle.border(BorderStroke(HairlineWidth, colors.hairline), CircleShape)
            }.clickable(onClick = onToggle),
        )
        Spacer(Modifier.width(space.md))
        Column(modifier = Modifier.weight(1f).clickable(onClick = onOpen)) {
            SbText(text = ui.item.content, style = type.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(space.xs))
            SbText(
                text = if (ui.streak > 0) "${ui.streak.toString().toPersianDigits()} روز پیاپی" else "بزن که شروع شه",
                style = type.monoSmall,
                color = if (ui.streak > 0) colors.accent else colors.muted,
            )
        }
    }
}

@Composable
private fun AddBar(onCreate: (String) -> Unit) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing
    var draft by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SecondBrainTheme.shapes.large)
            .background(colors.surface)
            .padding(horizontal = space.lg, vertical = space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SbTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.weight(1f),
            placeholder = "عادت جدید…",
            singleLine = true,
        )
        Spacer(Modifier.width(space.md))
        val canAdd = draft.isNotBlank()
        Box(
            modifier = Modifier
                .clip(SecondBrainTheme.shapes.medium)
                .background(if (canAdd) colors.accent else colors.hairline)
                .clickable(enabled = canAdd) { onCreate(draft); draft = "" }
                .padding(horizontal = space.lg, vertical = space.sm),
        ) {
            SbText(
                text = "افزودن",
                style = type.label,
                color = if (canAdd) colors.onAccent else colors.muted,
            )
        }
    }
}
