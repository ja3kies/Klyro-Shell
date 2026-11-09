"""The status title and its badge must not compete for one narrow row."""
from pathlib import Path
import unittest
MAIN = (Path(__file__).resolve().parents[1] /
        "app/src/main/java/com/oryn/terminal/MainActivity.kt").read_text()
class RuntimeBadgeResponsiveTest(unittest.TestCase):
    def test_badge_stacks_under_section_header_and_allows_two_lines(self):
        p = MAIN.split('Text("RUNTIME STATUS"', 1)[0]
        self.assertIn("Column(", p[-300:])
        q = MAIN.split('Text("RUNTIME STATUS"', 1)[1].split("homeStatus.runtimeMessage",1)[0]
        self.assertIn("maxLines = 2", q)
if __name__ == "__main__":
    unittest.main()
