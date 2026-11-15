# Termux parity engineering evidence — 2026-11-15

**Production parity is not established.** This is a test build, not a claim of
readiness for primary use. Whole-priority statuses follow the requested Definition
of Done; successful subchecks do not make a domain VALIDATED.

Base: `f63f77e1fa047827de3703e0126da89c16d08829`, the latest remote modal branch
when work began (three commits newer than the earlier local delivery). Dedicated
branch: `feat/termux-parity-reliability`. Initial tree clean; no merge, release,
data reset, package uninstall, permission broadening or user-process termination.
Existing Freeze Recorder and latest stress-launch fixes are retained.

## Priority ledger

The machine-readable ledger is [ledger.json](validation/termux-parity/ledger.json).

| Priority | Status | Evidence still required / implementation |
|---|---|---|
| KLY-01 Android validation | BLOCKED | Actual authorized ARM64 API34–37 devices; new repeatable runner and APK-UID runtime tests ready |
| KLY-02 Bootstrap/packages | BLOCKED | Complete fresh install/transactions under APK UID; 30 current host-QA readonly checks pass |
| KLY-03 Runtime contract | BLOCKED | Private-prefix PRoot retained; target36 SELinux/execution and Termux-free device proof absent |
| KLY-04 Installation resilience | BLOCKED | Linked/binary/concurrent profile preservation implemented; real Android interruption/disk-full/upgrade cases absent |
| KLY-05 Runtime/package security | BLOCKED | Existing signed downloads/extraction safeguards retained; actual APK download/interruption/extraction permission paths unverified |
| KLY-06 PTY correctness | BLOCKED | Native host tests pass; exited output retained; real Android final-tail/input/editor scenarios absent |
| KLY-07 Session persistence | BLOCKED | Live/finished ownership and FGS counts corrected; Home/lock/recents/recreation/memory cases absent |
| KLY-08 Modals/diagnostics | BLOCKED | Prior corrections preserved; isolated 1500-line lifecycle/touch checks need actual device |
| KLY-09 Input/rendering | BLOCKED | ZWJ/ZWNJ paste correction and security tests; IME, graphemes, hardware keys/selection unverified |
| KLY-10 Performance | BLOCKED | Notification backpressure implemented; Android UI/jank/CPU/memory/prolonged metrics unavailable |
| KLY-11 Storage | BLOCKED | Existing SAF integrity layer preserved; actual provider/revocation/rotation not exercised |
| KLY-12 Networking/SSH | BLOCKED | Current QA HTTPS/tools and earlier isolated SSH proof; APK LAN/VPN/IPv6/server/forwarding absent |
| KLY-13 State restoration | BLOCKED | Stable finished IDs and live counts; actual reattachment/process-death tests absent |
| KLY-14 Backup/restore | NOT STARTED | No complete user-controlled environment portability workflow; not simulated by SAF single-file export |
| KLY-15 Package recovery | BLOCKED | Existing signed APT integration; actual APK upgrade/mirror/interruption/background transactions absent |
| KLY-16 Upstream compatibility | BLOCKED | Upstream exit behavior compared; terminal-view/fork ABI prevents an independent blind dependency upgrade |
| KLY-17 Release engineering | BLOCKED | Signed/reproducible delivery proven below; actual installation/platform compatibility not exercised |
| KLY-18 Android17 | BLOCKED | Official changes reviewed; actual APK API37 memory/IME/network/lifecycle proof absent |
| KLY-19 UI/accessibility | BLOCKED | Existing issue13 retained; physical system bars/themes/TalkBack/large-font evidence missing |
| KLY-20 Diagnostics/plugins | BLOCKED | Existing privacy/trust mechanisms retained; complete platform/add-on integration not established |

Dependency order matters: KLY-01/03 determine whether the current runtime works
under the real application sandbox. Host execution cannot settle that decision.
KLY-02/04/06/07/10 consequently cannot be closed by more host builds. KLY-14 is
an absent product capability, not an externally verified success or a completed
backup system. No P1/P2 redesign was substituted for the unfinished P0 gates.

