package ir.dbsgraphic.secondbrain.feature.project

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbChip
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbHairline
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbMediaThumb
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbText
import ir.dbsgraphic.secondbrain.core.designsystem.component.hasThumb
import ir.dbsgraphic.secondbrain.core.designsystem.theme.SecondBrainTheme
import ir.dbsgraphic.secondbrain.core.designsystem.util.relativeTimeFa
import ir.dbsgraphic.secondbrain.core.designsystem.util.toPersianDigits

@Composable
fun ProjectRoute(
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    viewModel: ProjectViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    ProjectScreen(
        state = state,
        onSelectTab = viewModel::selectTab,
        onOpenItem = onOpenItem,
        onTrash = { id ->
            viewModel.trash(id)
            Toast.makeText(context, "به سطل منتقل شد", Toast.LENGTH_SHORT).show()
        },
        onBack = onBack,
    )
}

@Composable
fun ProjectScreen(
    state: ProjectUiState,
    onSelectTab: (ProjectTab) -> Unit,
    onOpenItem: (String) -> Unit,
    onTrash: (String) -> Unit,
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
        TopRow(title = state.project?.name ?: "پروژه", onBack = onBack)
        Spacer(Modifier.height(space.xs))
        SbText(
            text = "${state.items.size.toString().toPersianDigits()} آیتم",
            style = type.caption,
            color = colors.muted,
        )

        Spacer(Modifier.height(space.lg))

        // Tabs — each derived by filtering the project's Items (§4).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(space.sm),
        ) {
            ProjectTab.entries.forEach { tab ->
                SbChip(
                    label = tab.label,
                    selected = state.tab == tab,
                    onClick = { onSelectTab(tab) },
                )
            }
        }

        Spacer(Modifier.height(space.lg))

        val visible = state.visibleItems
        if (visible.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
            ) {
                SbText(
                    text = "این بخش هنوز خالیه.",
                    style = type.body,
                    color = colors.muted,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = visible, key = { it.id }) { item ->
                    ProjectItemRow(
                        item = item,
                        onClick = { onOpenItem(item.id) },
                        onLongPress = { onTrash(item.id) },
                    )
                    SbHairline()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProjectItemRow(item: Item, onClick: () -> Unit, onLongPress: () -> Unit) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(vertical = space.lg),
        verticalAlignment = Alignment.Top,
    ) {
        if (hasThumb(item.contentType)) {
            SbMediaThumb(contentType = item.contentType, blobRef = item.blobRef)
            Spacer(Modifier.width(space.md))
        }
        Column(modifier = Modifier.weight(1f)) {
            SbText(text = item.content, style = type.bodyLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(space.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SbText(text = typeLabelFa(item.type), style = type.monoSmall, color = colors.accent)
                Spacer(Modifier.width(space.sm))
                SbText(
                    text = relativeTimeFa(item.createdAt),
                    style = type.monoSmall,
                    color = colors.muted,
                )
            }
        }
    }
}

private fun typeLabelFa(type: String?): String = when (type) {
    "task" -> "کار"
    "note" -> "یادداشت"
    "idea" -> "ایده"
    "doc" -> "سند"
    else -> "—"
}
