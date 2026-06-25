package ir.dbsgraphic.secondbrain.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dbsgraphic.secondbrain.core.data.ThemeMode
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbChip
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbHairline
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbText
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbTextButton
import ir.dbsgraphic.secondbrain.core.designsystem.theme.SecondBrainTheme

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    SettingsScreen(
        themeMode = themeMode,
        onThemeModeChange = viewModel::setThemeMode,
        onOpenAbout = onOpenAbout,
        onBack = onBack,
    )
}

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onOpenAbout: () -> Unit,
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
            SbText(text = "تنظیمات", style = type.title)
            SbTextButton(label = "بازگشت", onClick = onBack)
        }
        Spacer(Modifier.height(space.xl))

        SbText(text = "پوسته", style = type.monoSmall, color = colors.muted)
        Spacer(Modifier.height(space.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(space.sm)) {
            ThemeOption("سیستم", themeMode == ThemeMode.SYSTEM) { onThemeModeChange(ThemeMode.SYSTEM) }
            ThemeOption("روشن", themeMode == ThemeMode.LIGHT) { onThemeModeChange(ThemeMode.LIGHT) }
            ThemeOption("تاریک", themeMode == ThemeMode.DARK) { onThemeModeChange(ThemeMode.DARK) }
        }

        Spacer(Modifier.height(space.xl))
        SbHairline()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenAbout)
                .padding(vertical = space.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SbText(text = "درباره", style = type.bodyLarge)
            SbText(text = "‹", style = type.title, color = colors.muted)
        }
        SbHairline()

        Spacer(Modifier.weight(1f))
        SbText(text = AppInfo.VERSION_LABEL, style = type.monoSmall, color = colors.muted)
        Spacer(Modifier.height(space.lg))
    }
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    SbChip(label = label, selected = selected, onClick = onClick)
}
