package com.oryn.terminal.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TerminalRuntimeWarningUiTest {
    @get:Rule val ui = createComposeRule()

    @Test fun warningHasOneClearRouteAndASeparateDismissAction() {
        var reviews = 0
        var dismissals = 0
        setBanner(width = 412.dp, fontScale = 1f,
            onReview = { reviews++ }, onDismiss = { dismissals++ })

        ui.onNodeWithTag("terminal-runtime-warning").assertIsDisplayed()
        ui.onNodeWithText("Environment checks need attention").assertIsDisplayed()
        ui.onNodeWithText("One or more shell checks need a closer look.").assertIsDisplayed()
        val review = ui.onNodeWithText("Review diagnostics")
        val dismiss = ui.onNodeWithText("Dismiss")
        review.assertIsDisplayed().assertHasClickAction().assertHeightIsAtLeast(48.dp)
        dismiss.assertIsDisplayed().assertHasClickAction().assertHeightIsAtLeast(48.dp)
        review.assertWidthIsAtLeast(48.dp)
        dismiss.assertWidthIsAtLeast(48.dp)
        assertEquals(Role.Button, review.fetchSemanticsNode().config[SemanticsProperties.Role])
        assertEquals(Role.Button, dismiss.fetchSemanticsNode().config[SemanticsProperties.Role])
        assertEquals(LiveRegionMode.Polite,
            ui.onNodeWithTag("terminal-runtime-warning")
                .fetchSemanticsNode().config[SemanticsProperties.LiveRegion])

        review.performClick()
        ui.runOnIdle { assertEquals(1, reviews); assertEquals(0, dismissals) }
        dismiss.performClick()
        ui.runOnIdle { assertEquals(1, reviews); assertEquals(1, dismissals) }
    }

    @Test fun narrowLargeTextKeepsCopyAndBothActionsVisible() {
        setBanner(width = 320.dp, fontScale = 2f)

        val card = ui.onNodeWithTag("terminal-runtime-warning")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val title = ui.onNodeWithText("Environment checks need attention")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val detail = ui.onNodeWithText("One or more shell checks need a closer look.")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val review = ui.onNodeWithText("Review diagnostics")
            .assertIsDisplayed().assertHeightIsAtLeast(48.dp).fetchSemanticsNode().boundsInRoot
        val dismiss = ui.onNodeWithText("Dismiss")
            .assertIsDisplayed().assertHeightIsAtLeast(48.dp).fetchSemanticsNode().boundsInRoot

        assertTrue(title.left >= card.left && title.right <= card.right)
        assertTrue(detail.left >= card.left && detail.right <= card.right)
        assertTrue(review.left >= card.left && review.right <= card.right)
        assertTrue(dismiss.left >= card.left && dismiss.right <= card.right)
        assertTrue(detail.top >= title.top)
        assertTrue(review.top >= detail.bottom)
        assertTrue(dismiss.top >= detail.bottom)
        assertTrue(review.right <= dismiss.left || dismiss.right <= review.left ||
            review.bottom <= dismiss.top || dismiss.bottom <= review.top)
        assertTrue(dismiss.top >= review.bottom)
    }

    @Test fun wideLightAndDarkLayoutsKeepTheActionAvailable() {
        listOf(true, false).forEach { dark ->
            var opened = false
            setBanner(width = 600.dp, fontScale = 1f, dark = dark,
                onReview = { opened = true })
            val card = ui.onNodeWithTag("terminal-runtime-warning").assertIsDisplayed()
            card.assertWidthIsAtLeast(570.dp)
            ui.onNodeWithText("Review diagnostics").assertIsDisplayed().performClick()
            ui.runOnIdle { assertTrue(opened) }
        }
    }

    private fun setBanner(
        width: androidx.compose.ui.unit.Dp,
        fontScale: Float,
        dark: Boolean = true,
        onReview: () -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        ui.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale),
                LocalKlyroColors provides KlyroColors.create(dark, "cyan")
            ) {
                MaterialTheme {
                    Box(Modifier.width(width)) {
                        KlyroTerminalRuntimeWarningBanner(
                            onReviewDiagnostics = onReview,
                            onDismiss = onDismiss
                        )
                    }
                }
            }
        }
    }
}
