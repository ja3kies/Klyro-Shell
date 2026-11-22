package com.oryn.terminal.ui

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.oryn.terminal.rememberHomeMotionEnabled

/** Animate accent changes within an appearance; snap across light/dark for readable contrast. */
@Composable
internal fun rememberAnimatedKlyroPalette(
    target: KlyroColors,
    motion: Boolean = rememberHomeMotionEnabled()
): KlyroColors {
    val transition = updateTransition(target, label = "Klyro accent")
    val background by transition.animateColor(transitionSpec = { tween(420) }, label = "background") { it.background }
    val chrome by transition.animateColor(transitionSpec = { tween(420) }, label = "chrome") { it.chrome }
    val key by transition.animateColor(transitionSpec = { tween(420) }, label = "key") { it.key }
    val sheet by transition.animateColor(transitionSpec = { tween(420) }, label = "sheet") { it.sheet }
    val tile by transition.animateColor(transitionSpec = { tween(420) }, label = "tile") { it.tile }
    val terminal by transition.animateColor(transitionSpec = { tween(420) }, label = "terminal-preview") { it.terminal }
    val accent by transition.animateColor(transitionSpec = { tween(420) }, label = "accent") { it.accent }
    if (!motion || transition.currentState.dark != target.dark ||
        transition.currentState.accentId.startsWith("mono-") || target.accentId.startsWith("mono-")) return target
    return target.copy(background = background, chrome = chrome, key = key,
        sheet = sheet, tile = tile, accent = accent, terminal = terminal)
}
