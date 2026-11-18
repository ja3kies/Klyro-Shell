# Storage interruption flows — implementation handoff

Branch: integration/storage-interruptions-20261118. Base: diagnostics-recovery 75781df. No main merge.

Ruling: user assigned execution of tests to another agent. Work here is implementation and static review, not validated Android behavior. Existing KlyroFileSafety rules, terminal keyboard and PTY ownership are preserved.

## 45 implementation objectives

1. A shared synchronous gate prevents competing file transfers.
2. Picker ownership is acquired before launching Android UI, preventing repeated launches.
3. Folder, import and export picker ownership are distinguished.
4. Picker ownership is restored after Activity recreation.
5. Shell-requested folder selection waits while a UI-owned picker/transfer is active.
6. Deferred shell selection is reconsidered by the existing poller after UI ownership ends.
7. Shell selection also waits while a transfer result or export form owns focus.
8. Starting a transfer opens an explicit progress/result surface instead of relying only on a transient toast.
9. Progress uses an indeterminate indicator rather than inventing a byte percentage.
10. Transfer cancellation is exposed as a user action.
11. Cancellation has its own pending state, preventing repeated cancel dispatch.
12. Users can close the dialog after cancellation even if a provider blocks; normal app navigation is not trapped.
13. The operation gate remains active until the cancelled worker actually finishes, preventing overlapping storage writes.
14. Cancellation propagates through worker code rather than becoming a generic success/error result.
15. Results remain readable until dismissed and survive Activity recreation.
16. Recreation during IO reports an unknown outcome and requests inspection; it does not assert success or restart the copy automatically.
17. Transfer result text can be selected/copied.
18. Transfer status/error text uses a polite accessibility live region.
19. Transfer content scrolls within a bounded dialog for large fonts/short screens.
20. Back/outside dismissal requests cancellation during work, and closes once cancellation is pending or work is finished.
21. Folder authorization continues the user's pending import without requiring a second trip through Settings.
22. That continuation intent survives the system picker's configuration changes.
23. Cancelling or denying folder authorization clears continuation, so a later unrelated grant does not start an import.
24. When all saved folder grants are revoked, importing routes to authorization again.
25. Folder-picker launch failure clears ownership, reports failure and completes a shell request as denied.
26. Single-file picker launch failure clears ownership and provides a visible explanation.
27. Import/export picker cancellation is reported without implying any file was copied.
28. Export callbacks without a restored source explain the expired request rather than silently returning.
29. Export preflight runs on Dispatchers.IO instead of blocking the UI thread with filesystem validation.
30. Preflight has an immediate busy guard against repeated confirmation taps.
31. Export validation can be cancelled from its form without waiting for local filesystem work.
32. The source path is captured for the pending destination picker; editing cannot change an in-flight export.
33. File-path draft length is bounded by Unicode code points rather than cutting surrogate pairs.
34. Editing the path clears stale validation errors.
35. File-path input disables autocorrection and supports a Done action through the same validation route.
36. Relative/outside/missing-file errors use safe actionable messages; arbitrary provider error text is not exposed.
37. Export picker failure preserves the editable form and releases pending ownership.
38. Import worker logic is shared between single-document and folder flows, keeping temporary-file cleanup and collision-safe commit consistent.
39. Single-file import checks known size metadata before opening/copying an oversized file; streaming limits remain authoritative.
40. Missing optional display-name/size columns fall back safely instead of failing on fixed column indices.
41. Folder imports isolate per-file copy failures so later readable files can still be imported.
42. Batch results distinguish failed files, oversized files and inaccessible folders instead of collapsing them into one success count.
43. Batch inspection stops after 300 entries, reports that bound, and directs users to single-file import for remaining files.
44. Reaching the existing 30-file limit is explicitly reported; empty-root claims are made only after a successful, non-truncated inspection.
45. Settings and completion text explain root-level copying, private destinations, size limits, preserved originals and partial-result consequences.

## Review and evidence

`git diff --check`: clean. Independent static review identified two issues: a blocked provider could trap the cancellation dialog, and failed folder traversal could falsely imply an empty folder. Both were corrected. No tests were deleted, weakened or skipped; no runtime result is claimed.

Build/lint/unit/instrumented/Pixel tests: NOT RUN here. CI: UNKNOWN. Screenshots: NOT PRODUCED. Source-string contracts referring to the former MainActivity implementation need semantic review by the testing agent after the extraction, not blind rewriting.

Required verification: repeated clicks on every entry point; folder/import/export picker cancel and missing-provider paths; revoke folder permissions; authorization-to-import continuation; rotation in picker/form/validation/copy/result; blocked and slow document providers; cancellation before/after each chunk and commit; existing imported files preserved; 0/30/31 files; >300 entries; oversized/unknown-size/missing-column documents; per-file failure followed by a readable file; partial folder traversal failure; invalid/outside/symlink/missing export paths; full storage; partial export destinations; expired export request; shell bridge requests while another modal owns the storage flow; TalkBack, large text and narrow screens.

Limits: provider calls may be non-cooperative. Cancellation remains pending until those calls return; the dialog can be closed, but another storage transfer is gated meanwhile. IO is Activity-scoped, not a resumable background service. Files already committed remain after cancellation; exported destinations may be incomplete. Inspect outcomes before retrying.

## Component reference

https://www.carbondesignsystem.com/building-blocks/core/components/file-uploader/guidelines
https://developer.android.com/training/data-storage/shared/documents-files

Applied: clear file-operation status, persistent actionable errors, native Android document pickers. No new dependency or custom file browser.
