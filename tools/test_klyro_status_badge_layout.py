"""The status title and its badge must not compete for one narrow row."""
from pathlib import Path
import unittest
MAIN = (Path(__file__).resolve().parents[1] /
        "app/src/main/java/com/oryn/terminal/MainActivity.kt").read_text()
class RuntimeBadgeResponsiveTest(unittest.TestCase):
    def test_status_summary_wraps_without_truncation(self):
        # The compact tile keeps full status text reachable at large font scales.
        status = MAIN.split('stateLabel,', 1)[1].split('homeStatus.terminalFailure?.let', 1)[0]
        self.assertIn('homeStatus.runtimeMessage', status)
        self.assertIn('liveRegion = LiveRegionMode.Polite', status)
        self.assertNotIn('maxLines =', status)
        self.assertIn('modifier = Modifier.weight(1f)', MAIN)
    def test_recheck_is_in_compact_status_row_and_accessible(self):
        action = MAIN.split('val recheckAction:', 1)[1].split('Row(', 1)[0]
        self.assertIn('if (verifyingRuntime) "Checking…" else "Recheck"', action)
        self.assertIn('contentDescription = "Recheck environment"', action)
        self.assertIn('heightIn(min = 48.dp)', action)
        self.assertIn('enabled = !verifyingRuntime && !installing', action)

    def test_bento_adapts_to_width_and_font_scale(self):
        home = MAIN.split('BoxWithConstraints(modifier = Modifier.fillMaxWidth())', 1)[1]
        self.assertIn('maxWidth < 340.dp', home)
        self.assertIn('LocalDensity.current.fontScale >= 1.3f', home)
        self.assertIn('if (compactTiles)', home)
        self.assertIn('Column(verticalArrangement = Arrangement.spacedBy(10.dp))', home)
        self.assertIn('Row(horizontalArrangement = Arrangement.spacedBy(12.dp))', home)

if __name__ == "__main__":
    unittest.main()
