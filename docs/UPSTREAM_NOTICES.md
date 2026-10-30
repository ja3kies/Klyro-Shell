# Upstream notices

## Chimera Terminal — PTY implementation

The initial Oryn PTY adapter and native JNI bridge are derived from source code in:

- Repository: https://github.com/ChimeraAnt-DEV/Android-Terminal
- Upstream components: `PtyProcess.java` and `pty_bridge.cpp`
- Local Oryn components: `app/src/main/java/com/oryn/terminal/core/PtyProcess.java` and `app/src/main/cpp/pty_bridge.cpp`

The code was adapted for Oryn's Java package, JNI symbols, and native library name. This notice records provenance; it is not a substitute for the upstream project's terms. Oryn is currently intended for personal use. Review the exact upstream terms and all dependency notices before redistributing source or binaries.
