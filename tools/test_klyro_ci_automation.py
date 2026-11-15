"""Source contracts for Klyro CI orchestration."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
BUILD = (ROOT / ".github/workflows/android-build.yml").read_text()
ROBOT = (ROOT / ".github/workflows/android-device-robot.yml").read_text()
TARGET = (ROOT / "tools/ci_targeted_tests.py").read_text()
REPORT = (ROOT / "tools/ci_build_report.py").read_text()


import importlib.util

spec = importlib.util.spec_from_file_location("ci_targeted_tests", ROOT / "tools/ci_targeted_tests.py")
selector = importlib.util.module_from_spec(spec)
spec.loader.exec_module(selector)


class TargetedSelectorTests(unittest.TestCase):
    def test_single_test_file_runs_only_that_test(self):
        command = selector.command_for(["tools/test_klyro_ci_automation.py"])
        self.assertIn("tools.test_klyro_ci_automation", command)
        self.assertNotIn("discover", command)

    def test_kotlin_change_forces_full_suite(self):
        self.assertIn("discover", selector.command_for(["app/src/main/java/Foo.kt"]))

    def test_python_runtime_change_forces_full_suite(self):
        self.assertIn("discover", selector.command_for(["tools/klyro_ui_robot.py"]))

    def test_unknown_base_forces_full_suite(self):
        self.assertIn("discover", selector.command_for(None))

    def test_empty_diff_forces_full_suite(self):
        self.assertIn("discover", selector.command_for([]))


class AutomationContracts(unittest.TestCase):
    def test_progressive_gate_precedes_expensive_android_job(self):
        self.assertIn("fast-gate:", BUILD)
        self.assertIn("needs: fast-gate", BUILD)
        self.assertIn("python3 tools/ci_targeted_tests.py", BUILD)

    def test_cache_is_used_without_unverified_native_asset_caching(self):
        self.assertIn("gradle/actions/setup-gradle@v4", BUILD)
        self.assertIn("cache-read-only:", BUILD)
        self.assertNotIn("actions/cache@v3", BUILD)

    def test_targeted_tests_fail_closed_to_full_suite(self):
        self.assertIn('"unittest", "discover"', TARGET)
        self.assertIn('"git", "diff"', TARGET)
        self.assertIn("test_", TARGET)

    def test_report_always_runs_and_persists(self):
        self.assertIn("if: always()", BUILD)
        self.assertIn("tools/ci_build_report.py", BUILD)
        self.assertIn("klyro-build-summary", BUILD)
        self.assertIn("GITHUB_STEP_SUMMARY", REPORT)
        self.assertIn("klyro-shell-signed.apk", REPORT)
        self.assertIn("versionName", REPORT)

    def test_robot_requires_real_device_and_uploads_evidence(self):
        self.assertIn("workflow_dispatch:", ROBOT)
        self.assertIn("self-hosted", ROBOT)
        self.assertIn("adb devices", ROBOT)
        self.assertIn("tools/klyro_ui_robot.py", ROBOT)
        self.assertIn("klyro-device-robot-evidence", ROBOT)
        self.assertIn("if-no-files-found: error", ROBOT)


if __name__ == "__main__":
    unittest.main()
