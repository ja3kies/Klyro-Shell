package com.oryn.terminal
import org.junit.Assert.*
import org.junit.Test

class KlyroHomeRecentSessionTest {
    @Test fun lastSelectedTabIsTheOnlyResumeTargetEvenWithoutOutput() {
        assertEquals(HomeRecentSession(3L, "Development", "Projects", false),
            homeRecentSession(3L, listOf(1L to "First", 3L to "Development"), mapOf(3L to "p"), mapOf("p" to "Projects"), emptySet()))
    }
    @Test fun removedAndMissingTabsDoNotProduceResumeCards() {
        assertNull(homeRecentSession(3L, listOf(1L to "First"), emptyMap(), emptyMap(), emptySet()))
        assertNull(homeRecentSession(null, emptyList(), emptyMap(), emptyMap(), emptySet()))
    }
    @Test fun folderIsRelativeOnlyWhenInsideHome() {
        assertEquals("~/projects", homeDisplayDirectory("/data/home/projects", "/data/home"))
        assertEquals("/data/home2", homeDisplayDirectory("/data/home2", "/data/home"))
        assertNull(homeDisplayDirectory(null, "/data/home"))
    }
}
