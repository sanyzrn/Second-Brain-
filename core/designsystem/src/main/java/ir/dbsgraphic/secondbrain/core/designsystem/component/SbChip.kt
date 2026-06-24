package ir.dbsgraphic.secondbrain.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import ir.dbsgraphic.secondbrain.core.designsystem.theme.HairlineWidth
import ir.dbsgraphic.secondbrain.core.designsystem.theme.SecondBrainTheme

/**
 * A selectable chip — the choice control for type / project / tag. Selected
 * state fills with the accent; unselected is a quiet hairline outline. No
 * Material chip, so the look stays ours.
 */
@Composable
fun SbChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SecondBrainTheme.colors
    val shape = SecondBrainTheme.shapes.small
    val space = SecondBrainTheme.spacing

    val base = if (selected) {
        Modifier.background(colors.accent, shape)
    } else {
        Modifier.border(BorderStroke(HairlineWidth, colors.hairline), shape)
    }

    SbText(
        text = label,
        style = SecondBrainTheme.type.label,
        color = if (selected) colors.onAccent else colors.text,
        modifier = modifier
            .clip(shape)
            .then(base)
            .clickable(onClick = onClick)
            .padding(horizontal = space.md, vertical = space.sm),
    )
}

/** Quiet text-only action (e.g. back, secondary verbs). */
@Composable
fun SbTextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = SecondBrainTheme.colors.accent,
) {
    val space = SecondBrainTheme.spacing
    SbText(
        text = label,
        style = SecondBrainTheme.type.label,
        color = color,
        modifier = modifier
            .clip(SecondBrainTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = space.sm, vertical = space.xs),
    )
}
