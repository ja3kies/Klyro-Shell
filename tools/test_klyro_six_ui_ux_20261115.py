"""Six UI/UX regression contracts; source-only, not Pixel screenshot assertions."""
import unittest
from pathlib import Path

UI = Path(__file__).resolve().parents[1] / "app/src/main/java/com/oryn/terminal/ui"
W = (UI / "KlyroWorkspaceSheet.kt").read_text(encoding="utf-8")
P = (UI / "KlyroCommandPalette.kt").read_text(encoding="utf-8")
S = (UI / "KlyroSessionSheets.kt").read_text(encoding="utf-8")
T = (UI / "TerminalPane.kt").read_text(encoding="utf-8")


class KlyroSixUiUx(unittest.TestCase):
    def test_ui_workspaces_fixed_header_and_measured_scroll_body(self):
        area = W.split("KlyroSheet(onDismissRequest = onDismiss,", 1)[1].split("if (editorId != null)", 1)[0]
        self.assertIn('KlyroSheetTitle("Workspaces"', area)
        self.assertIn('Text("New", color = tint)', area)
        self.assertIn(".weight(1f, fill = false)", area)
        self.assertIn(".verticalScroll(workspaceScroll)", area)
        self.assertIn(".navigationBarsPadding()", area)
        self.assertIn(".imePadding()", area)
        self.assertLess(area.index('Text("New", color = tint)'), area.index(".verticalScroll(workspaceScroll)"))
        self.assertNotIn('rows = entries.size + projectTabs.size', W)

    def test_ui_palette_material_filter_chips_have_visible_and_semantic_states(self):
        self.assertIn("FilterChip(", P)
        self.assertIn("FilterChipDefaults.filterChipColors(", P)
        self.assertIn("selected = pinnedOnly", P)
        self.assertIn("selected = availableOnly", P)
        self.assertIn("modifier = Modifier.weight(1f)", P)
        self.assertIn("KlyroSettingsChoiceGrid(columns = 2, selectionGroup = false)", P)
        self.assertIn("pinnedOnly = !pinnedOnly", P)
        self.assertIn("availableOnly = !availableOnly", P)

    def test_ui_session_details_pinned_identity_and_explicit_done(self):
        a = S.split("internal fun KlyroSessionSheet(", 1)[1].split("internal fun KlyroRenameSessionDialog(", 1)[0]
        self.assertIn("KlyroSheetTitle(sessionName.ifBlank", a)
        self.assertIn('Text("Done")', a)
        self.assertIn("Modifier.weight(1f, fill = false)", a)
        self.assertIn(".verticalScroll(rememberScrollState())", a)
        self.assertIn(".navigationBarsPadding()", a)
        self.assertLess(a.index('Text("Done")'), a.index(".verticalScroll(rememberScrollState())"))
        self.assertIn("closeTargetId = sessionId", a)
        self.assertIn("if (confirmClose)", a)

    def test_ux_workspace_editor_focus_and_guarded_ime_done(self):
        self.assertIn("val editorFocus = remember { FocusRequester() }", W)
        self.assertIn("LaunchedEffect(editorId)", W)
        self.assertIn("editorFocus.requestFocus()", W)
        self.assertIn("focusRequester(editorFocus)", W)
        self.assertIn("KeyboardOptions(imeAction = ImeAction.Done)", W)
        self.assertIn("KeyboardActions(onDone = { if (canSave) saveWorkspaceEdit() })", W)
        self.assertIn("val canSave = editorText.trim().isNotEmpty()", W)
        self.assertIn("onClick = { saveWorkspaceEdit() }", W)
        self.assertIn("if (saved)", W)

    def test_ux_rename_ime_done_uses_same_validation_as_save(self):
        a = S.split("internal fun KlyroRenameSessionDialog(", 1)[1].split("internal fun KlyroChooseShellSheet(", 1)[0]
        self.assertIn("KeyboardOptions(imeAction = ImeAction.Done)", a)
        self.assertIn("KeyboardActions(onDone = { saveRename() })", a)
        self.assertIn("val canSave =", a)
        self.assertIn("if (!canSave) return", a)
        self.assertIn("onClick = { saveRename() }", a)
        self.assertIn("selection = TextRange(", a)
        self.assertIn("rememberSaveable(name, stateSaver = TextFieldValue.Saver)", a)

    def test_ux_selected_tab_uses_actual_relocation_not_index_approximation(self):
        a = T.split("val tabStripScroll = rememberScrollState()", 1)[1].split("if (sessionFailureMessage != null)", 1)[0]
        self.assertIn("BringIntoViewRequester()", a)
        self.assertIn("bringIntoViewRequester(", a)
        self.assertIn("bringIntoView()", a)
        self.assertIn("LaunchedEffect(", a)
        self.assertNotIn("tabStripScroll.maxValue.toLong() * selectedIndex", a)
        self.assertIn(".horizontalScroll(tabStripScroll)", a)
        self.assertIn("onClick = onNewTab", a)
        tab = (Path(__file__).resolve().parents[1] /
               "app/src/main/java/com/oryn/terminal/ui/KlyroTerminalSessionTab.kt").read_text()
        self.assertIn("role = Role.Tab", tab)
        self.assertIn("selected = active", tab)


if __name__ == "__main__":
    unittest.main()
