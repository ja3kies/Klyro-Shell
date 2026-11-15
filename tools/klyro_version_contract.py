"""Shared 0.4.x baseline contract; CI adds a monotonic run/attempt suffix."""
import re


def assert_supported_klyro_version(case, gradle_text):
    code_match = re.search(r"\bval baseKlyroVersionCode\s*=\s*(\d+)\b", gradle_text)
    name_match = re.search(r'\bval baseKlyroVersionName\s*=\s*"0\.4\.(\d+)-dev"', gradle_text)
    case.assertIsNotNone(code_match, "Klyro base versionCode missing")
    case.assertIsNotNone(name_match, "Klyro 0.4.x base versionName missing")
    code = int(code_match.group(1))
    patch = int(name_match.group(1))
    case.assertGreaterEqual(code, 9)
    case.assertGreaterEqual(patch, 2)
    # Historical baseline code 9 <-> 0.4.2; CI appends a run/attempt offset.
    case.assertEqual(code, patch + 7)
    case.assertIn("versionCode = finalVersionCode", gradle_text)
    case.assertIn("versionName = finalVersionName", gradle_text)
