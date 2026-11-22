# Preview release notes

Maintain docs/PREVIEW_NOTES.md as the short cumulative, user-facing highlights of the current integration branch. Update it in the same change as each user-visible feature or fix; remove obsolete claims and older details as needed. Describe only features present in the source revision, not planned features. Keep the notes short and readable in Settings.

The signed APK job copies these notes into klyro-update.json. Both Preview publication scopes use that exact artifact's releaseNotes, so no second checkout or moving branch can mix descriptions and code. The commit SHA, branch, version and digest remain in update metadata for security and diagnostics, not in normal user-facing release notes. Old Preview builds without real notes get a plain maintenance fallback; Stable release behavior is unchanged.
