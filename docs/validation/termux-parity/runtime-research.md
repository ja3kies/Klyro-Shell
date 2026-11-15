# Runtime/package research and preservation fix

Research date: 2026-11-14. Repository task base: f63f77e; working branch
feat/termux-parity-reliability. This note records independently checked scope,
not a claim of APK/device validation. No user environment was modified.

## Architecture decision supported by existing implementation

Retain Klyro's private Termux-compatible userland under packaged ARM64 PRoot.
TermuxBootstrapInstaller builds explicit binds from Klyro-owned usr/home to the
compiled Termux virtual paths. TerminalSessionManager clears the host environment
and uses packaged PRoot/loader plus private libtalloc; writable guest state remains
in Klyro filesDir. PRoot is process/path translation, not an extra security sandbox.

Alternatives examined:
- Native own-prefix Termux: requires rebuilding the package dependency closure
  and maintaining a signed repository; renaming applicationId does not relocate
  embedded ELF/shebang/config paths. No evidence justifies discarding this
  already operational QA runtime or migrating existing user files now.
- Debian/glibc PRoot: viable different userland, but changes ABI/package ecosystem
  and Android adapters; it does not establish Termux package/add-on parity.
- Existing private-prefix PRoot: preserves installations and leverages current
  signed Termux packages; APK-UID execution must still be verified, especially
  Android W^X/SELinux and lifecycle restrictions.

## Authoritative sources actually consulted

- https://github.com/termux/termux-packages/wiki/Termux-execution-environment
  (page edited Nov 4, 2026): packages have fixed app/prefix paths; Termux normally
  executes Bionic programs natively; target>=29 app-data execution restrictions
  require specific treatment. Relevant sections: Execution, App Data File
  Execute Restrictions and Path Environment Variables.
- https://github.com/termux/termux-packages/wiki/Building-packages
  documents architecture, dependency builds and the build-package workflow.
- https://developer.android.com/about/versions/10/behavior-changes-10#execute-permission
  provides the official target>=29 writable-app-data execution restriction.
- https://github.com/termux/proot/blob/master/doc/proot/man.1 and
  https://github.com/termux/proot/blob/master/src/execve/enter.c
  were inspected for the actual process/path translation implementation.
- https://manpages.debian.org/bookworm/apt/apt-secure.8.en.html
  documents signed Release/index/package checksum trust. Authentication trusts
  repository maintainers; it does not sandbox package code or make it harmless.
- https://github.com/termux/termux-api#installation
  explicitly requires compatible main-app signing identity for API permissions.
  Installing the CLI package alone cannot establish Klyro API add-on parity.

## Existing trust and recovery mechanisms verified by code inspection

- Bootstrap version/HTTPS URL/SHA-256 pinned; cached archive verified before use.
- Download byte/time bounds, archive entry/expanded-byte/time bounds and traversal
  rejection; symlink destinations validated against private/virtual prefix.
- Signed repository InRelease -> SHA-256 Packages.gz -> SHA-256 pinned .deb
  chain in prepare_proot_runtime.py; APK-native runtime manifest rechecked.
- Bootstrap repair retains usr.previous and refuses overwriting an existing
  backup; interrupted rename recovery exists. Main rejects repair with live PTYs.
- APT/dpkg use their native transaction locks; no code removes locks to force
  progress. Existing production fixture evidence includes interrupted dpkg
  recovery and lock contention, but those mutations were not replayed here.
- Offline first bootstrap download is unavailable unless the verified archive
  already exists. Existing environments do not need network merely to launch.
- Rolling Termux repository retention/pinned version availability remain external
  dependencies; absence must fail explicitly rather than silently repin.

## Confirmed preservation defect and implemented correction

Before this change, generated-profile migration followed .bashrc symlinks during
reads, then replaced the link inode on publication. A dangling link could be
followed by writeIfAbsent and create/overwrite its target. An existing empty
.profile or .bashrc was treated as absent. UTF-8 replacement decoding plus
unbounded readAllBytes could alter invalid user bytes or allocate from huge
profiles. Prior ShellProfileThemeTest checked strings, not these filesystem cases.

