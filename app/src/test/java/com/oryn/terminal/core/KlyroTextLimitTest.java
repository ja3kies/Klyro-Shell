package com.oryn.terminal.core;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class KlyroTextLimitTest {
    @Test public void limitCountsUnicodeCodePointsWithoutSplittingEmoji() {
        String wanted = "a".repeat(31) + "😀";
        assertEquals(wanted, KlyroTextLimit.takeCodePoints(wanted + "z", 32));
        assertEquals(32, KlyroTextLimit.codePointCount(wanted));
    }

    @Test public void zeroAndNegativeLimitsProduceEmptyText() {
        assertEquals("", KlyroTextLimit.takeCodePoints("Klyro😀", 0));
        assertEquals("", KlyroTextLimit.takeCodePoints("Klyro😀", -1));
    }
}
