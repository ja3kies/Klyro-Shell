# Upstream notices

## Chimera Terminal — PTY implementation

The initial Klyro PTY adapter and native JNI bridge are derived from source code in:

- Repository: https://github.com/ChimeraAnt-DEV/Android-Terminal
- Upstream components: `PtyProcess.java` and `pty_bridge.cpp`
- Local Klyro components: `app/src/main/java/com/oryn/terminal/core/PtyProcess.java` and `app/src/main/cpp/pty_bridge.cpp`

The code was adapted for Klyro's Java package, JNI symbols, and native library name. This notice records provenance; it is not a substitute for the upstream project's terms. Klyro is currently intended for personal use. Review the exact upstream terms and all dependency notices before redistributing source or binaries.


## Termux ARM64 proot runtime

The ARM64 runtime assets are unmodified files extracted from official Termux APT packages. CI verifies the signed Termux stable repository metadata and pins these package versions:

| Package | Version | Upstream source |
|---|---:|---|
| `proot` | `5.1.107.102` | https://github.com/termux/proot |
| `libtalloc` | `2.5.0` | https://www.samba.org/ftp/talloc/ |
| `libandroid-shmem` | `0.7` | https://github.com/termux/libandroid-shmem |

The corresponding Termux package build recipes declare the applicable licences. Copyright/license files are copied into the generated APK assets when supplied by each package. The complete upstream licence terms remain authoritative.

The Termux repository signing keys are sourced from the pinned `termux/termux-packages` keyring revision used by the CI workflow.

Klyro is currently intended for personal use. Review all upstream licence obligations before redistributing the derived application, APK, or source.


## Termux terminal emulator and view

Klyro uses the Termux `terminal-view` library and a vendored `terminal-emulator` module. The transitive emulator is excluded; Klyro builds its JNI and process/input fixes locally:

- Maven coordinate: `com.termux.termux-app:terminal-view:0.118.0`
- Upstream: https://github.com/termux/termux-app
- Upstream module publication: `terminal-view/build.gradle` and `terminal-emulator/build.gradle`
- The Termux application repository is licensed under GPLv3-only. Its license file identifies an Apache-2.0 exception for code originating from Android Terminal Emulator.

The library supplies terminal rendering, VT/ANSI emulation, terminal input handling, clipboard callbacks, and its native PTY implementation. Klyro supplies its own shell command, private paths, palette, and Compose integration. Review and comply with the upstream licence before any redistribution.
