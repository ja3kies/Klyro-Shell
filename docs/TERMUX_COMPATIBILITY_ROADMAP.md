# Termux CLI compatibility — Klyro Shell

Official inventories:
- https://github.com/termux/termux-tools/blob/master/scripts/Makefile.am
- https://github.com/termux/termux-tools/blob/master/scripts/termux-setup-storage.in
- https://github.com/termux/termux-api

Updated: 2026-11-07. The objective is genuine, tested equivalents, not fake
scripts which print success while doing nothing.

## Status vocabulary

- IMPLEMENTED / DEVICE PENDING: source and tests exist, Android UI not yet verified.
- CONDITIONAL: upstream command may exist in the private package prefix;
  runtime execution is not guaranteed until tested.
- PLANNED: not implemented and not presented as functional.
- RESTRICTED: Android permissions, app signature, or platform limitations prevent
  a drop-in command without an explicit product/security decision.

## Core Termux commands

| Upstream command | Klyro equivalent / constraint | Status |
|---|---|---|
| termux-setup-storage | Generated Bash function redirects to klyro-setup-storage, opens native Android SAF picker, returns status via private request bridge. No Unix mount or shared symlinks. | IMPLEMENTED / DEVICE PENDING |
| termux-info | Generated Bash function redirects to klyro-info, read-only private Bash/package environment report. | IMPLEMENTED / DEVICE PENDING |
| termux-change-repo | Private termux-tools script if installed. Check actual mirror handling before claiming support. | CONDITIONAL |
| pkg, apt | Present in Klyro bootstrap; search, install, upgrade through private pkg runtime. | CONDITIONAL |
| termux-setup-package-manager | Upstream script if installed, test configuration handling. | CONDITIONAL |
| termux-fix-shebang | Upstream script if installed; ensure it only modifies authorized files. | CONDITIONAL |
| termux-open, xdg-open | Android ACTION_VIEW and Klyro FileProvider; scoped URIs and MIME types. | PLANNED |
| termux-open-url | Android ACTION_VIEW for reviewed HTTPS URLs. | PLANNED |
| termux-share | ACTION_SEND with explicit chooser and file/content URI ownership. | PLANNED |
| termux-storage-get | Android OpenDocument to private home, atomic bounded import. Existing UI can import; CLI bridge not wired. | PLANNED |
| termux-backup | Explicit SAF export with archive integrity and optional encryption. | PLANNED |
| termux-restore | User-confirmed restore with rollback and no silent overwrite. | PLANNED |
| termux-reload-settings | Settings already applied immediately; optional status/refresh command. | PLANNED |
| termux-wake-lock and termux-wake-unlock | Bounded PowerManager lock, visible notification and stop action. | RESTRICTED |
| termux-reset | Only through reviewed destructive flow, backup and confirmation. | RESTRICTED |
| chsh, login, su | Never fake Unix accounts or elevated privileges. | RESTRICTED |

## Termux:API command families

Termux:API normally uses a companion Android application and compatible
signature/permissions. Klyro cannot assume that a Termux-signed add-on can call
Klyro or vice versa. Implement independent Klyro Android adapters with explicit
user control instead.

| Commands | Native integration required | Status |
|---|---|---|
| termux-clipboard-get/set | ClipboardManager, foreground clipboard/privacy rules | PLANNED |
| termux-toast, termux-dialog | Android UI, rate limit and no silent prompts | PLANNED |
| termux-notification, remove, list | Notification channels and user permission | RESTRICTED |
| termux-vibrate | VibratorManager, bounded pulse, opt-in | RESTRICTED |
| termux-battery-status | BatteryManager and read-only diagnostics | PLANNED |
| termux-brightness | System settings/write permission or app window brightness | RESTRICTED |
| termux-wifi-connectioninfo/scaninfo | WifiManager and Android permission requirements | RESTRICTED |
| termux-location | LocationManager, permission and active indicator | RESTRICTED |
| termux-camera-photo/info | Native camera, camera permission, save through SAF | RESTRICTED |
| termux-microphone-record | Microphone permission and visible recording control | RESTRICTED |
| termux-media-player | MediaPlayer and scoped media URIs | PLANNED |
| termux-contact-list/get | ContactsContract with sensitive permission | RESTRICTED |
| termux-sms-list/send | SMS platform restrictions and role requirements | RESTRICTED |
| termux-call-log and termux-telephony-* | Call log/telephony role and privacy requirements | RESTRICTED |
| termux-fingerprint | BiometricPrompt per operation | RESTRICTED |
| termux-sensor | SensorManager with correct lifecycle and battery limits | PLANNED |
| termux-tts-* and termux-speech-to-text | Android TTS / recognition with microphone consent | PLANNED |
| termux-wallpaper-* | WallpaperManager, explicit selection | PLANNED |
| termux-usb, volume, job-scheduler | USB/audio/JobScheduler APIs and controls | PLANNED |

Not exhaustive for all Termux package versions. Each future iteration must
compare with upstream scripts and test real behavior; no placeholders may
claim success.

## Invariants

1. No automatic Enter from Command Palette.
2. No global Android storage permission requested.
3. SAF is not a Linux mount or a replacement for ~/storage/shared symlinks.
4. Private prefixed klyro-* scripts only; preserve upstream and custom scripts.
5. Sensitive operations are opt-in, rejectable and report actual failure.
6. Never touch /data/data/com.termux or erase existing user data.
7. Privileged or destructive commands require explicit security review.

## Device checks after compilation

- type termux-setup-storage and type termux-info in a new Klyro Bash session.
- termux-info reports the Klyro runtime rather than the separate Termux app.
- termux-setup-storage opens SAF folder picker when Klyro is foreground.
- Cancel returns a nonzero status, no new permission.
- Authorize returns success and shows the folder as connected in Settings.
- No direct POSIX mount is advertised; import/export remain scoped.
- Two simultaneous requests do not open two pickers.
- Rotation and process recreation do not silently grant access.
