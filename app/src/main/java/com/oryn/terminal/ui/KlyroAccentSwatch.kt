package com.oryn.terminal.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.oryn.terminal.R
import com.oryn.terminal.rememberHomeMotionEnabled
import kotlin.math.cos
import kotlin.math.sin

/** Darken the actual hue, not a neutral foreground. Keep the state indicator legible. */
internal fun accentGlyphTint(accent: Color): Color {
    var factor = .65f
    fun shade() = Color(accent.red * factor, accent.green * factor, accent.blue * factor)
    while (factor > .025f && (accent.luminance() + .05f) /
        (shade().luminance() + .05f) < 3.15f) {
        factor -= .025f
    }
    return shade()
}

/**
 * Independent Compose adaptation of Calcula's palette-swatch interaction:
 * a rounded diamond settles into a rounded square, with an expanding tonal ring.
 * Only the painted surface rotates; the accessible 48dp target never moves.
 */
@Composable
internal fun KlyroAccentSwatch(
    accent: Color,
    active: Boolean,
    description: String,
    onClick: () -> Unit
) {
    val motion = rememberHomeMotionEnabled()
    val selection by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = if (motion) tween(300, easing = FastOutSlowInEasing) else snap(),
        label = "accent-swatch-selection"
    )
    val tile = LocalKlyroColors.current.tile
    Box(
        modifier = Modifier.size(KlyroUiMetrics.minTouch)
            .clickable(role = Role.RadioButton, onClickLabel = "Select $description") { onClick() }
            .semantics { selected = active; contentDescription = description }
            .testTag("accent-$description"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(KlyroUiMetrics.accentSwatch)
                .graphicsLayer { rotationZ = 45f * (1f - selection) }
                .drawWithCache {
                    val p = selection.coerceIn(0f, 1f)
                    val radius = size.minDimension * (.5f - .2f * p)
                    val inset = 2.dp.toPx() * p
                    val angle = -45f * (1f - p) * Math.PI / 180.0
                    val half = Offset(
                        (size.width * cos(angle) - size.height * sin(angle)).toFloat() * .5f,
                        (size.width * sin(angle) + size.height * cos(angle)).toFloat() * .5f
                    )
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val brush = Brush.linearGradient(
                        listOf(accent, lerp(accent, Color.White, .08f)),
                        start = center - half, end = center + half
                    )
                    onDrawBehind {
                        drawRoundRect(brush, cornerRadius = CornerRadius(radius))
                        if (inset > .01f) {
                            val gap = inset * .65f
                            fun dimensions(padding: Float) =
                                Size(size.width - 2 * padding, size.height - 2 * padding)
                            drawRoundRect(tile, topLeft = Offset(gap, gap),
                                size = dimensions(gap),
                                cornerRadius = CornerRadius((radius - gap).coerceAtLeast(0f)))
                            drawRoundRect(brush, topLeft = Offset(inset, inset),
                                size = dimensions(inset),
                                cornerRadius = CornerRadius((radius - inset).coerceAtLeast(0f)))
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(R.drawable.klyro_accent_shaked),
                contentDescription = null,
                tint = accentGlyphTint(accent),
                modifier = Modifier.size(18.dp).graphicsLayer {
                    alpha = selection
                    scaleX = .75f + .25f * selection
                    scaleY = scaleX
                    rotationZ = -45f * (1f - selection)
                }
            )
        }
    }
}
