"""F/G source contract checks; runtime behavior still needs a real device."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java/com/oryn/terminal"


class FGSourceContracts(unittest.TestCase):
    def test_nonspoofable_bash_probe_and_private_paths(self):
        manager = (SRC / "session/TerminalSessionManager.java").read_text()
        bootstrap = (SRC / "termux/TermuxBootstrapInstaller.java").read_text()
        self.assertIn("freshVerificationMarker()", manager)
        self.assertIn('hasExactLine(stdout, "KLYRO_PATHS_OK")', manager)
        self.assertIn("toUpperCase(Locale.ROOT)", manager)
        self.assertIn("KLYRO_PATHS_OK", bootstrap)
        self.assertIn("KLYRO_UID_OK", bootstrap)
        self.assertIn("KLYRO_APT_OK", bootstrap)
        self.assertIn("KLYRO_PKG_OK", bootstrap)
        self.assertIn("&& -x", bootstrap)

    def test_cross_activity_workers_and_deadlines(self):
        activity = (SRC / "MainActivity.kt").read_text()
        bootstrap = (SRC / "termux/TermuxBootstrapInstaller.java").read_text()
        proot = (SRC / "termux/ProotRuntimeInstaller.java").read_text()
        self.assertIn("observePreviousInstall()", activity)
        self.assertGreaterEqual(activity.count("if (isDestroyed) return@runOnUiThread"), 4)
        self.assertIn("if (isInstalling || isVerifyingRuntime) return", activity)
        self.assertIn("static final AtomicBoolean installing", bootstrap)
        self.assertIn("static final AtomicBoolean installing", proot)
        self.assertIn("DOWNLOAD_DEADLINE_NANOS", bootstrap)
        self.assertIn("EXTRACTION_DEADLINE_NANOS", bootstrap)
        self.assertIn("Duplicate runtime manifest path", proot)

    def test_shareable_diagnostics_and_test_version(self):
        diagnostic = (SRC / "core/KlyroDiagnostics.java").read_text()
        self.assertIn("[private-path]", diagnostic)
        self.assertIn("MAX_LENGTH = 700", diagnostic)
        manager = (SRC / "session/TerminalSessionManager.java").read_text()
        self.assertIn("KlyroDiagnostics.sanitize(cleaned.toString())", manager)
        build = (ROOT / "app/build.gradle.kts").read_text()
        self.assertIn("versionCode = 9", build)
        self.assertIn('versionName = "0.4.2-dev"', build)
        self.assertTrue((ROOT / "docs/OBJECTIVES_F_G_2026-11-06.md").is_file())


if __name__ == "__main__":
    unittest.main()
