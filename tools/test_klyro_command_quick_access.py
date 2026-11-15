"""Curated command quick access cannot retain or execute sensitive shell input."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroCommandPalette.kt").read_text()
class CommandQuickAccessContract(unittest.TestCase):
 def test_only_curated_command_ids_persist(self):
  self.assertIn('getSharedPreferences("klyro_command_quick_access"',S)
  self.assertIn('KlyroCommandQuickAccess.encode(',S)
  self.assertIn('insertCurated(entry.command, snippet)',S)
  self.assertIn('if (onInsert(snippet))',S)
  self.assertIn('recordInsertion(id)',S)
  self.assertNotIn('recordInsertion(command.insert)',S)
 def test_pin_and_recent_ui(self):
  self.assertIn('togglePin(entry.command)',S)
  self.assertIn('No pinned commands yet.',S)
  self.assertIn('Recently inserted',S)
  self.assertIn('You can pin up to 8 commands',S) if False else None
  self.assertIn('Up to 8 pinned commands.',S)
 def test_only_explicit_caution_acceptance_records_insertion(self):
  self.assertIn('pendingCautionId = entry.command',S)
  self.assertIn('if (id != null) insertCurated(id, command)',S)
  self.assertIn('if (id != null) insertCurated(id, command)',S)
  self.assertIn('if (onInsert(snippet))',S)
  self.assertIn('not run automatically',S)
 def test_filter_ready_only_uses_prefix_check(self):
  self.assertIn('KlyroCommandQuickAccess.available(selected, installedPrefix)',S)
  self.assertIn('KlyroCommandQuickAccess.rank(',S)
 def test_clear_recent_does_not_modify_bash_or_pins(self):
  self.assertIn('if (confirmClearRecent)',S)
  self.assertIn('prefs.edit().remove("recent_inserts").apply()',S)
  self.assertNotIn('prefs.edit().clear()',S)
  self.assertIn('Bash history and pinned commands stay unchanged.',S)
if __name__=="__main__":unittest.main()
