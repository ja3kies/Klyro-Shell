package com.oryn.terminal.ui.motion

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** Decorative motion only: never change target size, visibility or action semantics. */
@Composable
internal fun Modifier.klyroPressScale(pressed: Boolean, enabled: Boolean = true): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (enabled && pressed) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "klyro-press-scale",
    )
    return graphicsLayer {
        scaleX = if (enabled) scale else 1f
        scaleY = if (enabled) scale else 1f
    }
}

/** Keep actionable cards visible and composed throughout their entrance. */
@Composable
internal fun Modifier.klyroStaggeredEntrance(index: Int, itemKey: String, enabled: Boolean): Modifier {
    if (!enabled || index >= 8) return this
    var entered by remember(itemKey) { mutableStateOf(false) }
    LaunchedEffect(itemKey) {
        delay(index.coerceAtLeast(0) * 35L)
        entered = true
    }
    val offset by animateFloatAsState(
        targetValue = if (entered) 0f else 7f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow),
        label = "klyro-card-entrance"
    )
    return graphicsLayer { translationY = offset.dp.toPx() }
}

/** Brief interpolation for decorative state chrome. Text and semantics update immediately. */
@Composable
internal fun klyroStateColor(target: Color, label: String): Color =
    animateColorAsState(target, animationSpec = tween(140), label = label).value
