package com.oryn.terminal.ui
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.*
import org.junit.Assert.*

class TerminalExtraKeyUiTest {
 @get:Rule val ui=createComposeRule()
 @Test fun tapDispatchesExactlyOnce() {
  var taps=0
  ui.setContent { MaterialTheme { TerminalExtraKey(label="Esc",description="Test escape",onClick={taps++}) } }
  ui.onNodeWithContentDescription("Test escape").performClick()
  ui.runOnIdle { assertEquals(1,taps) }
 }
 @Test fun longPressDoesNotAlsoSendShortKey() {
  var taps=0;var holds=0
  ui.setContent { MaterialTheme { TerminalExtraKey(label="Left",description="Test left",onLongClick={holds++},onClick={taps++}) } }
  ui.onNodeWithContentDescription("Test left").performTouchInput { longClick() }
  ui.runOnIdle { assertEquals(1,holds);assertEquals(0,taps) }
 }
 @Test fun largeFontCanExpandBeyondOldFixedHeight() {
  ui.setContent { val density=LocalDensity.current
   CompositionLocalProvider(LocalDensity provides Density(density.density,3f)) {
    MaterialTheme { TerminalExtraKey(label="End",description="Test end",onClick={}) }
   }
  }
  ui.onNodeWithContentDescription("Test end").assertHeightIsAtLeast(51.dp)
 }
}
