"""Plugins visual hierarchy and navigation accessibility contract."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
CODE = (ROOT / "app/src/main/java/com/oryn/terminal/ui/KlyroPluginsPage.kt").read_text()


class PluginCompactCatalogTest(unittest.TestCase):
    def test_header_does_not_repeat_screen_type(self):
        self.assertIn('Text("Plugins", fontSize = 24.sp', CODE)
        self.assertNotIn('"LOCAL EXTENSIONS"', CODE)
        self.assertIn(
            'Offline packs · commands run only when you choose.',
            CODE,
        )

    def test_catalog_has_compact_scannable_cards(self):
        catalog = CODE.split('KlyroPluginManager.bundled.forEach { plugin ->', 1)[1].split(
            'Surface(color = colors.chrome,', 1
        )[0]
        self.assertIn('KlyroExplorePluginCard(', catalog)
        card = CODE.split('internal fun KlyroExplorePluginCard(', 1)[1].split(
            'private fun PluginHeading(', 1
        )[0]
        self.assertIn('PluginHeading(', card)
        self.assertIn('"${plugin.commands.size} commands"', card)
        self.assertIn('Modifier.weight(1f)', card)
        self.assertNotIn('commands · Offline', card)
        self.assertIn('OutlinedButton(', card)
        self.assertIn('contentDescription = if (isInstalled)', card)
        self.assertIn('"Review and add ${plugin.name}"', card)
        self.assertIn('pendingBundledId = plugin.id', catalog)
        self.assertNotIn('ButtonDefaults.buttonColors(containerColor = colors.accent', catalog)
        self.assertIn('horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm)', card)
        self.assertIn('modifier = modifier.fillMaxWidth()', CODE)

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
