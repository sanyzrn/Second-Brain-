package ir.dbsgraphic.secondbrain.feature.inbox

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import ir.dbsgraphic.secondbrain.core.designsystem.R as DsR
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbHairline
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbIconButton
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbText
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbTextField
import ir.dbsgraphic.secondbrain.core.designsystem.theme.SecondBrainTheme
import ir.dbsgraphic.secondbrain.core.designsystem.util.relativeTimeFa
import ir.dbsgraphic.secondbrain.core.designsystem.util.rememberReducedMotion

@Composable
fun InboxRoute(viewModel: InboxViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                InboxEvent.Captured, InboxEvent.Triaged ->
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                InboxEvent.Trashed ->
                    Toast.makeText(context, "به سطل منتقل شد", Toast.LENGTH_SHORT).show()
                is InboxEvent.Failed ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Photo capture via the system camera app (no CAMERA permission needed).
    var photoPath by remember { mutableStateOf<String?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) photoPath?.let(viewModel::capturePhoto)
        photoPath = null
    }

    // Voice capture.
    val recorder = remember { VoiceRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }
    var voicePath by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) { onDispose { recorder.release() } }

    fun beginRecording() {
        val file = createBlobFile(context, "m4a")
        voicePath = file.absolutePath
        isRecording = recorder.start(file.absolutePath)
        if (!isRecording) voicePath = null
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) beginRecording()
        else Toast.makeText(context, "برای ضبط صدا، اجازه لازم است", Toast.LENGTH_SHORT).show()
    }

    InboxScreen(
        state = state,
        isRecording = isRecording,
        onDraftChange = viewModel::onDraftChange,
        onCapture = viewModel::capture,
        onItemClick = viewModel::openTriage,
        onItemLongPress = { viewModel.trash(it.id) },
        onDismissTriage = viewModel::dismissTriage,
        onConfirmTriage = viewModel::confirmTriage,
        onCreateProject = viewModel::createProject,
        onTrash = viewModel::trashCurrent,
        onTakePhoto = {
            val file = createBlobFile(context, "jpg")
            photoPath = file.absolutePath
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            runCatching { cameraLauncher.launch(uri) }
        },
        onRecordToggle = {
            if (isRecording) {
                val ok = recorder.stop()
                isRecording = false
                if (ok) voicePath?.let(viewModel::captureVoice)
                voicePath = null
            } else if (hasAudioPermission(context)) {
                beginRecording()
            } else {
                micPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
    )
}

@Composable
fun InboxScreen(
    state: InboxUiState,
    isRecording: Boolean,
    onDraftChange: (String) -> Unit,
    onCapture: () -> Unit,
    onItemClick: (Item) -> Unit,
    onItemLongPress: (Item) -> Unit,
    onDismissTriage: () -> Unit,
    onConfirmTriage: (ItemType, String?, List<String>) -> Unit,
    onCreateProject: (String) -> Unit,
    onTrash: () -> Unit,
    onTakePhoto: () -> Unit,
    onRecordToggle: () -> Unit,
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
            when (val content = state.content) {
                InboxContent.Loading -> Unit
                InboxContent.Empty -> InboxEmptyState()
                is InboxContent.Error -> SbText(
                    text = content.message,
                    style = type.body,
                    color = colors.muted,
                    modifier = Modifier.align(Alignment.Center),
                )
                is InboxContent.Items -> InboxList(content.items, onItemClick, onItemLongPress)
            }
        }

        QuickAddBar(
            draft = state.draft,
            canCapture = state.canCapture,
            isSaving = state.isSaving,
            isRecording = isRecording,
            onDraftChange = onDraftChange,
            onCapture = onCapture,
            onTakePhoto = onTakePhoto,
            onRecordToggle = onRecordToggle,
        )
        Spacer(Modifier.height(space.md))
    }

    state.triageTarget?.let { target ->
        TriageSheet(
            item = target,
            projects = state.projects,
            suggestion = state.triageSuggestion,
            onDismiss = onDismissTriage,
            onConfirm = onConfirmTriage,
            onCreateProject = onCreateProject,
            onDelete = onTrash,
        )
    }
}

