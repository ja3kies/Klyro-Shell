# Klyro Work State

Updated: 2026-11-19

- **Branch:** `feat/product-overhaul-latest`, based on `origin/fix/transparent-touch-feedback-20261118` (`2341c72`). No changes to `main`.
- **Current screen:** Terminal input and Unicode-safe query/name handling; Home lifecycle lot is committed.
- **Latest upstream context:** PR #50 (`fix/screen-audit-20261118`) and PR #51 (`fix/transparent-touch-feedback-20261118`) are open draft PRs. Latest branch CI for `2341c72` succeeded on 2026-11-18.
- **Inventory:** 16 Compose entry files. MainActivity 1,920 lines; TerminalPane 1,940; Settings 1,143; Plugins 695; Workspaces 513; Command Palette 523; Color Picker 508; shared UI 437. Existing tests: 340 files under app/native/tools combined, including Android UI, JVM/Python and native PTY suites.
- **Security baseline:** Confirmed: app backup disabled; session service and FileProvider are not exported; launcher aliases are exported as expected; plugins are declarative (no plugin script/WebView execution). Review still required: update APK install permission/grants and signature flow, SAF revoked/partial transfers, parser/archive boundaries, and diagnostics exports. No new permission changes made.
- **Finding — P1, Home/Terminal lifecycle (fixed in code):** `KlyroHomePixelCover.kt` and `TerminalPane.kt` derived `LifecycleOwner` by casting `LocalContext.current`, which can omit lifecycle observation when the context is wrapped. The Compose owner is now used directly.
- **Corrections in this overhaul:** lifecycle fix committed as `f79dec1`; Unicode-safe limits are now applied to FIND, workspace editor/catalog and session rename using the existing helper. Added catalog JVM and Compose interaction boundary tests.
- **Verified:** local Python suite 655/655 passed; native PTY host suite passed (20k lines, fd count unchanged, HUP/exit/UTF-8 checks); `git diff --check` passed. CI `44447468867` for `f79dec1` completed successfully, including JVM/native tests, lint/accessibility checks, ARM64 debug and instrumented APK builds, metadata, 16 KiB audit and signature checks. It predates the current Unicode changes.
- **Not verified:** local Android compile/instrumented execution (no SDK configured); physical Android interaction; full visual appearance in light/dark, landscape and large-font modes.
- **Next exact action:** commit and push the Unicode limit correction, await CI for that exact commit, then continue Terminal input/rendering audit. Do not claim Compose UI test execution until the instrumented suite actually runs.
