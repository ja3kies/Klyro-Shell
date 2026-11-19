package com.oryn.terminal.ui.motion

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Motion primitives shared across Klyro surfaces.
 * Keep terminal rendering, cursor and keyboard latency independent of decorative motion.
 */
object KlyroMotion {
    const val QUICK_MS = 120
    const val STANDARD_MS = 220
    const val EMPHASIS_MS = 320
    const val STAGGER_MS = 35
    const val MAX_STAGGER_ITEMS = 8
}

/**
 * Reusable spring feedback for pressable surfaces.
 *
 * Call with pressed state from a MutableInteractionSource; keep semantics and click handling
 * on the actual button or card. Motion is deliberately subtle to avoid visual noise.
 */
@Composable
fun Modifier.klyroPressScale(
    pressed: Boolean,
    enabled: Boolean = true,
): Modifier {
    val target = if (enabled && pressed) 0.975f else 1f
    val scale by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "klyro-press-scale",
    )
    return this.graphicsLayer {
        scaleX = if (enabled) scale else 1f
        scaleY = if (enabled) scale else 1f
    }
}

/** State-driven entrance without delaying composition or intercepting input. */
@Composable
fun KlyroReveal(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "klyro-reveal",
    )
    Box(
        modifier = modifier
            .alpha(progress)
            .graphicsLayer { translationY = (1f - progress) * 12f },
        content = content,
    )
}
