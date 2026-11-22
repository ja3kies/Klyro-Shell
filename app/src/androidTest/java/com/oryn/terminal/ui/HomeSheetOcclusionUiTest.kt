package com.oryn.terminal.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.oryn.terminal.HomePatternVisibility
import com.oryn.terminal.LocalHomePatternVisibility
import com.oryn.terminal.homePatternScreenBounds
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeSheetOcclusionUiTest {
    @get:Rule val ui = createAndroidComposeRule<ComponentActivity>()

    @Test fun settledCompactSheetLeavesBannerAnimationEnabled() {
        val tracker = HomePatternVisibility()
        val open = mutableStateOf(false)
        ui.setContent {
            CompositionLocalProvider(
                LocalKlyroColors provides KlyroColors.create(true, "lime"),
                LocalHomePatternVisibility provides tracker
            ) {
                MaterialTheme {
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxWidth().height(100.dp)
                            .onGloballyPositioned {
                                tracker.coverBounds = it.homePatternScreenBounds()
                            })
                        if (open.value) {
                            KlyroSheet(onDismissRequest = { open.value = false }) {
                                Spacer(Modifier.fillMaxWidth().height(100.dp))
                            }
                        }
                    }
                }
            }
        }
        ui.runOnIdle { open.value = true }
        ui.waitUntil(5000) { tracker.sheetBounds != null && tracker.coverBounds != null }
        ui.waitForIdle()
        ui.runOnIdle {
            val cover: Rect = requireNotNull(tracker.coverBounds)
            val sheet: Rect = requireNotNull(tracker.sheetBounds)
            assertTrue("Sheet must be below the banner: $sheet vs $cover", sheet.top > cover.bottom)
            tracker.settleMovement()
            assertTrue("A short sheet must leave animation running after settling", tracker.shouldAnimate)
        }
    }
}
