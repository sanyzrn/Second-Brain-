package ir.dbsgraphic.secondbrain.feature.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import ir.dbsgraphic.secondbrain.core.designsystem.R as DsR
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbCard
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbChip
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbHairline
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbText
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbTextButton
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbTextField
import ir.dbsgraphic.secondbrain.core.designsystem.theme.SecondBrainTheme
import ir.dbsgraphic.secondbrain.core.designsystem.util.toPersianDigits

private enum class Mode { SEARCH, ASK }

@Composable
fun SearchRoute(
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val aiReady by viewModel.aiReady.collectAsStateWithLifecycle()
    val ask by viewModel.ask.collectAsStateWithLifecycle()

    SearchScreen(
        query = query,
        results = results,
        aiReady = aiReady,
        ask = ask,
        onQueryChange = viewModel::onQueryChange,
        onAskQuestionChange = viewModel::onAskQuestionChange,
        onRunAsk = viewModel::runAsk,
        onOpenItem = onOpenItem,
        onBack = onBack,
    )
}

@Composable
fun SearchScreen(
    query: String,
    results: List<Item>,
    aiReady: Boolean,
    ask: AskState,
    onQueryChange: (String) -> Unit,
    onAskQuestionChange: (String) -> Unit,
    onRunAsk: () -> Unit,
    onOpenItem: (String) -> Unit,
    onBack: () -> Unit,
) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing
    var mode by remember { mutableStateOf(Mode.SEARCH) }

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
            SbText(text = if (mode == Mode.SEARCH) "جستجو" else "از مغزت بپرس", style = type.title)
            SbTextButton(label = "بستن", onClick = onBack)
        }
        Spacer(Modifier.height(space.md))

        Row(horizontalArrangement = Arrangement.spacedBy(space.sm)) {
            SbChip(label = "جستجو", selected = mode == Mode.SEARCH, onClick = { mode = Mode.SEARCH })
            SbChip(label = "بپرس", selected = mode == Mode.ASK, onClick = { mode = Mode.ASK })
        }
        Spacer(Modifier.height(space.lg))

        when (mode) {
            Mode.SEARCH -> SearchMode(query, results, onQueryChange, onOpenItem)
            Mode.ASK -> AskMode(aiReady, ask, onAskQuestionChange, onRunAsk, onOpenItem)
        }
    }
}

@Composable
private fun CommandBar(
    value: String,
    placeholder: String,
    imeAction: ImeAction,
    onValueChange: (String) -> Unit,
    onAction: () -> Unit = {},
) {
    val colors = SecondBrainTheme.colors
    val space = SecondBrainTheme.spacing
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
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = placeholder,
            textStyle = SecondBrainTheme.type.body,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            keyboardActions = KeyboardActions(onSearch = { onAction() }, onSend = { onAction() }, onDone = { onAction() }),
        )
    }
}

@Composable
private fun ColumnScope.SearchMode(
    query: String,
    results: List<Item>,
    onQueryChange: (String) -> Unit,
    onOpenItem: (String) -> Unit,
) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing

    CommandBar(value = query, placeholder = "جستجو در همه‌چیز…", imeAction = ImeAction.Search, onValueChange = onQueryChange)
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
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(items = results, key = { it.id }) { item ->
                    ResultRow(item, onClick = { onOpenItem(item.id) })
                    SbHairline()
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.AskMode(
    aiReady: Boolean,
    ask: AskState,
    onQuestionChange: (String) -> Unit,
    onRunAsk: () -> Unit,
    onOpenItem: (String) -> Unit,
) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing

    if (!aiReady) {
        SbCard {
            SbText(text = "دستیار هوشمند خاموش است.", style = type.bodyLarge)
            Spacer(Modifier.height(space.sm))
            SbText(
                text = "برای پرسش از یادداشت‌هایت، از تنظیمات ← دستیار هوشمند روشنش کن.",
                style = type.body,
                color = colors.muted,
            )
        }
        return
    }

    CommandBar(
        value = ask.question,
        placeholder = "از یادداشت‌هایت بپرس…",
        imeAction = ImeAction.Send,
        onValueChange = onQuestionChange,
        onAction = onRunAsk,
    )
    Spacer(Modifier.height(space.md))
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        SbTextButton(label = if (ask.loading) "…" else "بپرس", onClick = onRunAsk)
    }
    Spacer(Modifier.height(space.md))

    ask.answer?.let { answer ->
        SbCard {
            SbText(text = answer, style = type.bodyLarge)
        }
        if (ask.sources.isNotEmpty()) {
            Spacer(Modifier.height(space.lg))
            SbText(text = "بر پایه‌ی:", style = type.monoSmall, color = colors.muted)
            Spacer(Modifier.height(space.sm))
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(items = ask.sources, key = { it.id }) { item ->
                    ResultRow(item, onClick = { onOpenItem(item.id) })
                    SbHairline()
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.Hint(text: String) {
    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
        SbText(
            text = text,
            style = SecondBrainTheme.type.body,
            color = SecondBrainTheme.colors.muted,
        )
    }
}

@Composable
private fun ResultRow(item: Item, onClick: () -> Unit) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = space.lg),
    ) {
        SbText(text = item.content, style = type.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
