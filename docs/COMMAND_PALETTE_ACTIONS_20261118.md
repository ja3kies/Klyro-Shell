# Command Palette — implementation handoff

Base: workspaces-actions ffc805bb. Branch: integration/palette-actions-20261118.
Status: implementation and static review only; Android, build, lint, runtime accessibility and CI NOT VERIFIED. Tests delegated by user. No main merge.

## Implemented changes and acceptance points

1. Search, filters and results share one scrolling viewport; keyboard/large text cannot consume a separate fixed controls stack.
2. Explicit Close action dismisses the palette and hides the IME.
3. Guide navigation has its own action instead of competing with the title; Back to commands keeps the palette open.
4. Guide scroll position survives switching between guide and commands.
5. Guide text supports native selection/copy.
6. Shell search disables autocorrection to preserve command tokens.
7. Search clear uses a named native IconButton with a touch target independent of text length.
8. Clearing search restores field focus and keyboard so another query can be entered immediately.
9. Lazy item recreation does not repeatedly request keyboard focus; returning from guide deliberately requests it.
10. Result scroll state is explicit and retained; no unconditional initial reset overrides rotation restoration.
11. Filter chips wrap naturally and retain full labels instead of equal-width truncation.
12. Recent-insertion actions wrap on narrow screens instead of squeezing their label.
13. Plugin and shell command sections have distinct accessibility headings.
14. Plugin descriptions expose their purpose alongside literal insertion text.
15. Curated aliases/templates show actual inserted text when different from the command label.
16. Empty pinned searches distinguish no pins from no matches among existing pins.
17. Identical plugin suggestions are deduplicated, preventing duplicate lazy keys.
18. Plugin keys length-prefix labels so colon-containing labels/snippets cannot collide.
19. Pin actions expose pinned/not-pinned state to accessibility services.
20. Pin/unpin provides feedback; pin-limit messaging uses the authoritative limit.
21. Feedback has a dedicated live-region item brought into view on new messages, including errors from lower results.
22. Feedback/error severity survives recreation without resetting scroll on unchanged feedback.
23. Review dialog content scrolls with large text/limited height and preserves confirmation buttons.
24. Review includes the selected command's specific purpose rather than only a generic warning.
25. Review snippet supports native selection for precise inspection.
26. Exit receives review before insertion because executing it closes the session.
27. Confirmation revalidates availability and uses the canonical current catalog insertion rather than saved snippet text.
28. A shared accepted-insertion guard prevents repeated accepted taps while dismissal completes; failed insertions remain retryable.
29. Empty safe-insertion output produces explicit error feedback instead of silently doing nothing.
30. Clear-recent dialog explanation scrolls on constrained screens and successful clearing gives feedback.

No command executes automatically. Query limits, private-prefix availability, plugin safeInsertion, curated-ID-only persistence, terminal keyboard and PTY lifecycle remain unchanged.

## Review and verification

`git diff --check`: clean. Independent static review caught fixed-header list starvation, restoration reset and offscreen feedback; implementation revised for each. No screenshots produced and no visual completion claimed.

Structural assertion updates: guide dismissal now hides IME before same callback; guide return label becomes Back to commands; warning assertion follows dedicated feedback item and indentation; pin-limit assertion uses constant; insertion assertions follow shared acceptance guard and canonical revalidation. No assertion removed or skipped. These source checks are not runtime evidence.

Testing agent: compile/lint first; run command catalog/search/quick-access and palette contracts, update any additional source-location assertions only after checking semantics. Exercise 320dp/landscape/200% font with IME, rotation during scrolled search and pending review, duplicate plugin labels/colon keys, TalkBack pin/error feedback, repeated accepted insertion, failed insertion retry, exit review cancel/confirm, canonical availability removal, guide return, clear recent. Verify no auto-execution and existing terminal sessions survive.

## Component reference

https://ui.shadcn.com/docs/components/radix/command — grouped searchable command list and explicit empty state; used as an information hierarchy reference, without adding a web dependency.
https://developer.android.com/develop/ui/compose/layouts/flow — native wrapping filter controls.

Next: runtime findings from testing agent, then Terminal visual/interaction audit without replacing keyboard.