## Confirmed defects implemented

1. **User profile preservation.** `writeIfAbsent` followed dangling links and
   overwrote existing empty files. Generated `.bashrc` migration replaced links
   with regular files, could decode binary/oversized content, race another app
   migration or replace a user edit, and lose executable/custom permissions.
   Defaults now use exclusive creation. Linked/nonregular/empty/binary/invalid
   UTF-8/over-128KiB profiles are preserved. Hardlinked/unknown-topology profiles
   are preserved. Generated-only publication is serialized, snapshot-checked,
   backup-preserving and retains POSIX rwx mode. Unsupported providers preserve
   the original. Profile migration moved from Activity startup into explicit
   runtime verification's IO dispatcher; first installation still creates defaults.
   Legacy profiles gain new generated helpers only after explicit verification.
   Eighteen real-filesystem cases cover these paths. Actual hardlink creation is
   denied on this Android host: that precondition is SKIPPED locally, never PASS;
   Linux CI must still execute the inode assertions. A concurrent external editor
   can still race the final snapshot-check→rename interval; Java/POSIX provides no
   general compare-and-swap pathname rename. No absolute concurrent-editor guarantee.

2. **Exited sessions lost their transcript.** Every exit previously called closeTab,
   even a failure. The tab/emulator/exit code now remain until explicit close;
   siblings retain identity and selection; duplicate/stale callbacks cannot target
   another tab; pending paste is cleared. Finished rendering can resize without
   ioctl on a closed PTY. UI receives explicit finished IDs and shows DONE; a
   completed-tab confirmation describes transcript removal instead of claiming a
   live process will be stopped. Open tabs remain navigable while only live/starting
   tabs block install/repair and own the service. Existing service notifications
   update on natural/explicit close without starting an FGS from background.
   Model/listener/resize and Compose cases are implemented, compiled, not executed.
   Adjacent Android Back confirmation also permits explicit removal of a finished
   tab (identity is still checked), explains retained-output loss and preserves
   Cancel. A real TerminalPane/Android Back regression fixture covers that route.

3. **Input-rejection notices were unbounded.** Each failed enqueue posted another
   Runnable/Toast. Rejections now coalesce behind one cached callback with an
   immediate first notice and four-second monotonic cooldown. Concurrent producers,
   reentrant notice delivery, trailing delivery, background/resume and disposal are
   handled; byte rejection/atomicity is unchanged. The application context is used.
   Nine scheduler/concurrency JVM tests include 80,000 producer rejections.

4. **Legitimate joined text was rejected on paste.** ZWJ/ZWNJ are required for
   emoji/script shaping. The clipboard policy now accepts those join controls;
   bidi overrides/isolates, ESC/control bytes, hidden separators, malformed UTF-16
   and byte limits remain protected. Multiline review and bracketed atomic paste
   are unchanged. Unit cases cover family/profession emoji, Persian/Indic text,
   combining accents and security-boundary combinations. This proves accepted
   input bytes, not grapheme rendering or physical IME behavior.

