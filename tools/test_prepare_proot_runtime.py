import unittest
from prepare_proot_runtime import PACKAGES, package_records, release_hash


class SignedRepositoryParsingTest(unittest.TestCase):
    def records(self):
        return "\n\n".join(f"Package: {name}\nVersion: {version}\nArchitecture: aarch64\n"
                            f"Filename: pool/main/{name}.deb\nSHA256: {'a' * 64}"
                            for name, version in PACKAGES.items())

    def test_requires_all_pinned_arm64_packages(self):
        self.assertEqual(set(PACKAGES), set(package_records(self.records())))
        for broken in (self.records().replace("5.1.107.102", "wrong"),
                       self.records().replace("aarch64", "x86_64"),
                       self.records().split("\n\n")[0]):
            with self.assertRaises(ValueError):
                package_records(broken)

    def test_rejects_traversal_absolute_paths_and_missing_hashes(self):
        for filename in ("/data/local/tmp/payload", "pool/../../payload", "https://example.com/pkg"):
            with self.assertRaises(ValueError):
                package_records(self.records().replace("pool/main/proot.deb", filename))
        with self.assertRaises(ValueError):
            package_records(self.records().replace('a' * 64, ""))

    def test_index_must_match_the_sha256_section(self):
        digest = "b" * 64
        release = f"Date: today\nSHA256:\n {digest} 42 main/binary-aarch64/Packages.gz\nSHA512:\n"
        self.assertEqual(digest, release_hash(release, "main/binary-aarch64/Packages.gz"))
        with self.assertRaises(ValueError):
            release_hash(release, "evil/Packages.gz")
        with self.assertRaises((ValueError, IndexError)):
            release_hash(release.replace("SHA256:", "MD5Sum:"), "main/binary-aarch64/Packages.gz")
