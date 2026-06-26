package ir.dbsgraphic.secondbrain.feature.settings

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import ir.dbsgraphic.secondbrain.core.data.ThemeMode
import ir.dbsgraphic.secondbrain.core.designsystem.R as DsR
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbCard
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbHairline
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbText
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbTextButton
import ir.dbsgraphic.secondbrain.core.designsystem.theme.SecondBrainTheme

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenAi: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenData: () -> Unit,
    onOpenCalendar: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    SettingsScreen(
        themeMode = themeMode,
        onThemeModeChange = viewModel::setThemeMode,
        onOpenAbout = onOpenAbout,
        onOpenAi = onOpenAi,
        onOpenTrash = onOpenTrash,
        onOpenData = onOpenData,
        onOpenCalendar = onOpenCalendar,
        onBack = onBack,
    )
}

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onOpenAbout: () -> Unit,
    onOpenAi: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenData: () -> Unit,
    onOpenCalendar: () -> Unit,
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = space.xl),
    ) {
        Spacer(Modifier.height(space.lg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SbText(text = "تنظیمات", style = type.title)
            SbTextButton(label = "بازگشت", onClick = onBack)
        }

        Spacer(Modifier.height(space.xl))

        // ── Appearance ────────────────────────────────────────────────────
        SectionLabel("ظاهر")
        Spacer(Modifier.height(space.sm))
        SbCard {
            SbText(text = "پوسته", style = type.body)
            Spacer(Modifier.height(space.md))
            ThemeSegmented(themeMode = themeMode, onChange = onThemeModeChange)
        }

        Spacer(Modifier.height(space.xl))

        // ── Data & ownership ──────────────────────────────────────────────
        SectionLabel("داده‌ها")
        Spacer(Modifier.height(space.sm))
        SbCard(padding = space.xs) {
            NavRow(label = "سطل بازیافت", subtitle = "بازگردانی هرچه حذف کرده‌ای", onClick = onOpenTrash)
            SbHairline(modifier = Modifier.padding(horizontal = space.md))
            NavRow(label = "پشتیبان‌گیری و انتقال", subtitle = "برون‌بری و درون‌ریزی رمزگذاری‌شده", onClick = onOpenData)
            SbHairline(modifier = Modifier.padding(horizontal = space.md))
            NavRow(label = "تقویم", subtitle = "هم‌گام‌سازی با تقویم دستگاه و فایل ICS", onClick = onOpenCalendar)
        }

        Spacer(Modifier.height(space.xl))

        // ── More ──────────────────────────────────────────────────────────
        SectionLabel("بیشتر")
        Spacer(Modifier.height(space.sm))
        SbCard(padding = space.xs) {
            NavRow(label = "دستیار هوشمند", subtitle = "اختیاری، پیش‌فرض خاموش", onClick = onOpenAi)
            SbHairline(modifier = Modifier.padding(horizontal = space.md))
            NavRow(label = "درباره", subtitle = "سازنده، نسخه و راه‌های تماس", onClick = onOpenAbout)
        }

        Spacer(Modifier.height(space.xxl))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            SbText(text = AppInfo.VERSION_LABEL, style = type.monoSmall, color = colors.muted)
        }
        Spacer(Modifier.height(space.xl))
    }
}

@Composable
private fun SectionLabel(text: String) {
    SbText(
        text = text,
        style = SecondBrainTheme.type.monoSmall,
        color = SecondBrainTheme.colors.muted,
        modifier = Modifier.padding(start = SecondBrainTheme.spacing.xs),
    )
}

/** Equal-width segmented control for the three theme modes. */
@Composable
private fun ThemeSegmented(themeMode: ThemeMode, onChange: (ThemeMode) -> Unit) {
    val colors = SecondBrainTheme.colors
    val space = SecondBrainTheme.spacing
    val options = listOf(ThemeMode.SYSTEM to "سیستم", ThemeMode.LIGHT to "روشن", ThemeMode.DARK to "تاریک")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SecondBrainTheme.shapes.medium)
            .background(colors.background)
            .padding(space.xs),
        horizontalArrangement = Arrangement.spacedBy(space.xs),
    ) {
        options.forEach { (mode, label) ->
            val selected = themeMode == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(SecondBrainTheme.shapes.small)
                    .background(if (selected) colors.accent else colors.background)
                    .clickable { onChange(mode) }
                    .padding(vertical = space.sm),
                contentAlignment = Alignment.Center,
            ) {
                SbText(
                    text = label,
                    style = SecondBrainTheme.type.label,
                    color = if (selected) colors.onAccent else colors.text,
                )
            }
        }
    }
}

@Composable
private fun NavRow(label: String, subtitle: String, onClick: () -> Unit) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SecondBrainTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = space.md, vertical = space.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            SbText(text = label, style = type.bodyLarge)
            Spacer(Modifier.height(space.xs))
            SbText(text = subtitle, style = type.monoSmall, color = colors.muted)
        }
        Image(
            painter = painterResource(DsR.drawable.ic_chevron),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.muted),
            modifier = Modifier.size(20.dp),
        )
    }
}
