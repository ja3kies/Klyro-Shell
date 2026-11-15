"""All plugin mutations share one lock and imported previews are rechecked atomically."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/plugins/KlyroPluginManager.kt").read_text()
class RegistryAtomicWrite(unittest.TestCase):
 def test_shared_registry_lock_covers_read_modify_write(self):
  self.assertIn("private val registryLock = Any()",S)
  for fun in ("private fun upsert(", "fun installPreview(", "fun setEnabled(", "fun uninstall("):
   body=S.split(fun,1)[1].split("\n    }",1)[0]
   self.assertIn("synchronized(registryLock)",body,fun)
  preview=S.split("fun installPreview(",1)[1].split("fun setEnabled(",1)[0]
  self.assertLess(preview.index("synchronized(registryLock)"),preview.index('require(installed().none'))
 def test_registry_events_support_activity_rotation(self):
  self.assertIn("fun observeRegistry(onChange: () -> Unit): () -> Unit",S)
  self.assertIn('if (key == "manifest") onChange()',S)
  self.assertIn("prefs.unregisterOnSharedPreferenceChangeListener(observer)",S)
if __name__=="__main__":unittest.main()
