"""Accent is reserved for primary actions and semantic statuses, not metadata."""
from pathlib import Path
import unittest

APP = Path(__file__).resolve().parents[1] / "app/src/main/java/com/oryn/terminal"
HOME = (APP / "MainActivity.kt").read_text(encoding="utf-8")
PANE = (APP / "ui/TerminalPane.kt").read_text(encoding="utf-8")
SHARED = (APP / "ui/KlyroUiComponents.kt").read_text(encoding="utf-8")
PLUGINS = (APP / "ui/KlyroPluginsPage.kt").read_text(encoding="utf-8")

class AccentHierarchyContract(unittest.TestCase):
    def test_architecture_badge_is_neutral(self):
        badge = HOME.split('Text("ARM64"', 1)[0][-230:]
        self.assertIn("color = ui.key,", badge)
        self.assertIn('Text("ARM64", color = ui.muted', HOME)

    def test_menu_and_repeated_icons_do_not_mimic_primary_ctas(self):
        self.assertIn('contentDescription = "More actions", tint = TerminalMuted', PANE)
        self.assertIn('val ink = if (!enabled) colors.muted else if (destructive) colors.danger else colors.muted', SHARED)
        self.assertIn('enabled = enabled,', SHARED)
        section = PLUGINS.split('private fun PluginHeading', 1)[1]
        self.assertIn('Surface(color = colors.key,', section)
        self.assertIn('tint = colors.muted,', section)
        self.assertIn('Text(detail, color = colors.muted', section)
        self.assertNotIn('colors.accent', section)

    def test_actions_keep_semantic_accent(self):
        self.assertIn('containerColor = colors.accent', PLUGINS)
        self.assertIn('containerColor = ui.accent', HOME)
        self.assertIn('stateColor = when (badge.severity)', HOME)

if __name__ == "__main__":
    unittest.main()
