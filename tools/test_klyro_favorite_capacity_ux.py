"""Six saved colors must never silently evict another choice."""
import unittest
from pathlib import Path
UI=Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui"
D=(UI/"KlyroColorPickerDialog.kt").read_text()
F=(UI/"KlyroFavoriteColors.kt").read_text()
class FavoriteCapacityTest(unittest.TestCase):
    def test_save_is_disabled_until_user_makes_room(self):
        self.assertIn("canSaveWithoutEviction(",D)
        self.assertIn("enabled = KlyroHexInput.isComplete(hexInput) && hasFavoriteSpace",D)
        self.assertIn("Remove a color before adding another",D)
    def test_model_also_guards_against_unintended_eviction(self):
        self.assertIn("if (!canSaveWithoutEviction(favorites, value)) return favorites",F)
if __name__=="__main__": unittest.main()
