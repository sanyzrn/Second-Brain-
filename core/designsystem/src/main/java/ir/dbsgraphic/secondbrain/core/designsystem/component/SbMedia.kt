package ir.dbsgraphic.secondbrain.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ir.dbsgraphic.secondbrain.core.designsystem.R
import ir.dbsgraphic.secondbrain.core.designsystem.theme.SecondBrainTheme
import java.io.File

/** Renders a local image file (Coil handles async load + downsampling). */
@Composable
fun SbImage(
    path: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    AsyncImage(
        model = File(path),
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale,
    )
}

/**
 * A small leading media indicator for list rows: an image thumbnail, a voice
 * glyph, or nothing for plain text. Takes primitives so the design system stays
 * free of the data layer.
 */
@Composable
fun SbMediaThumb(
    contentType: String,
    blobRef: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val colors = SecondBrainTheme.colors
    val shape = SecondBrainTheme.shapes.small
    when {
        contentType == "image" && !blobRef.isNullOrBlank() ->
            SbImage(path = blobRef, modifier = modifier.size(size).clip(shape))

        contentType == "voice" ->
            Box(
                modifier = modifier.size(size).clip(shape).background(colors.surface),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_mic),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colors.accent),
                    modifier = Modifier.size(size * 0.5f),
                )
            }
    }
}

fun hasThumb(contentType: String): Boolean = contentType == "image" || contentType == "voice"
