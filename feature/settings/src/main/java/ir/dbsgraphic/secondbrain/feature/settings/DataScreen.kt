package ir.dbsgraphic.secondbrain.feature.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbCard
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbPrimaryButton
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbText
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbTextButton
import ir.dbsgraphic.secondbrain.core.designsystem.theme.SecondBrainTheme

@Composable
fun DataRoute(
    onBack: () -> Unit,
    viewModel: DataViewModel = hiltViewModel(),
) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(status) {
        status?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearStatus()
        }
    }

    DataScreen(
        onExport = viewModel::export,
        onImport = viewModel::import,
        onBack = onBack,
    )
}

@Composable
fun DataScreen(
    onExport: (android.net.Uri) -> Unit,
    onImport: (android.net.Uri) -> Unit,
    onBack: () -> Unit,
) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri -> uri?.let(onExport) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(onImport) }

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
            SbText(text = "پشتیبان‌گیری و انتقال", style = type.title)
            SbTextButton(label = "بازگشت", onClick = onBack)
        }
        Spacer(Modifier.height(space.lg))

        SbCard {
            SbText(text = "برون‌بری رمزگذاری‌شده", style = type.bodyLarge)
            Spacer(Modifier.height(space.sm))
            SbText(
                text = "یک فایل رمزگذاری‌شده از همه‌ی داده‌هایت بساز. داده‌ها متعلق به توست.",
                style = type.body,
                color = colors.muted,
            )
            Spacer(Modifier.height(space.md))
            SbPrimaryButton(
                label = "برون‌بری",
                onClick = { exportLauncher.launch("second-brain-backup.sbk") },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(space.lg))

        SbCard {
            SbText(text = "درون‌ریزی", style = type.bodyLarge)
            Spacer(Modifier.height(space.sm))
            SbText(
                text = "بازگردانی از یک فایل پشتیبان. با داده‌های فعلی ادغام می‌شود؛ چیزی پاک نمی‌شود.",
                style = type.body,
                color = colors.muted,
            )
            Spacer(Modifier.height(space.md))
            SbTextButton(
                label = "انتخاب فایل و بازگردانی",
                onClick = { importLauncher.launch(arrayOf("*/*")) },
            )
        }

        Spacer(Modifier.height(space.lg))
        SbText(
            text = "فایل پشتیبان با کلید همین دستگاه رمزگذاری می‌شود و فقط روی همین دستگاه باز می‌شود.",
            style = type.monoSmall,
            color = colors.muted,
        )
    }
}
