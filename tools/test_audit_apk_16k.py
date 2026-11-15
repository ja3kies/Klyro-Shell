import struct
import tempfile
import unittest
from pathlib import Path
from zipfile import ZipFile

from audit_apk_16k import elf_load_alignments, audit


def fake_elf(align: int) -> bytes:
    raw = bytearray(128)
    raw[:4] = b"\x7fELF"
    raw[4] = 2
    raw[5] = 1
    struct.pack_into("<Q", raw, 32, 64)
    struct.pack_into("<H", raw, 54, 56)
    struct.pack_into("<H", raw, 56, 1)
    struct.pack_into("<I", raw, 64, 1)
    struct.pack_into("<Q", raw, 64 + 48, align)
    return bytes(raw)


class NativePageSizeTest(unittest.TestCase):
    def test_aligned_and_unaligned_headers(self):
        self.assertEqual([16384], elf_load_alignments(fake_elf(16384)))
        self.assertEqual([4096], elf_load_alignments(fake_elf(4096)))

    def test_reject_corrupt_headers(self):
        with self.assertRaises(ValueError):
            elf_load_alignments(b"not ELF")
        with self.assertRaises(ValueError):
            elf_load_alignments(fake_elf(16384)[:80])

    def test_apk_scans_every_shared_object(self):
        with tempfile.TemporaryDirectory() as tmp:
            apk = Path(tmp) / "test.apk"
            with ZipFile(apk, "w") as z:
                z.writestr("lib/arm64-v8a/a.so", fake_elf(16384))
                z.writestr("lib/arm64-v8a/b.so", fake_elf(4096))
                z.writestr("assets/data.txt", b"data")
            rows, failures = audit(apk)
            self.assertEqual(2, len(rows))
            self.assertEqual(1, failures)
            self.assertIn("PASS", rows[0][1])
            self.assertIn("FAIL", rows[1][1])


if __name__ == "__main__":
    unittest.main()
