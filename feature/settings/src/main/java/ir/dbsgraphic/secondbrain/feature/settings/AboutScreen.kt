package ir.dbsgraphic.secondbrain.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ir.dbsgraphic.secondbrain.core.designsystem.R as DsR
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbHairline
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbText
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbTextButton
import ir.dbsgraphic.secondbrain.core.designsystem.theme.SecondBrainTheme

@Composable
fun AboutRoute(onBack: () -> Unit) {
    AboutScreen(onBack = onBack)
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing
    val context = LocalContext.current

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
            SbText(text = "درباره", style = type.title)
            SbTextButton(label = "بازگشت", onClick = onBack)
        }

        Spacer(Modifier.height(space.xxxl))

        // Two marks side by side: the Second Brain mark and the DBS logo.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(DsR.drawable.ic_brand_mark),
                contentDescription = "مغز دوم",
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.width(space.lg))
            Image(
                painter = painterResource(R.drawable.dbs_logo),
                contentDescription = "DBS",
                modifier = Modifier.size(56.dp),
            )
        }

        Spacer(Modifier.height(space.xl))
        SbText(text = "مغز دوم", style = type.title)
        Spacer(Modifier.height(space.xs))
        SbText(text = AppInfo.VERSION_LABEL, style = type.monoSmall, color = colors.muted)

        Spacer(Modifier.height(space.xl))
        SbHairline()
        Spacer(Modifier.height(space.lg))

        SbText(text = AppInfo.DEVELOPED_BY, style = type.body)
        Spacer(Modifier.height(space.lg))

        InfoRow(label = "ایمیل", value = AppInfo.EMAIL) {
            context.runCatching {
                startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${AppInfo.EMAIL}")))
            }
        }
        Spacer(Modifier.height(space.md))
        InfoRow(label = "تلفن", value = AppInfo.PHONE) {
            context.runCatching {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${AppInfo.PHONE}")))
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, onClick: () -> Unit) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = space.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SbText(text = label, style = type.body, color = colors.muted)
        // Contact details are data → mono voice, Latin digits.
        SbText(text = value, style = type.mono, color = colors.accent)
    }
}
