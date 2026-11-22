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
        self.assertIn('else -> "Check for updates"', SETTINGS)

    def test_stable_source_selects_latest_release_and_named_apk(self):
        self.assertIn('"$api/releases/latest"', REPO)
        self.assertIn('findByName("klyro-shell.apk")', REPO)
        self.assertIn('findByName("klyro-update.json")', REPO)

    def test_preview_is_branch_scoped_and_uses_public_release_assets(self):
        self.assertIn('releases/tags/preview-$branchHash', REPO)
        self.assertIn('manifest.optString("branch") != branch', REPO)
        self.assertIn('release.optBoolean("prerelease")', REPO)
        self.assertIn('apk.optString("browser_download_url")', REPO)
        self.assertNotIn('archive_download_url', REPO)

    def test_only_cumulative_branch_may_publish_global_preview(self):
        self.assertIn("branches: ['**']", WORKFLOW)
        preview = WORKFLOW.split("  publish-preview:", 1)[1]
        self.assertIn("github.ref_type == 'branch'", preview)
        self.assertIn("github.event_name == 'push'", preview)
        self.assertIn("github.event_name == 'workflow_dispatch'", preview)
        self.assertIn("needs: debug-apk", preview)
        self.assertIn("scope: [branch, global]", preview)
        self.assertIn('tag="preview-latest"', preview)
        self.assertIn('CONSOLIDATED_BRANCH="integration/senior-release-20261121"', preview)
        self.assertIn('"$PREVIEW_SCOPE" == global && "$GITHUB_REF_NAME" != "$CONSOLIDATED_BRANCH"', preview)
        self.assertIn('"$head_sha" != "$GITHUB_SHA"', preview)
        self.assertIn('compare/$old_sha...$GITHUB_SHA', preview)
        self.assertIn('metadata["commitSha"] != os.environ["GITHUB_SHA"]', preview)
        self.assertIn("new_code <= old_code", preview)
        self.assertIn("cancel-in-progress: false", preview)
        self.assertIn("--prerelease --latest=false", preview)
        self.assertIn('metadata["branch"] != os.environ["GITHUB_REF_NAME"]', preview)
        self.assertIn('metadata["apkSha256"] != hashlib.sha256', preview)

    def test_preview_rejects_unrelated_branch_even_with_newer_build_number(self):
        self.assertIn('releases/tags/preview-latest', REPO)
        self.assertIn('releases/tags/preview-$branchHash', REPO)
        self.assertIn('val safeBranch = KlyroUpdatePolicy.CONSOLIDATED_PREVIEW_BRANCH', REPO)
        self.assertIn('ignoreOtherBranch = true', REPO)
        self.assertIn('if (ignoreOtherBranch) return UpdateLookup.NonePublished', REPO)
        self.assertIn('git merge-base --is-ancestor 9705735f5a654df8625411c9d3d4b3a699ff9172 HEAD', WORKFLOW)
        self.assertIn('sourceBranch = manifest.optString("branch")', REPO)
        self.assertIn("publishedBranch.isBlank()", REPO)
        self.assertNotIn("Source branch: $source", SETTINGS)
        self.assertIn('Text("What\'s new"', SETTINGS)
        self.assertIn("may not contain all installed features", SETTINGS)

    def test_preview_release_uses_user_facing_notes_from_exact_build(self):
        self.assertIn('data["releaseNotes"]', WORKFLOW)
        self.assertEqual(WORKFLOW.count("--notes-file release-assets/preview-notes.txt"), 2)
        self.assertNotIn('--notes "Signed Android build for', WORKFLOW)
        self.assertIn('previewUserNotes(manifest.optString("releaseNotes"), release.optString("body"))', REPO)

    def test_expired_preview_has_separate_message_and_recovery_action(self):
        self.assertIn('connection.responseCode in setOf(404, 410)', REPO)
        self.assertIn('stalePreview = true', REPO)
        self.assertIn('Find a newer Preview', SETTINGS)
        self.assertIn('!updateStalePreview', SETTINGS)

    def test_channel_switch_is_disabled_while_checking_or_downloading(self):
        self.assertIn('enabled = !updateDownloading && !updateChecking', SETTINGS)

    def test_invalid_and_replaced_apks_are_removed_from_cache(self):
        self.assertIn('if (sha256(source) != normalized) {\n                source.delete()', REPO)
        self.assertIn('unverifiedDownload?.delete()', SETTINGS)
        self.assertIn('downloadedUpdate?.delete()', SETTINGS)

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
