"""Cross-file contract checks for the signed in-app updater."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
REPO = (ROOT / "app/src/main/java/com/oryn/terminal/updates/KlyroUpdateRepository.kt").read_text()
POLICY = (ROOT / "app/src/main/java/com/oryn/terminal/updates/KlyroUpdatePolicy.kt").read_text()
INSTALLER = (ROOT / "app/src/main/java/com/oryn/terminal/updates/KlyroUpdateInstaller.kt").read_text()
SETTINGS = (ROOT / "app/src/main/java/com/oryn/terminal/ui/KlyroSettingsSheet.kt").read_text()
GRADLE = (ROOT / "app/build.gradle.kts").read_text()
MANIFEST = (ROOT / "app/src/main/AndroidManifest.xml").read_text()
WORKFLOW = (ROOT / ".github/workflows/android-build.yml").read_text()


class InAppUpdateContractTest(unittest.TestCase):
    def test_settings_has_both_channels_and_saved_choice(self):
        self.assertIn('putString("channel", channel.storageValue)', SETTINGS)
        self.assertIn('getString("channel", "stable")', SETTINGS)
        self.assertIn('UpdateChannel.entries.forEachIndexed', SETTINGS)

    def test_settings_checks_on_entry_and_has_manual_recheck(self):
        self.assertIn('LaunchedEffect(section, updateChannel)', SETTINGS)
        self.assertIn('if (section == "updates") checkForUpdates()', SETTINGS)
        self.assertIn('Text(if (updateChecking) "Checking…" else "Check for updates")', SETTINGS)

    def test_stable_source_selects_latest_release_and_named_apk(self):
        self.assertIn('"$api/releases/latest"', REPO)
        self.assertIn('findByName("klyro-shell.apk")', REPO)
        self.assertIn('findByName("klyro-update.json")', REPO)

    def test_preview_is_branch_and_workflow_scoped(self):
        self.assertIn('android-build.yml/runs?branch=$encodedBranch&status=success', REPO)
        self.assertIn('run.optString("head_branch") != safeBranch', REPO)
        self.assertIn('WORKFLOW_PATH = ".github/workflows/android-build.yml"', POLICY)

    def test_only_signed_preview_artifact_is_eligible(self):
        self.assertIn('SIGNED_PREVIEW_ARTIFACT = "klyro-shell-debug"', POLICY)
        self.assertIn('artifact.name == SIGNED_PREVIEW_ARTIFACT', POLICY)
        self.assertNotIn('SIGNED_PREVIEW_ARTIFACT = "klyro-shell-debug-key"', POLICY)

    def test_preview_requires_completed_success_and_sha256(self):
        self.assertIn('run.optString("status") != "completed"', REPO)
        self.assertIn('runConclusion == "success"', POLICY)
        self.assertIn('sha256:[a-fA-F0-9]{64}', POLICY)

    def test_asset_downloads_are_bounded_and_digest_checked(self):
        self.assertIn('MAX_APK_BYTES = 250L * 1024L * 1024L', POLICY)
        self.assertIn('MAX_PREVIEW_ZIP_BYTES = 300L * 1024L * 1024L', POLICY)
        self.assertIn('sha256(source) != normalized', REPO)
        self.assertIn('total > maxBytes', REPO)

    def test_installer_checks_package_version_and_same_signer(self):
        self.assertIn('candidateInfo.packageName != KlyroUpdatePolicy.PACKAGE_NAME', INSTALLER)
        self.assertIn('candidateCode <= installedCode', INSTALLER)
        self.assertIn('candidateSigners.map', INSTALLER)

    def test_installer_requires_user_approved_android_handoff(self):
        self.assertIn('canRequestPackageInstalls()', INSTALLER)
        self.assertIn('ACTION_MANAGE_UNKNOWN_APP_SOURCES', INSTALLER)
        self.assertIn('Intent.ACTION_VIEW', INSTALLER)
        self.assertIn('FLAG_GRANT_READ_URI_PERMISSION', INSTALLER)

    def test_file_provider_is_confined_to_update_cache(self):
        self.assertIn('android:exported="false"', MANIFEST)
        self.assertIn('android:grantUriPermissions="true"', MANIFEST)
        self.assertIn('path="updates/"', (ROOT / "app/src/main/res/xml/klyro_update_file_paths.xml").read_text())

    def test_ci_embeds_branch_and_stable_semver_override(self):
        self.assertIn('buildConfigField("String", "UPDATE_BRANCH"', GRADLE)
        self.assertIn('GITHUB_HEAD_REF', GRADLE)
        self.assertIn('KLYRO_RELEASE_VERSION_NAME', GRADLE)
        self.assertIn("tags: ['v*']", WORKFLOW)

    def test_stable_release_is_signed_main_ancestor_and_digest_checked(self):
        self.assertIn('git merge-base --is-ancestor "$GITHUB_SHA" origin/main', WORKFLOW)
        self.assertIn('klyro-shell.apk', WORKFLOW)
        self.assertIn('test "$ACTUAL_SHA256" = "$EXPECTED_SHA256"', WORKFLOW)
        self.assertIn('contents: write', WORKFLOW)


if __name__ == "__main__":
    unittest.main()
