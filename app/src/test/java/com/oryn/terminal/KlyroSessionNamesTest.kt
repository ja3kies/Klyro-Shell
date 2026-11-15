package com.oryn.terminal

import org.junit.Assert.assertEquals
import org.junit.Test

class KlyroSessionNamesTest {
    @Test fun lastSessionClosedThenNewIdRestartsVisualNumbering() {
        val first = listOf(1L to null)
        assertEquals(listOf(1L to "Session 1"), KlyroSessionNames.display(first))
        assertEquals(emptyList<Pair<Long, String>>(), KlyroSessionNames.display(emptyList()))
        assertEquals(listOf(9L to "Session 1"),
            KlyroSessionNames.display(listOf(9L to null)))
    }

    @Test fun closingMiddleRenumbersOnlyDefaultNames() {
        val original = listOf(21L to null, 25L to null, 38L to null)
        assertEquals(listOf("Session 1", "Session 2", "Session 3"),
            KlyroSessionNames.display(original).map { it.second })
        assertEquals(listOf(21L to "Session 1", 38L to "Session 2"),
            KlyroSessionNames.display(original.filterNot { it.first == 25L }))
    }

    @Test fun oldBlankCustomNamesRenderAsDefaultAndDoNotAffectIds() {
        val saved = listOf(10L to " ", 99L to "Dev", 101L to null)
        assertEquals(listOf(10L to "Session 1", 99L to "Dev", 101L to "Session 3"),
            KlyroSessionNames.display(saved))
    }

    @Test fun renamesStayBoundToStableSessionId() {
        val original = listOf(4L to null, 6L to "Development", 8L to null)
        assertEquals(listOf(4L to "Session 1", 6L to "Development", 8L to "Session 3"),
            KlyroSessionNames.display(original))
        assertEquals(listOf(6L to "Development", 8L to "Session 2"),
            KlyroSessionNames.display(original.drop(1)))
    }
}
