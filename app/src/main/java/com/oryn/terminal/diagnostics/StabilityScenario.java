package com.oryn.terminal.diagnostics;

/** Only inserted into a newly created, explicitly requested diagnostic PTY. */
public final class StabilityScenario {
    private StabilityScenario() {}
    public static final String COMMAND = "sleep 3; printf 'P0-STRESS 1500 OUTPUT TEST START\\n'; " +
        "for ((klyro_i=1;klyro_i<=1500;klyro_i++)); do printf 'P0-STRESS %04d\\n' \"$klyro_i\"; done; " +
        "unset klyro_i; printf 'P0-STRESS 1500 OUTPUT TEST END\\n'\n";
}