5. **Unicode cell widths diverged from Termux CLI tables.** The compiled baseline
   returned1 for U+1FAE0/U+1FAF6 (must be2) and U+11F00/U+1E4EC (must be0).
   Exact Unicode15 tables from upstream commit2f5a6f7 are backported without
   changing lookup methods/API. All343 zero and121 wide ranges match official
   termux/wcwidth v4, the libandroid-support package baseline. Seven tests cover
   sorted/disjoint tables, widths, split UTF-8/DSR cursor, wrapping and reflow/copy.
   This is not a claim of Unicode18 or whole-grapheme shaping; ZWJ clusters retain
   existing per-code-point cell advance. References:
   [pinned Java source](https://github.com/termux/termux-app/blob/2f5a6f7de663074df829a62e44a23ee70e932763/terminal-emulator/src/main/java/com/termux/terminal/WcWidth.java),
   [CLI v4](https://github.com/termux/wcwidth/blob/v4/wcwidth.c),
   [package recipe](https://github.com/termux/termux-packages/blob/master/packages/libandroid-support/build.sh).

6. **UTF-8 validity depended on platform Unicode data.** CI Java17 revealed
   three cursor/wrap/reflow failures absent on local Java21. The decoder used
   Character.UNASSIGNED to replace valid scalar values unknown to that runtime.
   It now validates scalar range/surrogate exclusion directly, independently of
   assignment catalogues, and rejects overlong boundary encodings. Five added
   cases preserve unassigned/noncharacters, test valid boundaries, reject encoded
   surrogates/above-maximum/overlong input, and ensure disguised ESC cannot become
   an ANSI command. The old test invariant was incorrect: assignment is not UTF
   validity. It is replaced by scalar validation; surrogate pairing and screen
   widths remain enforced. Java17 and21 runs now pass, as does CI. Reference:
   [Unicode UTF roundtrip and malformed sequences](https://www.unicode.org/faq/utf_bom.html).
   Existing C0/C1 terminal control interpretation remains; this is not a generic
   lossless transcoder. The failed CI remains linked in the final test evidence.

7. **Device tests used null session callbacks.** Two old resume fixtures passed
   null after production diagnostics began reading session.getPid(). They now use
   real unstarted session objects; assertions remain. Compilation alone had not
   exposed that runtime fixture defect.

8. **Deliberate freeze guard allowed retained output.** `hasLiveSessions()` intentionally excludes completed tabs, but Freeze Recorder said “no open sessions” while checking only live PTYs. An artificial 6/11-second main-thread block could therefore trigger with a completed transcript still open. Added `hasOpenSessions()` for UI eligibility, both pre-execution checks and the instrumentation assumption; service/runtime guards continue using `hasLiveSessions()`. The retained-session regression test proves the ended tab is open while not live and becomes closed only after explicit close. CI passes. Device execution of the deliberate stall remains blocked.

## Executable Android acceptance

`tools/run_android_acceptance.py` requires a new evidence directory, exact APKs,
full source SHA, one authorized device (or --serial), API34–37, ARM64, matching
installed APK hashes and the correct runner. It never installs, clears data,
force-stops, changes permissions or closes app sessions. Instrumentation itself
can restart the target, so `--dedicated-device` is mandatory before execution;
finish all user jobs and use a dedicated test environment first.

The selected classes cover real modal windows/InputDispatcher taps, native PTY,
resume batching, finished-session state/UI, and opt-in installed runtime behavior.
`InstalledRuntimeAcceptanceTest` starts its own unregistered PTY in the APK UID,
checks guest UID/paths, filesystem/Unicode, pipelines/redirection, executable
scripts and readonly apt/pkg; a second case executes essential developer tools.
It bypasses user startup profiles, uses only its own mktemp work, and closes only
its own test handle. It does not install missing tools or simulate success.

```sh
python3 tools/run_android_acceptance.py \
  --source-commit FULL_CODE_SHA \
  --app-apk /path/klyro-shell-signed.apk \
  --test-apk /path/klyro-shell-tests-signed.apk \
  --output /path/to/NEW-evidence-directory --dedicated-device
```

Run separately on API34,35,36,37; one device does not validate the matrix. A skip,
failed case, zero/mismatched count, missing final result, process crash, timeout
or output overrun stays BLOCKED. Logs are capped at 4MiB and execution at 600s;
only the owned host adb client is terminated on timeout/overrun. Android tests may
still run, explicitly reported; no force-stop is issued. Source SHA is caller
linkage to CI metadata, not independently decoded from APK. Installed hashes
prove supplied APK identity. Sixteen Python tests include real bounded flood,
timeout and unrelated-process preservation checks.

Current actual preflight: **BLOCKED**, exactly no authorized ADB device. No
emulator, screenshot, interaction, PID continuity or physical installation PASS
is inferred. Native host tests run on Android ARM64 under Termux UID, not Klyro.

## Research and architecture decision

The maintained private-root PRoot design is retained. It exposes the original
compiled Termux paths inside a guest backed only by Klyro data, with a cleared
host environment and APK-provided loader/dependencies. Native own-prefix builds
would require rebuilding dependency closure and operating a signed package repo.
Debian/PRoot would change package ABI, ecosystem and migration semantics. Neither
is a justified blind replacement before measuring the existing APK sandbox path.
PRoot is not a security sandbox and does not prove target36 execution/SELinux.

Authoritative sources actually consulted:

- [Termux execution environment](https://github.com/termux/termux-packages/wiki/Termux-execution-environment),
  [building packages](https://github.com/termux/termux-packages/wiki/Building-packages),
  [Android execution restrictions](https://developer.android.com/about/versions/10/behavior-changes-10#execute-permission).
- [APT signed metadata trust](https://manpages.debian.org/bookworm/apt/apt-secure.8.en.html)
  and [upstream pkg](https://github.com/termux/termux-tools/blob/master/scripts/pkg.in).
  Signature integrity is not an audit of package code.
- [Termux Activity exit policy](https://raw.githubusercontent.com/termux/termux-app/master/app/src/main/java/com/termux/app/terminal/TermuxTerminalSessionActivityClient.java).
  Upstream retains unexpected failures but removes statuses0/130 and some plugin
  results; Klyro intentionally retains all exits for stronger output preservation.
- [Termux API](https://github.com/termux/termux-api#installation),
  [Boot](https://github.com/termux/termux-boot), [Widget](https://github.com/termux/termux-widget):
  separate Android integrations/app identity/signatures; a CLI package is not parity.
- [Android tests](https://developer.android.com/training/testing),
  [FGS types](https://developer.android.com/develop/background-work/services/fgs/service-types),
  [SAF](https://developer.android.com/training/data-storage/shared/documents-files),
  [16KiB](https://developer.android.com/guide/practices/page-sizes).
- [API37 overview](https://developer.android.com/about/versions/17/summary),
  [all-app changes](https://developer.android.com/about/versions/17/behavior-changes-all),
  [targeted changes](https://developer.android.com/about/versions/17/behavior-changes-17).
  Memory limits, rotation IME and global shortcuts need actual device tests.
  Compile/target36 remains; no speculative permissions/target switch.
- [Unicode emoji joiners](https://www.unicode.org/reports/tr51/),
  [bidi formatting](https://www.unicode.org/reports/tr9/).

Issue [#13](https://github.com/ja3kies/Klyro-Shell/issues/13) is still the UI/system-bar
tracking issue, not a duplicate or a claim of visually fixed behavior.

## Actual runtime evidence and limits

A fresh readonly recheck of the pre-existing **QA-only** rootfs executed 30 cases:
path/nonroot identity, apt/pkg/dpkg queries, installed tools, temporary Unicode
files and Git commit/branch, Python/Node, clang compilation, HTTPS/TLS. No package
update/install/upgrade/remove. Fixture launch argv hash and host UID are recorded.
This is current availability of an older installed fixture, not a new APK install
or execution of the new profile migration. The initial unsupported `top -V`
failure is retained; `top --help` proves availability only. Earlier 69 package
transaction/12 interactive PTY results remain historical, not rerun or relabelled.

The native JNI harness reran twenty PTYs, 20,000 output lines, actual signals,
EINTR, UTF-8 argv/env/cwd and NUL rejection; fd count stayed4→4. This does not
establish app UI performance or all Android process-lifetime guarantees.

Remaining probable P0 risk: a reader/join/exit-handler race may lose final output
when the main looper stalls or descendants hold output open. No physical trace
currently confirms it; no speculative drain rewrite was made. Reproduce with a
real isolated Looper/PTY and exact final markers before declaring it resolved.

Android dismissal/focus/jank/startup/resume, CPU, GC, memory pressure, LAN/VPN,
SAF and hours-long stability measurements: **NOT MEASURED**. No benchmark gains
are inferred from unit tests or host package versions.

## Unresolved differences from Termux

- APK-UID bootstrap/package execution and Termux-free first installation unproven.
- PRoot/ptrace overhead, target36 sandbox compatibility and full package coverage
  differ from Termux's native prefix; package Android service integration is absent.
- Termux API/Boot/Widget and full automation intents are not implemented equivalents.
- Terminal-view0.118.0/fork ABI and Unicode/grapheme/keyboard behavior are not current
  upstream parity; independent artifact upgrades can break JNI/resize integration.
- Complete environment backup/restore is absent. SAF single-file export is not it.
- Android14–17 lifecycle, accessibility, keyboard, provider, network and prolonged
  performance evidence is missing. Force-stop/reboot cannot preserve live PTYs;
  no fake process restoration is promised.

## Tests, commits and delivery

Code SHA: `92be2da98055d988bbdc89ceb85f61e405ff9487`.
The final commits and documentation are included in the evidence report; source CI ran on this exact SHA.

| Verification | Actual result |
|---|---|
| Local app JVM | 366 cases:365 PASS,1 SKIPPED (Android hardlink-creation denial),0 failure/error |
| Local emulator JVM | 46 PASS under Java17 and explicitly rerun under Java21 |
| CI app/emulator JVM | 366+46=412 PASS,0 skipped/failure/error on both successful runs for final source `92be2da` |
| Python | 523 PASS; mixed behavioral tests and pre-existing source contracts, not physical proof |
| Native C/JNI | PASS,20 PTYs,20,000 lines,fd4→4; host Termux UID |
| QA fixture readonly recheck | 30 PASS, no package transaction; historical installed fixture only |
| AndroidTest Kotlin/Java/APK | Compilation PASS; actual execution BLOCKED |
| Lint | 45 warnings+11 hints unchanged by file/message from source CI; no suppression |
| ARM64/debug/test/release compilation | PASS; signed debug/test APKs delivered; no release publication |
| Signatures/permissions | v2/v3 PASS, app/test same existing Klyro certificate; five permissions unchanged |
| ELF/ZIP | Seven ELF PT_LOAD alignments≥16KiB PASS; zipalign-c4 PASS; no actual16KiB-device claim |
| Same-source reproducibility | Two independent hosted builds produce byte-identical app AND test APKs |
| Physical scenarios / Android metrics | BLOCKED / NOT MEASURED |

The earlier initial failed run is retained:
[43602091722](https://github.com/ja3kies/Klyro-Shell/actions/runs/43602091722)
(three Unicode cursor/reflow/wrap failures under Java17). Fix and new scalar tests
passed locally on17/21, then on both fresh successful hosted runs. The assertions
that found the failure were retained. Existing compiler deprecation/_GNU_SOURCE
warnings remain; no unrelated warning suppression was introduced.

No production-readiness status is granted from CI alone.

Commits:

- [3160b12](https://github.com/ja3kies/Klyro-Shell/commit/3160b12cd608871a6fb2754389547cd8962a3218) — fix(runtime): preserve user profile links, bytes and modes during migration
- [2b0a7ea](https://github.com/ja3kies/Klyro-Shell/commit/2b0a7ea045bc99b1d52e26f0026c03f170850f1a) — fix(sessions): retain completed output and bound input failure notifications
- [fa7f690](https://github.com/ja3kies/Klyro-Shell/commit/fa7f690ffcf5d803f0e8f1712ecfbc5deca7fcf7) — test(android): gate real APK runtime acceptance on dedicated source-linked devices
- [b769cdf](https://github.com/ja3kies/Klyro-Shell/commit/b769cdfb7df1aedcb8ed99e093758affb94a365b) — fix(unicode): align cell widths with Termux v4 and preserve joiner pastes
- [e4718ae](https://github.com/ja3kies/Klyro-Shell/commit/e4718ae8a52a0870dade09fae0dd17a5812338f2) — fix(sessions): allow explicit Back-confirmed removal of finished output
- [874beef](https://github.com/ja3kies/Klyro-Shell/commit/874beef0b376905e8e8fbd5dddfb3472a6fa1f40) — ci: build parity reliability branch as version 0.4.12-dev
- [712602e](https://github.com/ja3kies/Klyro-Shell/commit/712602e5ea415642e5b2a6da0c18eeeff358310f) — fix(terminal): decode UTF-8 scalars independently of platform Unicode catalogues
- [92be2da](https://github.com/ja3kies/Klyro-Shell/commit/92be2da98055d988bbdc89ceb85f61e405ff9487) — fix(diagnostics): protect retained sessions from deliberate stalls

Verified Actions builds of the exact delivered source `92be2da`:

- [Push build](https://github.com/ja3kies/Klyro-Shell/actions/runs/43653490511)
- [Independent repeated build](https://github.com/ja3kies/Klyro-Shell/actions/runs/43653944038)

- [Signed ARM64 app APK](https://github.com/ja3kies/Klyro-Shell/actions/runs/43653944038/artifacts/14137636740)
- [Matching signed AndroidTest APK](https://github.com/ja3kies/Klyro-Shell/actions/runs/43653944038/artifacts/14137900553)
- [JVM XML evidence](https://github.com/ja3kies/Klyro-Shell/actions/runs/43653944038/artifacts/14136814992)
- [Lint report](https://github.com/ja3kies/Klyro-Shell/actions/runs/43653944038/artifacts/14137746263)
- [ELF CI audit](https://github.com/ja3kies/Klyro-Shell/actions/runs/43653944038/artifacts/14137841056)

Artifacts retained 14 days by Actions; local copies: `.local/parity/run-43653944038`.

App APK SHA-256: `a5fcdbeb5aa9624c466d904c6949ebcb7dec7ecfe71dcd05b1a5a75df8613dcb`.

Test APK SHA-256: `da20af6b87b0026afe4a41d30442e345285c0ddcfdddf1260821e82240cfe5bb`.

Common certificate SHA-256: `01a7e05326237fdfa4cda54e75fb8164cbe12bbc964d36e2d9c2d1a8ef7e5c14`.

Version0.4.12-dev (19), ARM64, min26/target36/compile36. No Android permission or dependency addition. Upstream width data is an attributed same-API backport.
No bootstrap payload/PRoot version change, main merge, tag or release.

All raw logs, failed attempts, CI XML, immutable Unicode provenance and reports
are packaged in `.local/parity/termux-parity-evidence.zip`; raw originals remain
under `.local/parity`. APKs are separate. [Structured results](validation/termux-parity/test-results.json),
[artifact hashes](validation/termux-parity/artifacts.json),
[reproducibility](validation/termux-parity/reproducibility.json),
[lint comparison](validation/termux-parity/lint-comparison.json).

## Exact remaining gate

`adb devices -l` has no authorized device. The real runner returned exit2/BLOCKED
for the exact delivered source/APKs; see [device-blocker.json](validation/termux-parity/device-blocker.json).
The final source-linked acceptance runner returned exit 2/BLOCKED for SHA `92be2da`; the proof records the matching APK hashes. No on-device instrumentation, Klyro-UID package install, screenshot or manual interaction was performed. Supply a dedicated ARM64 device/connection for each API34–37 needed by the matrix, complete setup/install essential tools deliberately,
and run the source-linked gate. Do not update the APK while user jobs are active: Android
package updates/instrumentation can restart its process. This work did not install
an APK, stop a user job or alter their environment.

After that, run the existing physical daily-trial matrix: first install without
Termux; package update/install/upgrade/remove+recovery; editors/signals/paste;
1500-line stress→Settings/modal dismissal→Home/another app/return; multiple
sessions→lock/recents/rotation/low memory; SAF revoke/import/export; network/SSH
LAN/VPN/IPv6; TalkBack/large font/themes/system bars(issue13); prolonged metrics.
Keep expected/observed/PID/source/APK proof per case, never infer PASS from CI.

P0 dependencies remain open; the campaign is not declared complete. Nineteen
whole priorities are BLOCKED and KLY-14 is NOT STARTED. The latter is an absent
feature held behind the unvalidated runtime/data contract, not a completed or
simulated backup solution. Further P1/P2 product work was not substituted for
missing P0 operational evidence.
