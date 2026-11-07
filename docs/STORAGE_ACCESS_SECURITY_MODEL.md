# Klyro storage access: permissions and threat model

## Verified platform boundary
Android 11+ blocks selecting the root **Download** directory with
`ACTION_OPEN_DOCUMENT_TREE`. A successful SAF tree grant is not a POSIX mount.
The private Termux-compatible proot environment does not inherit Android
document-provider access simply because its terminal uses Bash.

## Current branch implementation
- `klyro-setup-storage` is the canonical helper; `setup-storage` and
  `termux-setup-storage` are compatibility functions in generated profiles.
- Multiple persisted, read-only SAF tree grants are retained; the last-selected
  tree remains in the legacy `tree_uri` key for migration.
- Folder import copies files into Klyro's private `~/storage-imports`.
  The import operation is bounded and checks filenames and file sizes.
- The standalone file picker can select a file inside Downloads without
  granting the restricted root tree.
- **Not yet equivalent to Termux:** no `~/storage/downloads` POSIX link and
  no automatic shell access to arbitrary Downloads files.

## Next permission decision
To reproduce Termux's direct `~/storage/downloads` access, Klyro would need
an explicit Android shared-storage access model plus a carefully constrained
PRoot bind. On Android 11+, broad all-files access uses
`MANAGE_EXTERNAL_STORAGE`, which is a sensitive special app access and may
be restricted by distribution policy. It must be opt-in, never silently enabled,
and verified on a physical device before being called functional.

## Regression checklist
1. Existing SAF grant remains after adding a second folder.
2. Repeated selection does not duplicate a grant.
3. Cancelled selection leaves prior grants untouched.
4. Revoked grants are not treated as authorized.
5. Import never follows unsafe destination paths.
6. Private `$HOME` and `$PREFIX` are not exposed through shared storage.
7. CLI helper installation never overwrites user-managed scripts.
8. Monochrome variants retain legible text and distinguish active controls.

Rollback: revert the feature branch; do not remove existing persisted SAF
permissions or rewrite user-managed Bash startup files.
