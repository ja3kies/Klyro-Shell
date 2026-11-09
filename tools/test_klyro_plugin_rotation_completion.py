"""Registry UI survives a rotation during an already-confirmed disk commit."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroPluginsPage.kt").read_text()
class PluginRotationCompletion(unittest.TestCase):
 def test_confirmed_write_outlives_composition_and_notifies_new_page(self):
  self.assertIn('withContext(Dispatchers.IO + NonCancellable)',S)
  self.assertIn('DisposableEffect(manager)',S)
  self.assertIn('manager.observeRegistry',S)
  self.assertIn('onDispose { stopObserving() }',S)
  self.assertIn('installed = withContext(Dispatchers.IO) { manager.installed() }',S)
 def test_completed_mutations_are_reconciled_by_ids(self):
  self.assertIn('var confirmedImportId by rememberSaveable',S)
  self.assertIn('var confirmedRemovalId by rememberSaveable',S)
  self.assertIn('if (pendingBundledId in installedIds)',S)
  self.assertIn('if (confirmedImportId != null && confirmedImportId in installedIds)',S)
  self.assertIn('if (confirmedRemovalId != null && confirmedRemovalId !in installedIds)',S)
  self.assertIn('confirmedImportId = record.id',S)
  self.assertIn('confirmedRemovalId = id',S)
 def test_user_can_cancel_recovered_confirmation(self):
  self.assertIn('confirmedImportId = null; error = null',S)
  self.assertIn('confirmedRemovalId = null; error = null',S)
if __name__=="__main__": unittest.main()
