# Evidence scope

ledger.json records whole-priority status only, using the requested vocabulary.
Final artifact metadata binds source commit, CI run, APK SHA-256 and certificates.
Logs include failed attempts and the successful reruns; skipped physical/device
work is not converted to success. The standalone readonly rootfs recheck uses a
historical QA fixture under host Termux UID, not the Klyro APK sandbox.

Command dumps committed here normalize trailing whitespace only. Full original
logs and CI XML/artifacts are retained in `.local/parity` and the delivery ZIP.
No user terminal transcript, clipboard, credentials or documents are collected.
