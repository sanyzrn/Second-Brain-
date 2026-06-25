package ir.dbsgraphic.secondbrain.feature.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import ir.dbsgraphic.secondbrain.core.ai.TriageSuggestion
import ir.dbsgraphic.secondbrain.core.data.ItemType
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import ir.dbsgraphic.secondbrain.core.database.entity.Project
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbCard
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbChip
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbPrimaryButton
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbText
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbTextButton
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbTextField
import ir.dbsgraphic.secondbrain.core.designsystem.theme.SecondBrainTheme

/**
 * Triage — where organizing happens, never at capture (Constitution §3, §20).
 * A bottom sheet keeps it in-place; on confirm the item is typed and moves out
 * of the Inbox. The sheet's behavior is Material's; the look is entirely ours.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriageSheet(
    item: Item,
    projects: List<Project>,
    suggestion: TriageSuggestion?,
    onDismiss: () -> Unit,
    onConfirm: (type: ItemType, projectId: String?, tags: List<String>) -> Unit,
    onCreateProject: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedType by remember { mutableStateOf<ItemType?>(null) }
    var selectedProjectId by remember { mutableStateOf<String?>(null) }
    var tags by remember { mutableStateOf(listOf<String>()) }
    var tagDraft by remember { mutableStateOf("") }
    var newProjectName by remember { mutableStateOf("") }
    var showNewProject by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        scrimColor = colors.background.copy(alpha = 0.6f),
        dragHandle = { SheetHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = space.xl)
                .padding(bottom = space.xxl),
        ) {
            SbText(text = item.content, style = type.bodyLarge, color = colors.text)

            // ── AI suggestion (only suggests; user taps to apply, §12) ─────
            val hasSuggestion = suggestion != null &&
                (suggestion.type != null || suggestion.projectName != null || suggestion.tags.isNotEmpty())
            if (hasSuggestion) {
                Spacer(Modifier.height(space.lg))
                SbCard(padding = space.md) {
                    SbText(text = "پیشنهاد دستیار", style = type.monoSmall, color = colors.muted)
                    Spacer(Modifier.height(space.xs))
                    SbText(text = suggestionSummary(suggestion!!), style = type.body)
                    Spacer(Modifier.height(space.sm))
                    SbTextButton(
                        label = "اعمال پیشنهاد",
                        onClick = {
                            suggestion.type?.let { t -> selectedType = ItemType.entries.find { it.value == t } }
                            suggestion.projectName?.let { name ->
                                selectedProjectId = projects.find { it.name == name }?.id
                            }
                            if (suggestion.tags.isNotEmpty()) {
                                tags = (tags + suggestion.tags).distinct()
                            }
                        },
                    )
                }
            }

            Spacer(Modifier.height(space.xl))

            // ── Type ──────────────────────────────────────────────────────
            SectionLabel("نوع")
            Spacer(Modifier.height(space.sm))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(space.sm),
            ) {
                ItemType.entries.forEach { t ->
                    SbChip(
                        label = t.label,
                        selected = selectedType == t,
                        onClick = { selectedType = t },
                    )
                }
            }

            Spacer(Modifier.height(space.xl))

            // ── Project ───────────────────────────────────────────────────
            SectionLabel("پروژه")
            Spacer(Modifier.height(space.sm))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(space.sm),
            ) {
                SbChip(
                    label = "بدون پروژه",
                    selected = selectedProjectId == null,
                    onClick = { selectedProjectId = null },
                )
                projects.forEach { p ->
                    SbChip(
                        label = p.name,
                        selected = selectedProjectId == p.id,
                        onClick = { selectedProjectId = p.id },
                    )
                }
                SbChip(
                    label = "+ پروژه جدید",
                    selected = false,
                    onClick = { showNewProject = !showNewProject },
                )
            }
            if (showNewProject) {
                Spacer(Modifier.height(space.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    InlineField(
                        value = newProjectName,
                        onValueChange = { newProjectName = it },
                        placeholder = "نام پروژه",
                        modifier = Modifier.weight(1f),
                        imeAction = ImeAction.Done,
                        onImeAction = {
                            onCreateProject(newProjectName)
                            newProjectName = ""
                            showNewProject = false
                        },
                    )
                    Spacer(Modifier.width(space.sm))
                    SbTextButton(
                        label = "بساز",
                        onClick = {
                            onCreateProject(newProjectName)
                            newProjectName = ""
                            showNewProject = false
                        },
                    )
                }
            }

            Spacer(Modifier.height(space.xl))

            // ── Tags ──────────────────────────────────────────────────────
            SectionLabel("برچسب‌ها")
            Spacer(Modifier.height(space.sm))
            InlineField(
                value = tagDraft,
                onValueChange = { tagDraft = it },
                placeholder = "برچسب بنویس و enter بزن",
                modifier = Modifier.fillMaxWidth(),
                imeAction = ImeAction.Done,
                onImeAction = {
                    val t = tagDraft.trim()
                    if (t.isNotEmpty() && t !in tags) tags = tags + t
                    tagDraft = ""
                },
            )
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(space.sm))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(space.sm),
                ) {
                    tags.forEach { tag ->
                        SbChip(
                            label = "# $tag  ×",
                            selected = true,
                            onClick = { tags = tags - tag },
                        )
                    }
                }
            }

            Spacer(Modifier.height(space.xxl))

            SbPrimaryButton(
                label = "ثبت و انتقال",
                onClick = {
                    selectedType?.let { onConfirm(it, selectedProjectId, tags) }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (selectedType == null) {
                Spacer(Modifier.height(space.sm))
                SbText(
                    text = "اول نوعش را انتخاب کن.",
                    style = type.monoSmall,
                    color = colors.muted,
                )
            }

            Spacer(Modifier.height(space.lg))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                SbTextButton(label = "حذف به سطل", onClick = onDelete, color = colors.muted)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    SbText(
        text = text,
        style = SecondBrainTheme.type.monoSmall,
        color = SecondBrainTheme.colors.muted,
    )
}

@Composable
private fun SheetHandle() {
    val colors = SecondBrainTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(SecondBrainTheme.shapes.small)
                .background(colors.hairline),
        )
    }
}

private fun suggestionSummary(s: TriageSuggestion): String {
    val parts = mutableListOf<String>()
    s.type?.let { parts += "نوع: ${typeLabelFa(it)}" }
    s.projectName?.let { parts += "پروژه: $it" }
    if (s.tags.isNotEmpty()) parts += "برچسب: ${s.tags.joinToString("، ")}"
    return if (parts.isEmpty()) "—" else parts.joinToString("  ·  ")
}

private fun typeLabelFa(value: String): String = when (value) {
    "note" -> "یادداشت"
    "task" -> "کار"
    "idea" -> "ایده"
    "doc" -> "سند"
    else -> value
}

/** A bordered single-line field for the sheet (project name, tags). */
@Composable
private fun InlineField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: () -> Unit = {},
) {
    val colors = SecondBrainTheme.colors
    val space = SecondBrainTheme.spacing
    Box(
        modifier = modifier
            .clip(SecondBrainTheme.shapes.medium)
            .background(colors.background)
            .padding(horizontal = space.md, vertical = space.md),
    ) {
        SbTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            keyboardActions = KeyboardActions(
                onDone = { onImeAction() },
                onGo = { onImeAction() },
            ),
        )
    }
}
