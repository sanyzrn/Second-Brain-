package ir.dbsgraphic.secondbrain.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import ir.dbsgraphic.secondbrain.MainActivity

/**
 * Home-screen widget: one tap opens the app on the Inbox so a thought can be
 * captured immediately (Constitution §2 — capturing faster than forgetting).
 * Pure-local; the widget holds no data.
 */
class SecondBrainWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { WidgetContent() }
    }
}

private val Pine = Color(0xFF1F6F5C)
private val Paper = Color(0xFFF3EEE5)

@Composable
private fun WidgetContent() {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Pine))
            .cornerRadius(20.dp)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = "مغز دوم",
            style = TextStyle(color = ColorProvider(Paper), fontWeight = FontWeight.Bold),
        )
        Text(
            text = "+ ثبت سریع",
            style = TextStyle(color = ColorProvider(Paper)),
        )
    }
}

class SecondBrainWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SecondBrainWidget()
}
