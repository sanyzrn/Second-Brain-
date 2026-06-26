package ir.dbsgraphic.secondbrain.feature.settings

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dbsgraphic.secondbrain.core.calendar.DeviceEvent
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbCard
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbHairline
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbPrimaryButton
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbText
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbTextButton
import ir.dbsgraphic.secondbrain.core.designsystem.theme.SecondBrainTheme
import ir.dbsgraphic.secondbrain.core.designsystem.util.JalaliDate

@Composable
fun CalendarRoute(
    onBack: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(status) {
        status?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearStatus()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.refresh() }

    CalendarScreen(
        state = state,
        onBack = onBack,
        onRequestPermission = {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
            )
        },
        onSelectCalendar = viewModel::selectCalendar,
        onMirror = viewModel::mirrorReminders,
        onImportEvent = viewModel::importEvent,
        onExportIcs = viewModel::exportIcs,
        onImportIcs = viewModel::importIcs,
    )
}

@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onBack: () -> Unit,
    onRequestPermission: () -> Unit,
    onSelectCalendar: (Long) -> Unit,
    onMirror: () -> Unit,
    onImportEvent: (DeviceEvent) -> Unit,
    onExportIcs: (Uri) -> Unit,
    onImportIcs: (Uri) -> Unit,
) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/calendar"),
    ) { uri -> uri?.let(onExportIcs) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(onImportIcs) }

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
            SbText(text = "تقویم", style = type.title)
            SbTextButton(label = "بازگشت", onClick = onBack)
        }
        Spacer(Modifier.height(space.lg))

        // ── Device calendar ───────────────────────────────────────────────
        SbCard {
            SbText(text = "هم‌گام‌سازی با تقویم دستگاه", style = type.bodyLarge)
            Spacer(Modifier.height(space.sm))

            if (!state.canWrite) {
                SbText(
                    text = "برای افزودن یادآوری‌ها به تقویم گوگل/دستگاه، اجازه‌ی دسترسی لازم است.",
                    style = type.body,
                    color = colors.muted,
                )
                Spacer(Modifier.height(space.md))
                SbPrimaryButton(
                    label = "اجازه دسترسی به تقویم",
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                SbText(
                    text = "تقویمی که یادآوری‌ها در آن نوشته شوند:",
                    style = type.body,
                    color = colors.muted,
                )
                Spacer(Modifier.height(space.sm))
                if (state.calendars.isEmpty()) {
                    SbText(text = "تقویم قابل‌نوشتنی پیدا نشد.", style = type.body, color = colors.muted)
                } else {
                    state.calendars.forEach { calendar ->
                        CalendarChoice(
                            name = calendar.displayName,
                            account = calendar.accountName,
                            selected = state.selectedCalendarId == calendar.id,
                            onClick = { onSelectCalendar(calendar.id) },
                        )
                    }
                    Spacer(Modifier.height(space.md))
                    SbPrimaryButton(
                        label = "نوشتن یادآوری‌ها در تقویم",
                        onClick = onMirror,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // ── Upcoming device events ────────────────────────────────────────
        if (state.upcoming.isNotEmpty()) {
            Spacer(Modifier.height(space.lg))
            SbCard {
                SbText(text = "رویدادهای پیش‌رو", style = type.bodyLarge)
                Spacer(Modifier.height(space.xs))
                SbText(text = "از تقویم دستگاه — برای افزودن به صندوق بزن.", style = type.monoSmall, color = colors.muted)
                Spacer(Modifier.height(space.sm))
                state.upcoming.take(20).forEachIndexed { index, event ->
                    if (index > 0) SbHairline()
                    EventRow(event = event, onAdd = { onImportEvent(event) })
                }
            }
        }

        // ── ICS portability ───────────────────────────────────────────────
        Spacer(Modifier.height(space.lg))
        SbCard {
            SbText(text = "فایل تقویم (ICS)", style = type.bodyLarge)
            Spacer(Modifier.height(space.sm))
            SbText(
                text = "برون‌بری همه‌ی یادآوری‌ها به یک فایل استاندارد، یا درون‌ریزی از فایل تقویم دیگر.",
                style = type.body,
                color = colors.muted,
            )
            Spacer(Modifier.height(space.md))
            SbPrimaryButton(
                label = "برون‌بری ICS",
                onClick = { exportLauncher.launch("second-brain.ics") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(space.sm))
            SbTextButton(
                label = "درون‌ریزی از فایل ICS",
                onClick = { importLauncher.launch(arrayOf("text/calendar", "application/octet-stream", "*/*")) },
            )
        }

        Spacer(Modifier.height(space.lg))
        SbText(
            text = "رویدادهای درون‌ریزی‌شده به‌صورت آیتم‌های صندوق با یادآوری اضافه می‌شوند.",
            style = type.monoSmall,
            color = colors.muted,
        )
        Spacer(Modifier.height(space.xl))
    }
}

@Composable
private fun CalendarChoice(name: String, account: String, selected: Boolean, onClick: () -> Unit) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SecondBrainTheme.shapes.medium)
            .background(if (selected) colors.accent else colors.background)
            .clickable(onClick = onClick)
            .padding(horizontal = space.md, vertical = space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            SbText(
                text = name,
                style = type.body,
                color = if (selected) colors.onAccent else colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (account.isNotBlank()) {
                SbText(
                    text = account,
                    style = type.monoSmall,
                    color = if (selected) colors.onAccent else colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
    Spacer(Modifier.height(space.sm))
}

@Composable
private fun EventRow(event: DeviceEvent, onAdd: () -> Unit) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            SbText(
                text = event.title.ifBlank { "(بدون عنوان)" },
                style = type.body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(space.xs))
            SbText(
                text = "${JalaliDate.formatDate(event.begin)} • ${JalaliDate.formatTime(event.begin)}",
                style = type.monoSmall,
                color = colors.muted,
            )
        }
        SbTextButton(label = "افزودن", onClick = onAdd)
    }
}
