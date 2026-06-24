package ir.dbsgraphic.secondbrain.feature.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dbsgraphic.secondbrain.core.designsystem.R as DsR
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbHairline
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbText
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbTextButton
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbTextField
import ir.dbsgraphic.secondbrain.core.designsystem.theme.SecondBrainTheme
import ir.dbsgraphic.secondbrain.core.designsystem.util.toPersianDigits

@Composable
fun SearchRoute(
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    SearchScreen(
        query = query,
        results = results,
        onQueryChange = viewModel::onQueryChange,
        onBack = onBack,
    )
}

@Composable
fun SearchScreen(
    query: String,
    results: List<Item>,
    onQueryChange: (String) -> Unit,
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
            SbText(text = "جستجو", style = type.title)
            SbTextButton(label = "بستن", onClick = onBack)
        }
        Spacer(Modifier.height(space.md))

        // The command bar — mono "instrument" voice, accent caret.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SecondBrainTheme.shapes.large)
                .background(colors.surface)
                .padding(horizontal = space.lg, vertical = space.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(DsR.drawable.ic_search),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colors.muted),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(space.md))
            SbTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = "جستجو در همه‌چیز…",
                textStyle = type.body,
                singleLine = true,
            )
        }

        Spacer(Modifier.height(space.lg))

        when {
            query.isBlank() -> Hint("هر چیزی را که ثبت کرده‌ای، اینجا پیدا کن.")
            results.isEmpty() -> Hint("چیزی پیدا نشد.")
            else -> {
                SbText(
                    text = "${results.size.toString().toPersianDigits()} نتیجه",
                    style = type.monoSmall,
                    color = colors.muted,
                )
                Spacer(Modifier.height(space.sm))
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = results, key = { it.id }) { item ->
                        ResultRow(item)
                        SbHairline()
                    }
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        SbText(
            text = text,
            style = SecondBrainTheme.type.body,
            color = SecondBrainTheme.colors.muted,
        )
    }
}

@Composable
private fun ResultRow(item: Item) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = space.lg)) {
        SbText(text = item.content, style = type.bodyLarge)
        Spacer(Modifier.height(space.xs))
        SbText(text = typeLabelFa(item.type), style = type.monoSmall, color = colors.accentSecondary)
    }
}

private fun typeLabelFa(type: String?): String = when (type) {
    "task" -> "کار"
    "note" -> "یادداشت"
    "idea" -> "ایده"
    "doc" -> "سند"
    else -> "ثبت‌نشده"
}
