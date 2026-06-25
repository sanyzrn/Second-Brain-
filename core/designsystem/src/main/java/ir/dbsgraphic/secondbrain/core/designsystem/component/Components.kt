package ir.dbsgraphic.secondbrain.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.unit.dp
import ir.dbsgraphic.secondbrain.core.designsystem.theme.HairlineWidth
import ir.dbsgraphic.secondbrain.core.designsystem.theme.SecondBrainTheme

/**
 * The one text primitive. Built on Foundation's BasicText (no Material), so
 * the type system is fully ours. Defaults to body style + ink color.
 */
@Composable
fun SbText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = SecondBrainTheme.type.body,
    color: Color = SecondBrainTheme.colors.text,
    align: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(TextStyle(color = color, textAlign = align ?: TextAlign.Unspecified)),
        maxLines = maxLines,
        overflow = overflow,
    )
}

/** Editorial hairline rule — the only divider we use. */
@Composable
fun SbHairline(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(HairlineWidth)
            .background(SecondBrainTheme.colors.hairline),
    )
}

/**
 * Primary action. The accent (Deep Pine) appears here and on the Timeline
 * spine — nowhere else. Calm, tonal, no heavy shadow.
 */
@Composable
fun SbPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SecondBrainTheme.colors
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.accent)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
    ) {
        SbText(
            text = label,
            style = SecondBrainTheme.type.label,
            color = colors.onAccent,
        )
    }
}
