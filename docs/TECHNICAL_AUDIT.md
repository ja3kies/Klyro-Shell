# Technical audit — candidate terminal foundations

**Date:** 2026-10-30  
**Scope:** Oryn Terminal must install alongside stock Termux and own an independent app sandbox and userland.

## Executive finding

**ChimeraAnt-DEV/Android-Terminal is the strongest current prototype candidate.** It already uses a distinct Android application ID (`com.chimeraant.terminal`), has a native PTY bridge, session management, a foreground service, and a proot-based Termux userland strategy.

This is a provisional engineering choice, not a declaration that the project is production-ready. The package-manager path must be validated and hardened before adoption.

## Candidate comparison

| Candidate | Strength | Blocking mismatch / risk | Current assessment |
|---|---|---|---|
| Chimera Terminal | Independent app ID; PTY; per-app private storage; proot path mapping; session service | Old pinned Termux bootstrap; installation integrity and package-update compatibility need verification; app architecture and lifecycle need testing | Best prototype base |
| NewTermux | Mature Termux engine; polished Compose UI; real package ecosystem; tabs | Main app uses `com.termux`; coexist demo uses `com.termux.demo` but explicitly disables real commands | Not suitable as a direct base for safe coexistence |
| Official Termux | Mature terminal engine and package integration | Uses `com.termux` and Termux-specific paths, plugins, and bootstrap assumptions | Valuable upstream reference; fork would require broad identity/path work |

## Chimera: verified repository structure

Repository: https://github.com/ChimeraAnt-DEV/Android-Terminal

Observed components:
- `PtyProcess` and `pty_bridge.cpp`: JNI-backed PTY process creation using `forkpty`, input/output, resize, signals, and process wait.
- `TerminalSession`: process/emulator session wrapper.
- `SessionManager`: creates and tracks multiple sessions.
- `TerminalService`: foreground service intended to keep sessions alive in the background.
- `EnvironmentBuilder`: creates private app-local home/prefix/tmp directories and constructs shell environment variables.
- `TermuxBootstrapInstaller`: downloads and extracts Termux bootstrap, then maps its compiled-in Termux paths using proot.
- Unit tests cover terminal emulator behavior and the expected Termux path mapping.

Build configuration observed:
- Android namespace/application ID: `com.chimeraant.terminal`
- minSdk 26; compileSdk 34
- Java 17
- NDK 27.0.12077973; CMake 3.22.1
- ARM64 and ARM32 product flavors

## Critical risks to resolve before adopting the code

### P0 — Bootstrap age and provenance

The current installer pins `bootstrap-2021.02.19-r1`. This is too old to accept as the basis of a new package-management environment without compatibility testing. It downloads the archive at runtime and the reviewed installer code does not establish a modern, verified checksum/signature contract for the downloaded archive.

Required:
1. Select a currently supported Termux bootstrap release compatible with the app's Android target and device ABI.
2. Pin the exact release and expected SHA-256 per ABI.
3. Verify the checksum before extraction.
4. Make installation atomic: extract to staging, validate, then activate; recover safely from interruption.
5. Test `pkg update`, install, upgrade, remove, and reinstall on a clean ARM64 device.

### P0 — proot path mapping and isolation

The Termux binaries expect `/data/data/com.termux/files/usr` and `/data/data/com.termux/files/home`. Chimera maps its own private app-local directories onto those paths inside proot. This is the mechanism that allows a different Android application ID to use the Termux-built userland without writing into the real Termux app's data.

Required tests:
- Confirm every bind source resolves inside Oryn's own private data directory.
- Confirm no operation creates, reads, or writes the actual Termux app's private data.
- Validate absolute symlinks, relative symlinks, executable permissions, ELF interpreter paths, and shared-library loading.
- Validate `pkg`/APT maintainer scripts and package upgrades under the mapped prefix.
- Test spaces, Unicode, interrupted downloads, low storage, and corrupted archives.
- Review every bind mount; do not expose broad host paths without a clear requirement.

### P1 — session lifecycle

The code has a PTY and a foreground service, but persistence must be tested rather than inferred from the service class existing.

Required tests:
- Background/foreground transitions.
- Screen off, process pressure, activity recreation, and app process death.
- Session metadata restoration and detection of dead child processes.
- Closing one tab must not terminate other sessions.
- Service notification and Android foreground-service restrictions on current Android versions.

### P1 — app identity and hardcoded values

Before changing package identity, search the full project for:
- `com.chimeraant.terminal`
- `com.termux`
- notification/action/provider authorities
- JNI package/class method names
- filesystem paths, preferences, and file-provider authorities

The JNI symbol names must be updated together with Kotlin package/class renames if those classes move.

### P1 — security and maintenance

- Review archive extraction against path traversal and symlink attacks.
- Add download timeouts, retry policy, size limits, checksum validation, and clear failure recovery.
- Pin dependencies and document the upstream commit used as the base.
- Review the project's personal-use licence statement and all third-party notices before copying code. Do not publish or redistribute a derived APK/source until the applicable terms are clear.

## Why NewTermux is not the direct base

Repository: https://github.com/The412Banner/NewTermux

Its README states that the main app installs as `com.termux` and asks users to uninstall an existing Termux before installing it. Its separate coexist demo uses `com.termux.demo`, but the README and installer explicitly state that this mode runs a harmless fake shell and does not execute real commands. That conflicts with Oryn's core coexistence and real-shell requirements.

## Next verification gate

Do not begin a broad UI rewrite yet. First validate Chimera's bootstrap and proot pipeline with a minimal ARM64 prototype:
1. Rename the Android identity to `com.oryn.terminal` without touching the existing Termux installation.
2. Build and install alongside Termux.
3. Verify Oryn's private paths and confirm the real Termux directories remain untouched.
4. Install a current, checksum-pinned Termux bootstrap.
5. Run `bash`, `pkg update`, `pkg install git python openssh`, and a package upgrade.
6. Reboot/background test multiple independent PTY sessions.

Only after this gate passes should the UI be redesigned around the Neon Obsidian system.
