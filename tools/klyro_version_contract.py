"""Shared monotonically advancing 0.4.x development version contract."""
import re


def assert_supported_klyro_version(case, gradle_text):
    code_match = re.search(r"\\bversionCode\\s*=\\s*(\\d+)\\b", gradle_text)
    name_match = re.search(r'\\bversionName\\s*=\\s*"0\\.4\\.(\\d+)-dev"', gradle_text)
    case.assertIsNotNone(code_match, "versionCode missing")
    case.assertIsNotNone(name_match, "0.4.x development version missing")
    code = int(code_match.group(1))
    patch = int(name_match.group(1))
    case.assertGreaterEqual(code, 9)
    case.assertGreaterEqual(patch, 2)
    # Historical code 9 <-> 0.4.2. Keep versionName and versionCode aligned.
    case.assertEqual(code, patch + 7)
