# Workspaces — draft safety and action design

User-assigned split: implementation here; Android tests and acceptance by another
agent. No main merge. Base integrates Settings controls e9aa02f and Appearance
06f7bcd. The empty-state merge conflict retains the full-width action, spacing,
chevron and semibold typography. Existing accessibility assertions retained.

## Implemented

- Preserve an active create/rename draft: workspace selection, session switching,
  overflow menu and moving a session are disabled until Save or Cancel.
- Guard editProject itself against replacing an existing draft.
- New is an outlined 48dp-minimum button, accessible as New workspace.
- Workspace entries are a single-selection semantic group.
- Selected entry has a checkmark, not only an accent color.
- Workspace names may wrap to two lines before ellipsis.
- Overflow uses native IconButton and retains the workspace-specific label.
- Save action says Create workspace or Save name according to its effect.
- Editor actions wrap at large text rather than squeezing Cancel.
- Move current session here uses an outlined button with the existing callback.
- Latest upstream Appearance accent grouping and compact font presets integrated.

Root cause: the sheet already prevented swipe dismissal during editing, but
other callbacks could replace the editor or switch a session and call onDismiss.
The guards now cover these sibling paths without changing persistence or PTYs.

## Required validation

1. Begin a new workspace draft, type text, try workspace selection, session switch,
   overflow and move: none discards or modifies the draft.
2. Cancel restores navigation; Create creates exactly one workspace and restores
   navigation; failed save retains text and the existing error.
3. Repeat with rename, IME Done, rotation and background/resume.
4. Confirm stale rename/delete targets and max-workspace behavior remain intact.
5. 320dp and 200% fonts: New, Cancel and Create workspace remain reachable; long
   names wrap; editor focus and keyboard scroll into view.
6. TalkBack announces one workspace selection group, selected state, New workspace
   and Options for [name]; ordinary workspace navigation still works after save.
7. Verify terminal sessions and files survive deletion/move exactly as before.

Executed here: git diff --check; independent static review reported no blocking
code defect. No Android build/tests/captures or user validation claimed.

Reference researched after changes:
https://ui.shadcn.com/docs/components/base/item
Useful idea: separate a row's identity/content from its explicit secondary action.
Adapted using native Compose selection rows plus IconButton, not web components.
