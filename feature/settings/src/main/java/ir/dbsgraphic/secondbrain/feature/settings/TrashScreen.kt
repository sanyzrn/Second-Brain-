package ir.dbsgraphic.secondbrain.feature.settings

import androidx.compose.foundation.background
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbHairline
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbText
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbTextButton
import ir.dbsgraphic.secondbrain.core.designsystem.theme.SecondBrainTheme

@Composable
fun TrashRoute(
    onBack: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    TrashScreen(
        items = items,
        onRestore = { viewModel.restore(it) },
        onDeleteForever = { viewModel.deleteForever(it) },
        onEmpty = { viewModel.emptyTrash() },
        onBack = onBack,
    )
}

@Composable
fun TrashScreen(
    items: List<Item>,
    onRestore: (String) -> Unit,
    onDeleteForever: (String) -> Unit,
    onEmpty: () -> Unit,
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
            SbText(text = "سطل بازیافت", style = type.title)
            SbTextButton(label = "بازگشت", onClick = onBack)
        }
        Spacer(Modifier.height(space.sm))
        SbText(
            text = "هیچ‌چیز برای همیشه پاک نمی‌شود مگر خودت بخواهی.",
            style = type.body,
            color = colors.muted,
        )

        Spacer(Modifier.height(space.lg))

        if (items.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                SbText(text = "سطل خالیه.", style = type.title)
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                SbTextButton(label = "خالی کردن سطل", onClick = onEmpty, color = colors.muted)
            }
            Spacer(Modifier.height(space.sm))
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = items, key = { it.id }) { item ->
                    TrashRow(
                        item = item,
                        onRestore = { onRestore(item.id) },
                        onDeleteForever = { onDeleteForever(item.id) },
                    )
                    SbHairline()
                }
            }
        }
    }
}

@Composable
private fun TrashRow(item: Item, onRestore: () -> Unit, onDeleteForever: () -> Unit) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = space.lg)) {
        SbText(text = item.content, style = type.bodyLarge)
        Spacer(Modifier.height(space.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(space.lg)) {
            SbTextButton(label = "بازگردانی", onClick = onRestore)
            SbTextButton(label = "حذف برای همیشه", onClick = onDeleteForever, color = colors.muted)
        }
    }
}
