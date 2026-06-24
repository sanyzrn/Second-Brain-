package ir.dbsgraphic.secondbrain.feature.inbox

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbHairline
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbText
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbTextButton
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbTextField
import ir.dbsgraphic.secondbrain.core.designsystem.theme.SecondBrainTheme
import ir.dbsgraphic.secondbrain.core.designsystem.util.relativeTimeFa
import ir.dbsgraphic.secondbrain.core.designsystem.util.toPersianDigits

@Composable
fun InboxRoute(
    onOpenProjects: () -> Unit,
    viewModel: InboxViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                InboxEvent.Captured, InboxEvent.Triaged ->
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                is InboxEvent.Failed ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    InboxScreen(
        state = state,
        onDraftChange = viewModel::onDraftChange,
        onCapture = viewModel::capture,
        onItemClick = viewModel::openTriage,
        onOpenProjects = onOpenProjects,
        onDismissTriage = viewModel::dismissTriage,
        onConfirmTriage = viewModel::confirmTriage,
        onCreateProject = viewModel::createProject,
    )
}

@Composable
fun InboxScreen(
    state: InboxUiState,
    onDraftChange: (String) -> Unit,
    onCapture: () -> Unit,
    onItemClick: (Item) -> Unit,
    onOpenProjects: () -> Unit,
    onDismissTriage: () -> Unit,
    onConfirmTriage: (ItemType, String?, List<String>) -> Unit,
    onCreateProject: (String) -> Unit,
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
        InboxHeader(content = state.content, onOpenProjects = onOpenProjects)
        Spacer(Modifier.height(space.lg))

        Box(modifier = Modifier.weight(1f)) {
            when (val content = state.content) {
                InboxContent.Loading -> Unit // silent; first frame, no flash
                InboxContent.Empty -> InboxEmptyState()
                is InboxContent.Error -> SbText(
                    text = content.message,
                    style = type.body,
                    color = colors.muted,
                    modifier = Modifier.align(Alignment.Center),
                )
                is InboxContent.Items -> InboxList(content.items, onItemClick)
            }
        }

        QuickAddBar(
            draft = state.draft,
            canCapture = state.canCapture,
            isSaving = state.isSaving,
            onDraftChange = onDraftChange,
            onCapture = onCapture,
        )
        Spacer(Modifier.height(space.md))
    }

    // Triage surface, shown when an item is opened.
    state.triageTarget?.let { target ->
        TriageSheet(
            item = target,
            projects = state.projects,
            onDismiss = onDismissTriage,
            onConfirm = onConfirmTriage,
            onCreateProject = onCreateProject,
        )
    }
}

@Composable
private fun InboxHeader(content: InboxContent, onOpenProjects: () -> Unit) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SbText(text = "صندوق ورودی", style = type.title)
            if (content is InboxContent.Items) {
                SbText(
                    text = "  ${content.items.size.toString().toPersianDigits()} مورد",
                    style = type.caption,
                    color = colors.muted,
                )
            }
        }
        SbTextButton(label = "پروژه‌ها", onClick = onOpenProjects)
    }
}

@Composable
private fun InboxList(items: List<Item>, onItemClick: (Item) -> Unit) {
    val space = SecondBrainTheme.spacing
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = space.md),
    ) {
        items(items = items, key = { it.id }) { item ->
            // New captures materialize and reflow with a spatial animation;
            // triaged items animate out of the list.
            InboxItemRow(
                item = item,
                onClick = { onItemClick(item) },
                modifier = Modifier.animateItem(),
            )
            SbHairline()
        }
    }
}

@Composable
private fun InboxItemRow(item: Item, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = space.lg),
    ) {
        SbText(
            text = item.content,
            style = type.bodyLarge,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(space.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SbText(
                text = relativeTimeFa(item.createdAt),
                style = type.monoSmall,
                color = colors.muted,
            )
            Spacer(Modifier.width(space.sm))
            Box(
                Modifier
                    .height(4.dp)
                    .width(4.dp)
                    .clip(CircleShape)
                    .background(colors.muted),
            )
            Spacer(Modifier.width(space.sm))
            SbText(text = "ثبت‌نشده", style = type.monoSmall, color = colors.muted)
        }
    }
}

@Composable
private fun InboxEmptyState() {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
    ) {
        SbText(text = "صندوق خالیه.", style = type.title)
        Spacer(Modifier.height(space.sm))
        SbText(
            text = "اولین فکرت را همین پایین بنویس. بعداً مرتبش می‌کنیم.",
            style = type.body,
            color = colors.muted,
        )
    }
}

@Composable
private fun QuickAddBar(
    draft: String,
    canCapture: Boolean,
    isSaving: Boolean,
    onDraftChange: (String) -> Unit,
    onCapture: () -> Unit,
) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing

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
            onValueChange = onDraftChange,
            modifier = Modifier.weight(1f),
            placeholder = "چه چیزی در ذهنته؟",
            textStyle = type.body,
        )
        Spacer(Modifier.width(space.md))
        Box(
            modifier = Modifier
                .clip(SecondBrainTheme.shapes.medium)
                .background(if (canCapture) colors.accent else colors.hairline)
                .clickable(enabled = canCapture, onClick = onCapture)
                .padding(horizontal = space.lg, vertical = space.sm),
        ) {
            SbText(
                text = if (isSaving) "…" else "ثبت",
                style = type.label,
                color = if (canCapture) colors.onAccent else colors.muted,
            )
        }
    }
}

@Preview(showBackground = true, locale = "fa")
@Composable
private fun InboxEmptyPreview() {
    SecondBrainTheme {
        InboxScreen(
            state = InboxUiState(content = InboxContent.Empty),
            onDraftChange = {}, onCapture = {}, onItemClick = {}, onOpenProjects = {},
            onDismissTriage = {}, onConfirmTriage = { _, _, _ -> }, onCreateProject = {},
        )
    }
}

@Preview(showBackground = true, locale = "fa")
@Composable
private fun InboxItemsPreview() {
    SecondBrainTheme {
        InboxScreen(
            state = InboxUiState(
                content = InboxContent.Items(
                    listOf(
                        Item(
                            id = "1",
                            createdAt = System.currentTimeMillis() - 120_000,
                            updatedAt = 0,
                            content = "ایده‌ای برای صفحه‌ی شروع: نوار فرمان بالا باشد یا پایین؟",
                        ),
                        Item(
                            id = "2",
                            createdAt = System.currentTimeMillis() - 7_200_000,
                            updatedAt = 0,
                            content = "یادم باشد فردا با تیم طراحی هماهنگ کنم.",
                        ),
                    ),
                ),
                draft = "یک فکر تازه",
            ),
            onDraftChange = {}, onCapture = {}, onItemClick = {}, onOpenProjects = {},
            onDismissTriage = {}, onConfirmTriage = { _, _, _ -> }, onCreateProject = {},
        )
    }
}
