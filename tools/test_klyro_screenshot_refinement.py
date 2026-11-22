"""Contracts from the supplied Home/Explore captures; Android UI tests cover layout."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
HOME = (ROOT / "app/src/main/java/com/oryn/terminal/MainActivity.kt").read_text()
PLUGINS = (ROOT / "app/src/main/java/com/oryn/terminal/ui/KlyroPluginsPage.kt").read_text()


class ScreenshotRefinementTest(unittest.TestCase):
    def test_home_uses_workspace_context_instead_of_architecture_badge(self):
        identity = HOME.split('Text("KLYRO"', 1)[1].split('// Keep the terminal action', 1)[0]
        self.assertIn('homeWorkspaceName', identity)
        self.assertNotIn('"ARM64"', identity)
        self.assertIn('val calmHomeReady =', HOME)
        self.assertIn('if (calmHomeReady) screenChrome else ui.sheet', HOME)

    def test_plugin_tabs_use_selection_outline_instead_of_primary_action_fill(self):
        self.assertIn('border = if (selected) BorderStroke', PLUGINS)
        self.assertIn('color = klyroStateColor(if (selected) colors.sheet else colors.key', PLUGINS)
        self.assertIn('selectableGroup()', PLUGINS)

    def test_catalog_card_adapts_instead_of_squeezing_action_beside_large_text(self):
        self.assertIn('internal fun KlyroExplorePluginCard(', PLUGINS)
        self.assertIn('maxWidth < 320.dp || LocalDensity.current.fontScale >= 1.3f', PLUGINS)
        self.assertIn('if (stacked)', PLUGINS)

    def test_read_failure_is_retryable_and_is_not_an_empty_installation(self):
        self.assertIn('var registryReadFailed by remember', PLUGINS)
        self.assertIn('!registryLoading && !registryReadFailed', PLUGINS)
        self.assertIn('onClick = { registryRevision += 1 }', PLUGINS)
        self.assertIn('Text("Retry")', PLUGINS)
        completion = PLUGINS.split('LaunchedEffect(installed, registryLoading, registryReadFailed,', 1)[1]
        self.assertIn('if (registryLoading || registryReadFailed) return@LaunchedEffect', completion)

    def test_only_one_scroll_container_is_active_and_large_text_has_an_escape(self):
        self.assertIn('LocalConfiguration.current.screenHeightDp < 600', PLUGINS)
        self.assertIn('if (scrollWholePage) Modifier.verticalScroll(pageScroll) else Modifier', PLUGINS)
        self.assertIn('if (scrollWholePage) Modifier else Modifier.weight(1f).verticalScroll(pageScroll)', PLUGINS)

    def test_each_bundled_pack_has_a_relevant_icon(self):
        for plugin_id in ['git-review', 'storage', 'project']:
            self.assertIn('"app.klyro.' + plugin_id + '" ->', PLUGINS)


if __name__ == '__main__':
    unittest.main()
