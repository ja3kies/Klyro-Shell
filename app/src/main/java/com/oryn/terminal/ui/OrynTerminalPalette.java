package com.oryn.terminal.ui;

import com.termux.terminal.TerminalColors;

import java.util.Properties;

/** Neon Obsidian palette applied before a terminal emulator is created. */
public final class OrynTerminalPalette {
    private OrynTerminalPalette() { }

    public static void apply() {
        Properties colors = new Properties();
        colors.setProperty("foreground", "#E2E8F0");
        colors.setProperty("background", "#0B0F19");
        colors.setProperty("cursor", "#00F0FF");
        colors.setProperty("color0", "#111827");
        colors.setProperty("color1", "#FF0055");
        colors.setProperty("color2", "#00FF66");
        colors.setProperty("color3", "#FEFFDF");
        colors.setProperty("color4", "#35C1C2");
        colors.setProperty("color5", "#8338EC");
        colors.setProperty("color6", "#00F0FF");
        colors.setProperty("color7", "#E2E8F0");
        colors.setProperty("color8", "#64748B");
        colors.setProperty("color9", "#FF4D7D");
        colors.setProperty("color10", "#4DFF9A");
        colors.setProperty("color11", "#FEFFDF");
        colors.setProperty("color12", "#7CE7E7");
        colors.setProperty("color13", "#A77BF2");
        colors.setProperty("color14", "#5AF7FF");
        colors.setProperty("color15", "#FFFFFF");
        TerminalColors.COLOR_SCHEME.updateWith(colors);
    }
}
