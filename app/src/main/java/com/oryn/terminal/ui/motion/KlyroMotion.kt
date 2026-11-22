package com.oryn.terminal.ui.motion

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.oryn.terminal.rememberHomeMotionEnabled
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

/** Brief, always-visible entrance. Only inner visual content moves; hit targets stay stable. */
@Composable
internal fun Modifier.klyroStaggeredEntrance(index: Int, itemKey: String): Modifier {
    val motion = rememberHomeMotionEnabled()
    var entered by remember(itemKey) { mutableStateOf(false) }
    val distance = with(LocalDensity.current) { 6.dp.toPx() }
    LaunchedEffect(itemKey, motion) {
        if (motion) delay(index.coerceIn(0, 5) * 24L)
        entered = true
    }
    val progress by animateFloatAsState(
        targetValue = if (!motion || entered) 1f else 0f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "klyro-visible-entrance"
    )
    return graphicsLayer { translationY = if (motion) (1f - progress) * distance else 0f }
}
