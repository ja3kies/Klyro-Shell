#!/usr/bin/env python3
"""Run bounded, non-destructive acceptance on a dedicated Android test device.

Install matching app/test APKs yourself and finish all user jobs first. Instrumentation
can restart Klyro; this tool never installs, clears data, force-stops, or grants access.
Reference: https://developer.android.com/training/testing/instrumented-tests/androidx-test-libraries/runner
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import selectors
import subprocess
import time

APP = "com.klyro.shell"
TEST = APP + ".test"
RUNNER = TEST + "/androidx.test.runner.AndroidJUnitRunner"
CLASSES = (
    "com.oryn.terminal.ui.ModalLifecycleUiTest",
    "com.termux.terminal.NativePtyTest",
    "com.oryn.terminal.session.OrynTerminalResumeTest",
    "com.oryn.terminal.session.InstalledRuntimeAcceptanceTest",
    "com.oryn.terminal.session.FinishedSessionRetentionTest",
    "com.oryn.terminal.ui.FinishedSessionUiTest",
    "com.oryn.terminal.ui.FinishedTerminalBackUiTest",
)


class Blocked(RuntimeError):
    pass


def sha256(path):
    digest = hashlib.sha256()
    with Path(path).open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def select_device(output, serial=None):
    devices = [line.split()[:2] for line in output.splitlines()
               if line.strip() and not line.startswith(("List of devices", "*"))]
    if serial:
        devices = [entry for entry in devices if entry[0] == serial]
    if len(devices) != 1 or len(devices[0]) != 2 or devices[0][1] != "device":
        raise Blocked("Exactly one authorized ADB device is required (or select --serial).")
    return devices[0][0]


def instrumentation_result(output, returncode):
    """Require completed test events, final Android result, and matching JUnit count."""
    codes = [int(value) for value in re.findall(r"^INSTRUMENTATION_STATUS_CODE: (-?\d+)\s*$", output, re.M)]
    summaries = re.findall(r"^OK \((\d+) tests?\)\s*$", output, re.M)
    failures = re.search(r"INSTRUMENTATION_FAILED|FAILURES!!!|Process crashed|shortMsg=", output)
    count = int(summaries[-1]) if summaries else 0
    if (returncode != 0 or failures or any(code < 0 for code in codes) or count == 0
            or codes.count(0) != count or not re.search(r"^INSTRUMENTATION_CODE: -1\s*$", output, re.M)):
        return {"status": "BLOCKED", "tests_completed": codes.count(0),
                "reason": "Failed, skipped, incomplete or zero-test instrumentation; inspect raw log."}
    return {"status": "VALIDATED", "tests_completed": count,
            "scope": "Selected instrumented cases on this device/APK only; not cross-version parity."}


def command(args, timeout=30):
    result = subprocess.run(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                            text=True, timeout=timeout, check=False)
    if result.returncode:
        raise Blocked("ADB command failed: " + " ".join(args[:5]) + ": " + result.stderr.strip()[:300])
    return result.stdout.strip()


def capture_instrumentation(invoke, log, timeout=600, limit=4 * 1024 * 1024):
    """Bound bytes while receiving; terminate only our host adb client on failure."""
    process = subprocess.Popen(invoke, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, bufsize=0)
    deadline = time.monotonic() + timeout
    written = 0
    stopped = "Android tests may still be running. No force-stop issued."
    try:
        with log.open("wb") as stream, selectors.DefaultSelector() as ready:
            ready.register(process.stdout, selectors.EVENT_READ)
            while True:
                remaining = deadline - time.monotonic()
                if remaining <= 0 or not ready.select(max(0, remaining)):
                    raise Blocked(f"Instrumentation exceeded {timeout}s; raw log retained. " + stopped)
                chunk = os.read(process.stdout.fileno(), 65536)
                if not chunk:
                    break
                available = limit - written
                stream.write(chunk[:available])
                written += min(len(chunk), available)
                if len(chunk) > available:
                    raise Blocked(f"Instrumentation log truncated at {limit} bytes; output limit exceeded. " + stopped)
        try:
            return process.wait(timeout=max(0.01, deadline - time.monotonic()))
        except subprocess.TimeoutExpired:
            raise Blocked(f"Instrumentation exceeded {timeout}s; raw log retained. " + stopped)
    finally:
        process.stdout.close()
        if process.poll() is None:
            process.kill()
        process.wait(timeout=5)


def package_metadata(output):
    def required(pattern):
        match = re.search(pattern, output)
        if not match:
            raise Blocked("Installed package metadata is incomplete.")
        return match.group(1)
    return {"uid": int(required(r"\buserId=(\d+)")),
            "version_code": int(required(r"\bversionCode=(\d+)")),
            "version_name": required(r"\bversionName=([^\s]+)")}


def execute(args):
    out = Path(args.output)
    out.mkdir(parents=True, exist_ok=False)
    report = {"status": "BLOCKED", "source_commit": args.source_commit,
              "started_utc": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
              "limits": ["No Android installation, package mutation or permission changes.",
                         "One device does not validate Android 14–17 or prolonged physical use.",
                         "source_commit is caller-supplied linkage to CI metadata, not decoded from the APK; installed hashes verify the supplied artifacts."]}
    try:
        if not re.fullmatch(r"[0-9a-fA-F]{40}", args.source_commit):
            raise Blocked("--source-commit must be the full source SHA linked to the supplied APK artifacts.")
        listing = command([args.adb, "devices", "-l"])
        (out / "adb-devices.txt").write_text(listing + "\n")
        serial = select_device(listing, args.serial)
        adb = [args.adb, "-s", serial]
        def shell(*values):
            return command(adb + ["shell", *values])
        api = int(shell("getprop", "ro.build.version.sdk"))
        abi = shell("getprop", "ro.product.cpu.abilist")
        report["device"] = {"serial": serial, "api": api, "abis": abi.split(","),
                            "model": shell("getprop", "ro.product.model"),
                            "emulator": shell("getprop", "ro.kernel.qemu") == "1"}
        if api not in range(34, 38) or "arm64-v8a" not in abi.split(","):
            raise Blocked("Acceptance requires Android API 34–37 and ARM64.")
        report["app"] = package_metadata(shell("dumpsys", "package", APP))
        if report["app"]["uid"] < 10000:
            raise Blocked("Target package does not have an application UID.")
        report["apk"] = {}
        for package, supplied, label in [(APP, args.app_apk, "app"), (TEST, args.test_apk, "tests")]:
            paths = shell("pm", "path", package).splitlines()
            if len(paths) != 1 or not re.fullmatch(r"package:/data/app/[^\s]+\.apk", paths[0]):
                raise Blocked("Expected one installed base APK for " + package + "; split installs unsupported.")
            destination = out / (label + "-installed.apk")
            command(adb + ["pull", paths[0][8:], str(destination)], timeout=120)
            supplied_hash, installed_hash = sha256(supplied), sha256(destination)
            report["apk"][label] = {"supplied_sha256": supplied_hash, "installed_sha256": installed_hash}
            if supplied_hash != installed_hash:
                raise Blocked("Installed " + label + " APK differs from the supplied source-linked artifact.")
        runners = shell("pm", "list", "instrumentation")
        if not re.search(r"^instrumentation:" + re.escape(RUNNER) + r" \(target=" + re.escape(APP) + r"\)$", runners, re.M):
            raise Blocked("Matching AndroidJUnitRunner targeting Klyro is not installed.")
        if not args.dedicated_device:
            raise Blocked("Preflight complete. --dedicated-device is required: instrumentation can restart Klyro; finish user jobs first.")
        log = out / "instrumentation.txt"
        invoke = adb + ["shell", "am", "instrument", "-w", "-r", "-e", "class", ",".join(CLASSES),
                        "-e", "klyroRuntimeAcceptance", "true", RUNNER]
        report["instrumentation_command"] = invoke
        returncode = capture_instrumentation(invoke, log)
        report["instrumentation"] = instrumentation_result(log.read_text(errors="replace"), returncode)
        report["status"] = report["instrumentation"]["status"]
    except (Blocked, OSError, ValueError, subprocess.TimeoutExpired) as error:
        report["reason"] = str(error)
    (out / "results.json").write_text(json.dumps(report, indent=2) + "\n")
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", required=True, help="New evidence directory; never overwrites a prior run")
    parser.add_argument("--source-commit", required=True)
    parser.add_argument("--app-apk", required=True, type=Path)
    parser.add_argument("--test-apk", required=True, type=Path)
    parser.add_argument("--serial")
    parser.add_argument("--adb", default="adb")
    parser.add_argument("--dedicated-device", action="store_true",
                        help="Acknowledge instrumentation may restart Klyro; use only a device without user jobs")
    args = parser.parse_args()
    try:
        report = execute(args)
    except FileExistsError:
        parser.error("Output directory already exists; choose a new one.")
    print(report["status"] + ": " + report.get("reason", report.get("instrumentation", {}).get("scope", "Inspect results.json")))
    return 0 if report["status"] == "VALIDATED" else 2


if __name__ == "__main__":
    raise SystemExit(main())
