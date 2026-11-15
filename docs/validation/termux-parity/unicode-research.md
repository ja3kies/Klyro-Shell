# Unicode width and decoder research

Recorded UTC: 2026-11-14T14:04:43.574443+00:00.
No production source edits or Gradle invocation for this evidence capture.

## Reproduced baseline width lookup

The earlier JShell probe on Klyro's compiled pre-backport engine returned the six
values below. This capture independently reproduced them from the immutable
Klyro source commit `f63f77e1fa047827de3703e0126da89c16d08829` using an isolated javac/java process (Java 17
bytecode target; actual JVM version is in each raw log). This is pure Java width
lookup evidence, not Android rendering or APK execution.

```
java.version=21.0.13
U+1FAE0=1
U+1FAF6=1
U+11F00=1
U+1E4EC=1
U+754C=2
U+301=0
```

Pinned upstream lookup on the same JVM:

```
java.version=21.0.13
U+1FAE0=2
U+1FAF6=2
U+11F00=0
U+1E4EC=0
U+754C=2
U+301=0
```

## Exact provenance and compatibility

- Java source: https://github.com/termux/termux-app/blob/2f5a6f7de663074df829a62e44a23ee70e932763/terminal-emulator/src/main/java/com/termux/terminal/WcWidth.java
- CLI data: https://github.com/termux/wcwidth/blob/911263c514237997c9c590517672c6ea729388dc/wcwidth.c (v4).
- Package recipe: https://raw.githubusercontent.com/termux/termux-packages/91fad586ce5408b89711f478cc5e16598403e653/packages/libandroid-support/build.sh
- Exact interval equality: ZERO_WIDTH 343 and WIDE_EASTASIAN 121; sorted and
  nonoverlapping within each table. Old tables held 324 and 116 intervals.
- The old and pinned Java lookup method tails are identical. No API or algorithm
  replacement is necessary for the table backport. Current master also adds a
  separate helper for its combining-cell bound; Klyro already has its own bound.
- Java intentionally returns zero for C0/C1 width where the CLI C implementation
  returns -1. This longstanding difference is preserved.
- Current package recipe uses libandroid-support 29 plus wcwidth v4. This does
  not prove which library an installed Klyro guest currently loads.

Raw source, logs, and SHA-256 manifest:
`.local/parity/unicode-probe/provenance.json` and sibling files. The pinned Java source SHA-256 is
`778f70dd279d0a1490ef92ec7b66f44d90a55e2d2d442c65b02e9daa61bc32a8`.

## Decoder review after JDK 17 exposed the catalogue dependency

Authoritative references:

- https://www.unicode.org/faq/utf_bom.html
- https://www.unicode.org/faq/private_use.html#nonchar1

UTF mappings include unassigned code points and noncharacters; those values are
not malformed merely because the current JVM has no assigned character entry.
Surrogates and values above U+10FFFF remain invalid scalar values. Replacing the
Character.getType/UNASSIGNED gate with scalar bounds is correct and preserves
forward compatibility on older Android/JVM catalogues.

The revised minimum checks reject overlong two-, three-, and four-byte encodings
at 0x80/0x800/0x10000, including invalid encodings of U+07FF, U+FFFF and ESC.
Valid boundary scalars, noncharacters, invalid recovery and an overlong-ESC ANSI
bypass attempt are tested in Utf8ScalarTest. C1-ignore behavior is unchanged;
terminal control handling means this is not a general lossless Unicode converter.

No new control-sequence bypass or sentinel collision was found in this read-only
review. Structural surrogate and row-width invariants remain enforced. Existing
ZWJ handling remains per-code-point width, not full grapheme-cluster rendering.
No physical Android/font/IME validation was performed. Root owns consolidated
JDK 17/JDK 21 test results; this note does not invent their outcomes.
