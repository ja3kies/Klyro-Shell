# Terminal session tab action contract

## Goal

Make each session pill behave like the `Role.Tab` it advertises: tapping any tab selects it, while session management is a separate, discoverable action for touch and accessibility. Keep the current pill visuals, session data, terminal keyboard, and switching behavior intact.

## Baseline

- Branch starts from local `f38d8b7` (`design/terminal-warning-diagnostics-action-20261118`).
- PR #47 is still a draft and its hosted head `fa7318a` has a successful Android build run `44356430631`.
- PRs #32 and #33 are still drafts; #33's latest observed run `43792033879` succeeded. PR #32 has no workflow run discoverable for its head in the GitHub connector, so do not describe it as CI-validated.
- Baseline `python3 tools/test_klyro_session_tab_semantics.py`: PASS, but it only guards a handful of structural semantics and misses the active-tab tap mismatch.
- Corrected focused session-tab source suite: 8 PASS; full Python regression suite: 640 PASS.

## 45-point completion checklist

### Contract and state (1–9)

- [x] 1. Reproduce the current active-tab tap opening session management.
- [x] 2. Confirm inactive-tab tap selects exactly that tab.
- [x] 3. Confirm `Role.Tab` is retained for every session pill.
- [x] 4. Make selected and unselected states derive from the same active ID.
- [x] 5. Make every tab tap call only the selection callback.
- [x] 6. Make tapping the selected tab a harmless selection no-op.
- [x] 7. Keep long press as a touch route to session options.
- [x] 8. Add a separately labelled accessibility action for session options.
- [x] 9. Add an explicit More-menu route to manage the active session.

### Component and visual integrity (10–18)

- [x] 10. Extract the pill into a focused Compose component with explicit inputs.
- [x] 11. Keep the current active accent and on-accent colors.
- [x] 12. Keep the current inactive surface and foreground colors.
- [x] 13. Keep the selected pill height and 48 dp minimum interactive area.
- [x] 14. Keep the maximum visual tab width and text ellipsis behavior.
- [x] 15. Keep the unread-output dot for inactive sessions.
- [x] 16. Keep the compact activity badge and its existing color mapping.
- [x] 17. Keep horizontal strip scrolling and selected-tab bring-into-view.
- [x] 18. Pin More and New session actions outside the tab scroller at the eight-tab limit.

### Accessibility contract (19–27)

- [x] 19. Announce the session title and workspace in the tab name.
- [x] 20. Announce selected versus open state without using a misleading edit label.
- [x] 21. Preserve activity speech on the tab state.
- [x] 22. Preserve the unread-output announcement only for inactive sessions.
- [x] 23. Label the primary click as session selection for active and inactive tabs.
- [x] 24. Label long press as session management.
- [x] 25. Expose a `CustomAccessibilityAction` for session management.
- [x] 26. Keep the custom action available whether the tab is selected or not.
- [x] 27. Keep icon-only decoration hidden from accessibility traversal.

### Routing and lifecycle (28–36)

- [x] 28. Route active-tab menu management to the active session ID.
- [x] 29. Hide or disable that menu item when no valid active tab exists.
- [x] 30. Dismiss More before opening the session sheet.
- [x] 31. Route long press to the pressed tab's ID, not the active ID.
- [x] 32. Route the custom accessibility action to the same tab's ID.
- [x] 33. Preserve existing Rename, shell selection, new-terminal, and close actions.
- [x] 34. Preserve sheet cleanup when a session disappears while open.
- [x] 35. Preserve focus restoration and modal lifecycle ownership.
- [x] 36. Verify session switching still changes the visible terminal exactly once.

### Verification and release evidence (37–45)

- [x] 37. Add a red-first regression guard for the current click-label mismatch.
- [x] 38. Add Compose UI coverage for active-tab click behavior.
- [x] 39. Add Compose UI coverage for inactive-tab selection.
- [x] 40. Add Compose UI coverage for long-press session management.
- [x] 41. Add Compose UI coverage for invoking the custom accessibility action.
- [x] 42. Add Compose UI coverage for selected, workspace, activity, and unread state labels.
- [x] 43. Run the full Python source regression suite.
- [ ] 44. Run the exact-commit Android CI and inspect the built APK artifacts.
- [ ] 45. Inspect an installed Pixel capture before assigning a visual score or calling runtime accessibility verified.

## Test-first implementation sequence

1. Add focused source and Compose assertions; prove the new behavior fails against the baseline.
2. Extract the existing pill without changing its visual tokens.
3. Make click always select; add separate long-press, custom action, and active-session menu routes.
4. Run targeted and full regressions, then CI on the isolated branch.
5. Keep the PR draft until the APK has been installed and reviewed on a real Pixel. Do not merge to `main`.

## Design reference

Use the restrained, explicit action hierarchy from Julian Herbst's [Quick Action Tab Bar Exploration](https://dribbble.com/shots/8835387-Quick-Action-Tab-Bar-Exploration) only as a reference for separating the primary tab action from an additional action. Do not copy its animation or add a new shortcut bar to Klyro's terminal.
