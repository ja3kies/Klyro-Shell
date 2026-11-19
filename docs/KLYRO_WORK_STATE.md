# Klyro Work State

Updated: 2026-11-19

- **Branch:** `feat/product-overhaul-latest`, based on `origin/fix/transparent-touch-feedback-20261118` (`2341c72`). No changes to `main`.
- **Current screen:** Home / Welcome, with lifecycle ownership shared with Terminal.
- **Latest upstream context:** PR #50 (`fix/screen-audit-20261118`) and PR #51 (`fix/transparent-touch-feedback-20261118`) are open draft PRs. Latest branch CI for `2341c72` succeeded on 2026-11-18.
- **Inventory:** 16 Compose entry files. MainActivity 1,920 lines; TerminalPane 1,940; Settings 1,143; Plugins 695; Workspaces 513; Command Palette 523; Color Picker 508; shared UI 437. Existing tests: 340 files under app/native/tools combined, including Android UI, JVM/Python and native PTY suites.
- **Security baseline:** Confirmed: app backup disabled; session service and FileProvider are not exported; launcher aliases are exported as expected; plugins are declarative (no plugin script/WebView execution). Review still required: update APK install permission/grants and signature flow, SAF revoked/partial transfers, parser/archive boundaries, and diagnostics exports. No new permission changes made.
- **Finding — probable, Home/Terminal lifecycle:** `KlyroHomePixelCover.kt` and `TerminalPane.kt` derive `LifecycleOwner` by casting `LocalContext.current`. Compose can provide an Activity-wrapped context while exposing the actual owner separately; a failed cast silently omits stop/resume observation. Planned fix: use Compose's lifecycle owner local in both callsites, with a behavioral lifecycle test.
- **Corrections in this overhaul:** added a behavior test for Home GIF stop/resume driven by a Compose lifecycle owner; changed Home cover and Terminal job observation to use `LocalLifecycleOwner`; added explicit `lifecycle-runtime-compose:2.8.7` dependency. CI validation pending.
- **Verified:** latest upstream UI CI success; local Python suite: 654 tests passed after updating two stale source-contract assertions; native PTY host suite passed (20k lines, fd count unchanged, HUP/exit/UTF-8 checks); `git diff --check` passed.
- **Not verified:** local Android compile/instrumented execution (no SDK configured); physical Android interaction; full visual appearance in light/dark, landscape and large-font modes.
- **Next exact action:** run the focused Home/lifecycle test and Android compilation in CI, fix any compile/test issue, then continue the screen audit with Terminal input/rendering.
