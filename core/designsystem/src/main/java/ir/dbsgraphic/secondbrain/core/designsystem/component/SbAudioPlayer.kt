package ir.dbsgraphic.secondbrain.core.designsystem.component

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ir.dbsgraphic.secondbrain.core.designsystem.R
import ir.dbsgraphic.secondbrain.core.designsystem.theme.SecondBrainTheme
import kotlinx.coroutines.delay

/** A compact play/pause + progress control for a local audio file. */
@Composable
fun SbAudioPlayer(path: String, modifier: Modifier = Modifier) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing

    var isPlaying by remember(path) { mutableStateOf(false) }
    var positionMs by remember(path) { mutableIntStateOf(0) }
    var durationMs by remember(path) { mutableIntStateOf(0) }
    val player = remember { MediaPlayer() }

    DisposableEffect(path) {
        runCatching {
            player.reset()
            player.setDataSource(path)
            player.prepare()
            durationMs = player.duration
        }
        player.setOnCompletionListener {
            isPlaying = false
            positionMs = 0
            runCatching { player.seekTo(0) }
        }
        onDispose { runCatching { player.release() } }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            positionMs = runCatching { player.currentPosition }.getOrDefault(0)
            delay(250)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(SecondBrainTheme.shapes.large)
            .background(colors.surface)
            .padding(horizontal = space.md, vertical = space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SbIconButton(
            icon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
            contentDescription = if (isPlaying) "توقف" else "پخش",
            onClick = {
                if (isPlaying) {
                    runCatching { player.pause() }
                    isPlaying = false
                } else {
                    runCatching { player.start() }
                    isPlaying = true
                }
            },
            tint = colors.accent,
        )
        Spacer(Modifier.width(space.sm))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(SecondBrainTheme.shapes.small)
                .background(colors.hairline),
        ) {
            val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(SecondBrainTheme.shapes.small)
                    .background(colors.accent),
            )
        }
        Spacer(Modifier.width(space.sm))
        SbText(
            text = "${formatMs(positionMs)} / ${formatMs(durationMs)}",
            style = type.monoSmall,
            color = colors.muted,
        )
    }
}

private fun formatMs(ms: Int): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
