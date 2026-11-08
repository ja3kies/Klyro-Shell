"""Small source-level CI invariants for operational readiness.

No shell command is executed against a real Android device from CI.
"""
from pathlib import Path
import re
import unittest
from klyro_version_contract import assert_supported_klyro_version

ROOT = Path(__file__).resolve().parents[1]


class KlyroOperationalReadinessTest(unittest.TestCase):
    def test_version_bumped_and_settings_layout_unified(self):
        gradle = (ROOT / "app/build.gradle.kts").read_text()
        assert_supported_klyro_version(self, gradle)
        self.assertTrue((ROOT / "docs/OBJECTIVES_H_I_2026-11-06.md").is_file())
        self.assertNotIn('klyro_experimental_bento', gradle)
        self.assertFalse((ROOT / "app/src/main/java/com/oryn/terminal/ui/KlyroExperimentalBento.kt").exists())

    def test_native_paste_enforces_same_policy_as_compose(self):
        source = (ROOT /
                  "app/src/main/java/com/oryn/terminal/session/OrynTerminalSessionClient.java"
                  ).read_text()
        self.assertIn("ClipboardPastePolicy.canPaste(pasted)", source)
        self.assertIn("ClipboardPastePolicy.requiresConfirmation(pasted)", source)
        self.assertIn("listener.onNativePasteRequiresConfirmation(pasted)", source)
        viewmodel = (ROOT /
                     "app/src/main/java/com/oryn/terminal/TerminalSessionViewModel.kt"
                     ).read_text()
        self.assertIn("pendingNativePaste", viewmodel)
        ui = (ROOT /
              "app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt"
              ).read_text()
        self.assertIn("ClipboardPastePolicy.canPaste(incoming)", ui)
        self.assertIn("ClipboardPastePolicy.requiresConfirmation(incoming)", ui)
        self.assertIn("pendingMultilinePaste = incoming", ui)

    def test_operational_gate_no_package_mutations(self):
        source = (ROOT / "scripts/klyro-operational-gate.sh").read_text()
        for forbidden in (r"pkg\s+upgrade", r"pkg\s+install", r"apt\s+upgrade",
                          r"apt\s+install", r"apt\s+update"):
            self.assertNotRegex(source, forbidden)
        self.assertIn("KLYRO-OPERATIONAL-GATE/v1", source)


if __name__ == "__main__":
    unittest.main()
