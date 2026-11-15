# Session-bound paste confirmation UX — implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement each task with failing tests before code.

**Goal:** Make large and multiline shell paste safe, predictable, and understandable across fast tab changes and input backpressure.

**Architecture:** An immutable pure-Java `TerminalPasteReview` stores the originating tab ID and exact clipboard payload; it exposes a Unicode-safe bounded preview and measured size/line count. Compose retains a review object (not an unscoped string) and routes both native and menu pastes through the same confirmation. The final action checks the captured tab against both active and visible tab IDs before enqueuing, and reports rejection visibly.

**Tech Stack:** Kotlin + Jetpack Compose Material 3, Java 17/JUnit4, existing ClipboardPastePolicy/TerminalActionPolicy, Python source contracts, GitHub Actions.

**Spec:** User-observed confusing echoed/repeated script paste in Klyro-Shell on Pixel 9 Android 17. Existing UI confirms large payload without an immutable source-tab snapshot; it renders up to 65KiB of text directly in Compose and silently ignores declined enqueue. Do not claim this UI symptom is fixed until device QA.

## Global Constraints
- Do not change PTY write semantics, bracketed paste, clipboard Unicode/safety policy or user files.
- No new dependencies or intrusive onboarding, retain Android-native Material3 dialog.
- Preview limited to 3072 UTF-16 units without splitting a surrogate pair, with truthful visible truncation notice.
- Never silently submit to a different session, even if callback/recomposition order changes.
- Increment version beyond 0.4.17-dev/versionCode 24, retain Klyro APK signing, no merge.

## Review Focus
- A different tab becomes current while a multiline confirmation is open: reject stale action.
- A delayed native clipboard callback arrives after tab change: do not attribute it to the selected tab.
- Running shell exits between review and confirm: do not enqueue, report failure.
- Paste buffer fills after validation: report unsent paste, do not claim success.
- CRLF, surrogate boundary, 65KiB clipboard payload: accurate line count and safe preview.
- Repeated touches: clear pending request before attempting one enqueue.
- All related existing source-contract tests must remain green.

### Task 1: Pure session-owned review state
- Create `app/src/main/java/com/oryn/terminal/ui/TerminalPasteReview.java`.
- Create `app/src/test/java/com/oryn/terminal/ui/TerminalPasteReviewTest.java` with tests for tab changes, paste refusal, line count, Unicode-safe preview and overflow boundaries.
- Write tests first; observe red in CI, then implement.
- Expose `TerminalPasteReview(long tabId, String text)`, `getTabId()`, `getText()`, `getPreview()`, `isPreviewTruncated()`, `getLineCount()`, `getUtf8ByteCount()`, `sendIfCurrent(Long activeId, Long displayedId, boolean running, Predicate<String> writer)`.

### Task 2: Visible and trustworthy confirmation
- Modify `app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt`.
- Change `pendingMultilinePaste` to `TerminalPasteReview?` and construct it at request time for both clipboard and native entrypoints.
- Confirm via `sendIfCurrent`; display explicit failure feedback.
- Show bounded preview, exact payload size/line counts, and truthful truncation message, with a clearly worded action button.
- Add Python source contracts that fail before implementation and pass afterward.

### Task 3: Release and review
- Bump to `0.4.18-dev` `versionCode 25`; update existing version-test contract.
- Run full JVM/Python/native/lint/ARM64/signature/16K CI and review race conditions and regressions.
- Release branch artifact only, no merge; flag device-only QA separately.