@Composable
private fun InboxList(
    items: List<Item>,
    onItemClick: (Item) -> Unit,
    onItemLongPress: (Item) -> Unit,
) {
    val space = SecondBrainTheme.spacing
    val reducedMotion = rememberReducedMotion()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = space.sm, bottom = space.md),
    ) {
        items(items = items, key = { it.id }) { item ->
            InboxItemRow(
                item = item,
                onClick = { onItemClick(item) },
                onLongClick = { onItemLongPress(item) },
                modifier = if (reducedMotion) Modifier else Modifier.animateItem(),
            )
            SbHairline()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InboxItemRow(
    item: Item,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing

    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = space.lg),
    ) {
        SbText(text = item.content, style = type.bodyLarge, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(space.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SbText(text = relativeTimeFa(item.createdAt), style = type.monoSmall, color = colors.muted)
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
    isRecording: Boolean,
    onDraftChange: (String) -> Unit,
    onCapture: () -> Unit,
    onTakePhoto: () -> Unit,
    onRecordToggle: () -> Unit,
) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SecondBrainTheme.shapes.large)
            .background(colors.surface)
            .padding(horizontal = space.lg, vertical = space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isRecording) {
            Box(
                Modifier
                    .height(10.dp)
                    .width(10.dp)
                    .clip(CircleShape)
                    .background(RecordRed),
            )
            Spacer(Modifier.width(space.md))
            SbText(text = "در حال ضبط…", style = type.body, modifier = Modifier.weight(1f))
            PillButton(label = "پایان", onClick = onRecordToggle)
            return@Row
        }

        SbTextField(
            value = draft,
            onValueChange = onDraftChange,
            modifier = Modifier.weight(1f),
            placeholder = "چه چیزی در ذهنته؟",
            textStyle = type.body,
        )
        Spacer(Modifier.width(space.sm))
        if (canCapture) {
            PillButton(label = if (isSaving) "…" else "ثبت", onClick = onCapture)
        } else {
            SbIconButton(icon = DsR.drawable.ic_mic, contentDescription = "ضبط صدا", onClick = onRecordToggle)
            SbIconButton(icon = DsR.drawable.ic_camera, contentDescription = "عکس", onClick = onTakePhoto)
        }
    }
}

@Composable
private fun PillButton(label: String, onClick: () -> Unit) {
    val colors = SecondBrainTheme.colors
    val space = SecondBrainTheme.spacing
    Box(
        modifier = Modifier
            .clip(SecondBrainTheme.shapes.medium)
            .background(colors.accent)
            .clickable(onClick = onClick)
            .padding(horizontal = space.lg, vertical = space.sm),
    ) {
        SbText(text = label, style = SecondBrainTheme.type.label, color = colors.onAccent)
    }
}

private val RecordRed = Color(0xFFC0563E)

@Preview(showBackground = true, locale = "fa")
@Composable
private fun InboxItemsPreview() {
    SecondBrainTheme {
        InboxScreen(
            state = InboxUiState(
                content = InboxContent.Items(
                    listOf(
                        Item(id = "1", createdAt = System.currentTimeMillis() - 120_000, updatedAt = 0, content = "ایده‌ای برای صفحه‌ی شروع"),
                        Item(id = "2", createdAt = System.currentTimeMillis() - 7_200_000, updatedAt = 0, content = "فردا با تیم طراحی هماهنگ کن."),
                    ),
                ),
                draft = "یک فکر تازه",
            ),
            isRecording = false,
            onDraftChange = {}, onCapture = {}, onItemClick = {}, onItemLongPress = {},
            onDismissTriage = {}, onConfirmTriage = { _, _, _ -> }, onCreateProject = {},
            onTrash = {}, onTakePhoto = {}, onRecordToggle = {},
        )
    }
}
