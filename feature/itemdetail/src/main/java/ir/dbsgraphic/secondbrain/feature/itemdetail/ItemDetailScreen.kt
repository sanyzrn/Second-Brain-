package ir.dbsgraphic.secondbrain.feature.itemdetail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dbsgraphic.secondbrain.core.data.ItemType
import ir.dbsgraphic.secondbrain.core.data.TagsCodec
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import ir.dbsgraphic.secondbrain.core.database.entity.Project
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbCard
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbChip
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbPrimaryButton
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbText
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbTextButton
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbTextField
import ir.dbsgraphic.secondbrain.core.designsystem.theme.SecondBrainTheme
import ir.dbsgraphic.secondbrain.core.designsystem.util.JalaliDate

@Composable
fun ItemDetailRoute(
    onBack: () -> Unit,
    viewModel: ItemDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                DetailEvent.Saved -> Toast.makeText(context, "ذخیره شد", Toast.LENGTH_SHORT).show()
                DetailEvent.Restored -> Toast.makeText(context, "بازگردانده شد", Toast.LENGTH_SHORT).show()
                DetailEvent.Trashed -> { Toast.makeText(context, "به سطل منتقل شد", Toast.LENGTH_SHORT).show(); onBack() }
                DetailEvent.Deleted -> { Toast.makeText(context, "برای همیشه حذف شد", Toast.LENGTH_SHORT).show(); onBack() }
                is DetailEvent.Failed -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    ItemDetailScreen(
        state = state,
        onSave = viewModel::save,
        onTrash = viewModel::trash,
        onRestore = viewModel::restore,
        onDeleteForever = viewModel::deleteForever,
        onBack = onBack,
    )
}

@Composable
fun ItemDetailScreen(
    state: ItemDetailUiState,
    onSave: (String, ItemType?, String?, List<String>) -> Unit,
    onTrash: () -> Unit,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing
    val item = state.item

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = space.xl),
    ) {
        Spacer(Modifier.height(space.lg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SbText(text = "جزئیات", style = type.title)
            SbTextButton(label = "بازگشت", onClick = onBack)
        }
        Spacer(Modifier.height(space.lg))

        if (item == null) return@Column

        val trashed = item.status == "trashed"

        // Local editable state, re-initialized whenever the item identity changes.
        var content by remember(item.id) { mutableStateOf(item.content) }
        var selectedType by remember(item.id) { mutableStateOf(ItemType.fromValue(item.type)) }
        var selectedProjectId by remember(item.id) { mutableStateOf(item.projectId) }
        var tags by remember(item.id) { mutableStateOf(TagsCodec.decode(item.tags)) }
        var tagDraft by remember(item.id) { mutableStateOf("") }

        // ── Content ────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SecondBrainTheme.shapes.medium)
                .background(colors.surface)
                .padding(space.lg),
        ) {
            SbTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = "متن…",
                textStyle = type.bodyLarge,
            )
        }

        if (!trashed) {
            Spacer(Modifier.height(space.xl))
            SectionLabel("نوع")
            Spacer(Modifier.height(space.sm))
            ChipRow {
                ItemType.entries.forEach { t ->
                    SbChip(label = t.label, selected = selectedType == t, onClick = { selectedType = t })
                }
            }

            Spacer(Modifier.height(space.lg))
            SectionLabel("پروژه")
            Spacer(Modifier.height(space.sm))
            ChipRow {
                SbChip(label = "بدون پروژه", selected = selectedProjectId == null, onClick = { selectedProjectId = null })
                state.projects.forEach { p ->
                    SbChip(label = p.name, selected = selectedProjectId == p.id, onClick = { selectedProjectId = p.id })
                }
            }

            Spacer(Modifier.height(space.lg))
            SectionLabel("برچسب‌ها")
            Spacer(Modifier.height(space.sm))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SecondBrainTheme.shapes.medium)
                    .background(colors.surface)
                    .padding(horizontal = space.md, vertical = space.md),
            ) {
                SbTextField(
                    value = tagDraft,
                    onValueChange = { tagDraft = it },
                    placeholder = "برچسب بنویس و enter بزن",
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = {
                            val t = tagDraft.trim()
                            if (t.isNotEmpty() && t !in tags) tags = tags + t
                            tagDraft = ""
                        },
                    ),
                )
            }
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(space.sm))
                ChipRow {
                    tags.forEach { tag ->
                        SbChip(label = "# $tag  ×", selected = true, onClick = { tags = tags - tag })
                    }
                }
            }
        }

        Spacer(Modifier.height(space.xl))
        MetadataCard(item)

        Spacer(Modifier.height(space.xl))
        if (trashed) {
            SbPrimaryButton(label = "بازگردانی", onClick = onRestore, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(space.md))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                SbTextButton(label = "حذف برای همیشه", onClick = onDeleteForever, color = colors.muted)
            }
        } else {
            SbPrimaryButton(
                label = "ذخیره",
                onClick = { onSave(content, selectedType, selectedProjectId, tags) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(space.md))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                SbTextButton(label = "حذف به سطل", onClick = onTrash, color = colors.muted)
            }
        }
        Spacer(Modifier.height(space.xxl))
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(SecondBrainTheme.spacing.sm),
    ) { content() }
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
private fun MetadataCard(item: Item) {
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing
    SbCard {
        MetaRow("ساخته‌شده", "${JalaliDate.formatDate(item.createdAt)} · ${JalaliDate.formatTime(item.createdAt)}")
        Spacer(Modifier.height(space.sm))
        MetaRow("آخرین تغییر", "${JalaliDate.formatDate(item.updatedAt)} · ${JalaliDate.formatTime(item.updatedAt)}")
        Spacer(Modifier.height(space.sm))
        MetaRow("ثبت از", capturedViaFa(item.capturedVia))
        if (item.contentType != "text") {
            Spacer(Modifier.height(space.sm))
            MetaRow("نوع محتوا", contentTypeFa(item.contentType))
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SbText(text = label, style = type.body, color = colors.muted)
        SbText(text = value, style = type.monoSmall, color = colors.text)
    }
}

private fun capturedViaFa(v: String): String = when (v) {
    "quickAdd" -> "ثبت سریع"
    "share" -> "اشتراک‌گذاری"
    "voice" -> "ضبط صدا"
    "photo" -> "دوربین"
    "widget" -> "ویجت"
    else -> v
}

private fun contentTypeFa(v: String): String = when (v) {
    "image" -> "تصویر"
    "voice" -> "صدا"
    "file" -> "فایل"
    "link" -> "لینک"
    "pdf" -> "PDF"
    "location" -> "موقعیت"
    else -> "متن"
}
