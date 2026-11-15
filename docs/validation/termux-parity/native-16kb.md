# Native ELF 16 KiB page alignment

APK: `klyro-shell-signed.apk` — 7 packaged .so files, 0 incompatible/unknown

| Native library | PT_LOAD audit |
| --- | --- |
| `lib/arm64-v8a/libandroid-shmem.so` | PASS: minimum PT_LOAD p_align 16384 |
| `lib/arm64-v8a/libandroidx.graphics.path.so` | PASS: minimum PT_LOAD p_align 16384 |
| `lib/arm64-v8a/liboryn_pty.so` | PASS: minimum PT_LOAD p_align 16384 |
| `lib/arm64-v8a/libproot-loader.so` | PASS: minimum PT_LOAD p_align 16384 |
| `lib/arm64-v8a/libproot.so` | PASS: minimum PT_LOAD p_align 16384 |
| `lib/arm64-v8a/libtalloc.so` | PASS: minimum PT_LOAD p_align 16384 |
| `lib/arm64-v8a/libtermux.so` | PASS: minimum PT_LOAD p_align 16384 |

The APK uses legacy native extraction, so ELF PT_LOAD alignment is the
critical native issue. Alignment of first-party targets does not repair
Termux, PRoot or AndroidX third-party prebuilts.
