"""Regression checks for monotonic CI APK versions and semantic patch base."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
GRADLE = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
CI = (ROOT / ".github/workflows/android-build.yml").read_text(encoding="utf-8")
POLICY = (ROOT / "docs/BRANCH_AND_RELEASE_POLICY.md").read_text(encoding="utf-8")


class KlyroAutomaticVersioning(unittest.TestCase):
    def test_base_patch_is_bumped_once_for_this_change(self):
        self.assertIn('val baseKlyroVersionCode = 19', GRADLE)
        self.assertIn('val baseKlyroVersionName = "0.4.12-dev"', GRADLE)

    def test_github_run_and_attempt_are_both_monotonic_inputs(self):
        self.assertIn('GITHUB_ACTIONS', GRADLE)
        self.assertIn('GITHUB_RUN_NUMBER', GRADLE)
        self.assertIn('GITHUB_RUN_ATTEMPT', GRADLE)
        self.assertIn('run * 100 + attempt', GRADLE)
        self.assertIn('require(run in 1..20_000_000', GRADLE)
        self.assertIn('attempt in 1..99', GRADLE)
        self.assertIn('versionCode = finalVersionCode', GRADLE)
        self.assertIn('versionName = finalVersionName', GRADLE)
        self.assertIn('tasks.register("printKlyroVersion")', GRADLE)

    def test_ci_checks_actual_packaged_apk_version(self):
        self.assertIn('gradle -q :app:printKlyroVersion', CI)
        self.assertIn('versionCode=\'$EXPECTED_CODE\'', CI)
        self.assertIn('versionName=\'$EXPECTED_NAME\'', CI)
        self.assertIn('aapt" dump badging "$APK"', CI)

    def test_patch_minor_major_policy_is_documented(self):
        self.assertIn('0.4.12-dev', POLICY)
        self.assertIn('0.5.0', POLICY)
        self.assertIn('versionCode', POLICY)
        self.assertIn('GITHUB_RUN_ATTEMPT', POLICY)


if __name__ == "__main__":
    unittest.main()
