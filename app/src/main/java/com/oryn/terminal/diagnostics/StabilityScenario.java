package com.oryn.terminal.diagnostics;

/** Only inserted into a newly created, explicitly requested diagnostic PTY. */
public final class StabilityScenario {
    private StabilityScenario() {}
    public static final String COMMAND = "sleep 5; printf 'P0-STRESS 1500 OUTPUT TEST START\\n'; " +
        "for ((klyro_i=1;klyro_i<=1500;klyro_i++)); do " +
        "printf 'P0-STRESS %04d\\n' \"$klyro_i\"; " +
        "if (( klyro_i % 25 == 0 )); then sleep 0.5; fi; done; " +
        "unset klyro_i; printf 'P0-STRESS 1500 OUTPUT TEST END\\n'\n";
}
