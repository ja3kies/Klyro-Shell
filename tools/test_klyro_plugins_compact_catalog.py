"""Plugins visual hierarchy and navigation accessibility contract."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
CODE = (ROOT / "app/src/main/java/com/oryn/terminal/ui/KlyroPluginsPage.kt").read_text()


class PluginCompactCatalogTest(unittest.TestCase):
    def test_header_does_not_repeat_screen_type(self):
        self.assertIn('Text("Plugins", fontSize = 28.sp', CODE)
        self.assertNotIn('"LOCAL EXTENSIONS"', CODE)
        self.assertIn(
            'Command packs insert suggestions into Bash; nothing runs automatically.',
            CODE,
        )

    def test_catalog_has_compact_scannable_cards(self):
        catalog = CODE.split('KlyroPluginManager.bundled.forEach { plugin ->', 1)[1].split(
            'Surface(color = colors.chrome,', 1
        )[0]
        self.assertIn('PluginHeading(plugin, "")', catalog)
        self.assertIn('Text("${plugin.commands.size} commands · Offline"', catalog)
        self.assertIn('OutlinedButton(', catalog)
        self.assertIn('contentDescription = if (isInstalled)', catalog)
        self.assertIn('"Review and add ${plugin.name}"', catalog)
        self.assertIn('pendingBundledId = plugin.id', catalog)
        self.assertNotIn('ButtonDefaults.buttonColors(containerColor = colors.accent', catalog)

    def test_confirmation_is_required_for_local_bundled_add(self):
        self.assertIn('pendingBundledId?.let { id ->', CODE)
        self.assertIn('title = { Text("Add ${record.name}?") }', CODE)
        self.assertIn('manager.installBundled(id)', CODE)
        self.assertIn('Text("Cancel")', CODE)

    def test_reading_registry_does_not_disable_home_navigation(self):
        self.assertIn(
            'onClick = ::navigateBack,\n                enabled = !mutationBusy && !confirmedMutationPending,',
            CODE
        )
        self.assertIn('if (mutationBusy || confirmedMutationPending) return', CODE)
        self.assertIn('BackHandler { navigateBack() }', CODE)


if __name__ == "__main__":
    unittest.main()
