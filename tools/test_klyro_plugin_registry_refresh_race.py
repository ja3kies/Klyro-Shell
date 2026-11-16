"""Registry UI refreshes must not display an older result after a newer write."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroPluginsPage.kt").read_text()
class RegistryRefreshRace(unittest.TestCase):
 def test_refresh_key_cancels_prior_snapshot(self):
  self.assertIn('LaunchedEffect(manager, registryRevision)',S)
  self.assertIn('var registryRevision by remember { mutableStateOf(0) }',S)
  self.assertEqual(S.count('scope.launch { registryRevision += 1 }'),2)
  self.assertIn('val stopObserving = manager.observeRegistry',S)
  self.assertIn('onDispose { stopObserving() }',S)
  self.assertNotIn('scope.launch {\n                installed = withContext',S)
 def test_rotation_recovery_cancels_stale_reads_and_accepts_a_valid_snapshot(self):
  self.assertIn('LaunchedEffect(confirmedBundledId, confirmedImportId, confirmedRemovalId, mutationBusy, registryRevision)',S)
  recovery=S.split('delay(10_000L)',1)[1].split('LaunchedEffect(installed,',1)[0]
  self.assertIn('installed = refreshed\n        registryReadFailed = false',recovery)
if __name__=="__main__": unittest.main()
