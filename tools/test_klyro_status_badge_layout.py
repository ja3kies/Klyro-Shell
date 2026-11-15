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
if __name__ == "__main__":
    unittest.main()
