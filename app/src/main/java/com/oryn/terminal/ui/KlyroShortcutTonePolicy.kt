package com.oryn.terminal.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** Four states with a neutral default; paste is not an error/warning action. */
internal enum class KlyroShortcutTone { NEUTRAL, SELECT, PASTE }
internal data class KlyroShortcutVisual(val background: Color, val ink: Color)

internal object KlyroShortcutTonePolicy {
    fun resolve(
        palette: KlyroColors,
        tone: KlyroShortcutTone,
        state: ShortcutState
    ): KlyroShortcutVisual {
        val base = when (tone) {
            KlyroShortcutTone.NEUTRAL -> palette.key
            KlyroShortcutTone.SELECT -> lerp(palette.key, palette.accent, .14f)
            KlyroShortcutTone.PASTE -> palette.tile
        }
        return when (state) {
            ShortcutState.DEFAULT -> KlyroShortcutVisual(base, palette.foreground)
            ShortcutState.ACTIVE -> KlyroShortcutVisual(
                lerp(base, palette.accent, .25f), palette.foreground)
            ShortcutState.HOLD -> KlyroShortcutVisual(
                lerp(base, palette.foreground, .13f), palette.foreground)
            ShortcutState.INACTIVE -> KlyroShortcutVisual(
                palette.key.copy(alpha = .5f), palette.muted)
        }
    }
}
