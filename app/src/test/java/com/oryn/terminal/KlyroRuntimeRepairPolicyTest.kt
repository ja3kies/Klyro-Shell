package com.oryn.terminal

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KlyroRuntimeRepairPolicyTest {
    @Test fun onlyVerifiedFailureWithoutActivePtysOffersRepair() {
        assertTrue(KlyroRuntimeRepairPolicy.canOffer(true, false, true, false, false, false))
        assertFalse(KlyroRuntimeRepairPolicy.canOffer(true, false, true, true, false, false))
        assertFalse(KlyroRuntimeRepairPolicy.canOffer(true, true, true, false, false, false))
        assertFalse(KlyroRuntimeRepairPolicy.canOffer(true, false, false, false, false, false))
        assertFalse(KlyroRuntimeRepairPolicy.canOffer(true, false, true, false, true, false))
        assertFalse(KlyroRuntimeRepairPolicy.canOffer(true, false, true, false, false, true))
    }
}
