"""Plugins and command palette share truthful availability, errors and rotation safety."""
from pathlib import Path
import unittest
BASE=Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui"
P=(BASE/"KlyroPluginsPage.kt").read_text()
C=(BASE/"KlyroCommandPalette.kt").read_text()
class PluginPaletteClosure(unittest.TestCase):
 def test_installed_and_explore_reset_scroll_on_switch(self):
  self.assertIn('LaunchedEffect(page) { pageScroll.scrollTo(0) }',P)
  self.assertIn('.verticalScroll(pageScroll)',P)
 def test_errors_survive_rotation_and_share_accessible_component(self):
  self.assertIn('var error by rememberSaveable',P)
  self.assertIn('KlyroInlineNotice(it, isError = true)',P)
  self.assertIn('KlyroInlineNotice("Updating local plugins…")',P)
  self.assertIn('KlyroEmptyState(',P)
 def test_confirmed_bundle_is_not_reentrant(self):
  self.assertIn('var confirmedBundledId by rememberSaveable',P)
  self.assertIn('if (mutationBusy || confirmedMutationPending) return',P)
  self.assertIn('confirmedBundledId = id',P)
  self.assertIn('confirmedBundledId = null',P)
  self.assertIn('enabled = !mutationBusy && !confirmedMutationPending',P)
 def test_available_mode_does_not_mislabel_plugin_suggestions(self):
  self.assertIn('remember(query, pluginCommands, pinnedOnly, availableOnly)',C)
  self.assertIn('if (pinnedOnly || availableOnly) emptyList()',C)
  self.assertIn('their dependencies cannot be verified',C)
 def test_palette_feedback_uses_common_notice(self):
  self.assertIn('info?.let { KlyroInlineNotice(it, isError = unsafeFeedback) }',C)
if __name__=="__main__":unittest.main()