TermuxBootstrapInstaller.java now:
- Leaves existing empty, symbolic-linked, hardlinked and nonregular profiles alone.
- Requires a supported unix:nlink==1 view before migration; unsupported topology
  preserves the existing profile. Preserves original POSIX rwx permissions on the
  staged generated migration; unsupported POSIX providers keep the original.
- Uses exclusive CREATE_NEW for absent defaults and preserves race winners.
- Bounds reads to 128 KiB + 1 byte, uses strict UTF-8 and rejects NUL/binary input.
- Preserves linked/nonregular backup paths; never overwrites an existing backup.
- Serializes app migration publication and checks the expected profile snapshot
  both before preparing and immediately before atomic replacement.
- Retains the existing backup/atomic-move mechanism for generated-only changes.

ShellProfilePreservationTest adds 18 real filesystem cases, including valid and
dangling links, empty/nonregular/invalid/large/binary profiles, original backup,
existing user backup, stale user edit, exclusive concurrent default writers and
32 concurrent generated-profile migrations without staging leftovers. New checks
exercise actual hardlinks and preservation of executable/custom rwx permissions.
Java NIO APIs require API26+, while unix:nlink is provider-specific: unsupported
providers skip migration safely. API26-37 APK execution is not inferred from host
JVM filesystem support.
Root runs the consolidated JVM build; this agent did not start Gradle.

Boundary: Java/POSIX rename has no general compare-and-swap against arbitrary
same-UID editors. Snapshot checks preserve edits observed before publication,
but an external editor racing in the final check-to-rename instant is not proven
safe. This is not represented as a complete concurrent-editor guarantee.

## Fresh nonmutating QA runtime revalidation

Evidence: runtime-readonly.py, runtime-readonly.json, runtime-readonly.log.
Existing disposable fixture: .local/production/guest-complete, created by the
prior production campaign. It is not the host PREFIX or an APK installation.
PRoot executable, private library paths and usr/home bind sources were checked
under checkout .local; no host installed Termux binary/library prefix is bound.

30 final checks PASS: guest paths/nonroot UID, apt version, pkg installed list,
dpkg audit, tool versions/availability, Unicode files/pipeline, isolated Git
init/commit/branch, Python/Node Unicode, clang compilation/execution and HTTPS/TLS.
No apt/pkg update, upgrade, install or remove was run. Only an own mktemp project
was created and cleaned. The previous fixture acceptance records were preserved.

An initial harness assumption failed: top -V is unsupported by Termux's Android
system-top wrapper. The original JSON/log remain runtime-readonly-initial.*;
top --help then succeeded. The corrected script is reproducible. That check
proves availability only, not interactive process monitoring or ncurses behavior.

Critical limit: these checks run under host Termux UID 10453 on Android ARM64.
They do not establish execution under com.klyro.shell UID, startup without Termux,
SELinux policy, FGS/background survival, IME interaction, API14-17 coverage, or
physical session persistence. Prior 69 package/development and 12 PTY checks
remain prior evidence, not fresh package installation results from this pass.

## Ledger implications

KLY-02/03/04/05/15 have supported implemented mechanisms and independently
rechecked subcases, plus the concrete profile preservation fix. Whole domains
must remain BLOCKED wherever essential APK/device/install-interruption evidence
is missing. Full Termux add-on, native-execution performance, socket/API and
storage-mount parity remain unproven or absent; PRoot is not claimed equivalent
merely because version commands pass.

NIO reference verified for the preservation follow-up:
https://developer.android.com/reference/java/nio/file/Files#getPosixFilePermissions(java.nio.file.Path,java.nio.file.LinkOption...)
The official API states API26 availability, NOFOLLOW_LINKS support and
UnsupportedOperationException when the provider lacks PosixFileAttributeView.
The unix:nlink extension itself is provider-dependent, so unavailable metadata
causes conservative preservation, not a destructive fallback.

Local hardlink-test precondition: the actual Files.createLink operation was
rejected with AccessDeniedException on this Android host. The test skips only
that exception when /system/bin/linker64 identifies Android; all Linux CI errors
remain failures and Linux must execute the inode assertions. Local result is
SKIPPED, not PASS. The production unix:nlink preservation guard is unchanged.
