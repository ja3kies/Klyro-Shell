"""A pending color deletion keeps its identity through rotation, never auto-deletes."""
import unittest
from pathlib import Path
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroColorPickerDialog.kt").read_text()
class FavoriteDeleteRotationTest(unittest.TestCase):
    def test_confirmation_uses_saveable_hex_not_unparcelable_object(self):
        self.assertIn("var pendingFavoriteDeletionHex by rememberSaveable",S)
        self.assertIn("favorites.firstOrNull { it.hex == pendingFavoriteDeletionHex }",S)
        self.assertIn("pendingFavoriteDeletionHex = favorite.hex",S)
        self.assertNotIn("pendingFavoriteDeletion = favorite",S)
        self.assertIn("saveFavorites(favorites.filterNot { it.hex == favorite.hex })",S)
    def test_confirmation_stays_explicit(self):
        self.assertIn('title = { Text("Remove saved color?") }',S)
        self.assertIn('pendingFavoriteDeletion?.let { favorite ->',S)
if __name__=="__main__": unittest.main()
