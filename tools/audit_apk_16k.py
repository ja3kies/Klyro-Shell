#!/usr/bin/env python3
"""Audit every packaged native ELF PT_LOAD segment for Android 16 KiB pages.

This is diagnostic by default: prebuilt third-party ELF files cannot be made
compatible by changing the first-party linker flags. Use --strict in release
gates after all dependencies have been rebuilt.
"""
import argparse
from pathlib import Path
import struct
import sys
from zipfile import ZipFile

PAGE = 16384


def elf_load_alignments(binary: bytes) -> list[int]:
    if len(binary) < 64 or binary[:4] != b"\x7fELF":
        raise ValueError("not an ELF binary")
    if binary[4] != 2:  # ARM64 uses ELFCLASS64
        raise ValueError("not a 64-bit ELF binary")
    endian = "<" if binary[5] == 1 else ">" if binary[5] == 2 else None
    if endian is None:
        raise ValueError("invalid ELF endianness")
    phoff = struct.unpack_from(endian + "Q", binary, 32)[0]
    phentsize = struct.unpack_from(endian + "H", binary, 54)[0]
    phnum = struct.unpack_from(endian + "H", binary, 56)[0]
    if phentsize < 56 or phnum == 0 or phoff + phentsize * phnum > len(binary):
        raise ValueError("invalid ELF program header table")
    aligns = []
    for i in range(phnum):
        offset = phoff + i * phentsize
        if struct.unpack_from(endian + "I", binary, offset)[0] == 1:  # PT_LOAD
            aligns.append(struct.unpack_from(endian + "Q", binary, offset + 48)[0])
    if not aligns:
        raise ValueError("ELF has no PT_LOAD segment")
    return aligns


def audit(apk: Path) -> tuple[list[tuple[str, str]], int]:
    results = []
    failed = 0
    with ZipFile(apk) as archive:
        for name in sorted(archive.namelist()):
            if not name.startswith("lib/") or not name.endswith(".so"):
                continue
            payload = archive.read(name)
            try:
                aligns = elf_load_alignments(payload)
            except ValueError as error:
                status = f"UNKNOWN: {error}"
                failed += 1
            else:
                minimum = min(aligns)
                status = f"PASS: minimum PT_LOAD p_align {minimum}" if minimum >= PAGE else (
                    f"FAIL: minimum PT_LOAD p_align {minimum}"
                )
                if minimum < PAGE:
                    failed += 1
            results.append((name, status))
    return results, failed


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("apk", type=Path)
    parser.add_argument("--report", type=Path)
    parser.add_argument("--strict", action="store_true")
    args = parser.parse_args()
    rows, failed = audit(args.apk)
    lines = [
        "# Native ELF 16 KiB page alignment",
        "",
        f"APK: `{args.apk.name}` — {len(rows)} packaged .so files, {failed} incompatible/unknown",
        "",
        "| Native library | PT_LOAD audit |",
        "| --- | --- |",
    ]
    for name, status in rows:
        lines.append(f"| `{name}` | {status} |")
    lines.extend([
        "",
        "The APK uses legacy native extraction, so ELF PT_LOAD alignment is the",
        "critical native issue. Alignment of first-party targets does not repair",
        "Termux, PRoot or AndroidX third-party prebuilts.",
        "",
    ])
    report = "\n".join(lines)
    print(report)
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(report, encoding="utf-8")
    if failed:
        print(f"::warning::{failed} packaged native libraries do not meet verified 16 KiB ELF alignment",
              file=sys.stderr)
    return 1 if args.strict and failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
