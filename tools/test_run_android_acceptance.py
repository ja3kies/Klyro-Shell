"""Behavioral tests of the device gate, without pretending to execute Android."""
import argparse
import json
from pathlib import Path
import subprocess
import sys
import time
import tempfile
import unittest
from unittest.mock import patch
import run_android_acceptance as gate


def success(count=1):
    return ("INSTRUMENTATION_STATUS: class=com.example.Test\n"
            "INSTRUMENTATION_STATUS: test=works\n"
            "INSTRUMENTATION_STATUS_CODE: 1\n" +
            "INSTRUMENTATION_STATUS_CODE: 0\n" * count +
            f"INSTRUMENTATION_RESULT: stream=\nOK ({count} tests)\nINSTRUMENTATION_CODE: -1\n")


class InstrumentationResultTest(unittest.TestCase):
    def test_completed_events_and_summary_are_required(self):
        self.assertEqual(gate.instrumentation_result(success(3), 0)["status"], "VALIDATED")

    def test_skipped_or_assumed_cases_prevent_validation(self):
        for code in [-3, -4]:
            with self.subTest(code=code):
                self.assertEqual(gate.instrumentation_result(success() + f"INSTRUMENTATION_STATUS_CODE: {code}\n", 0)["status"], "BLOCKED")

    def test_assertion_failure_and_adb_nonzero_prevent_validation(self):
        for output, code in [(success()+"INSTRUMENTATION_STATUS_CODE: -2\n", 0), (success(), 1), (success()+"INSTRUMENTATION_FAILED: crash", 0)]:
            self.assertEqual(gate.instrumentation_result(output, code)["status"], "BLOCKED")

    def test_missing_summary_zero_tests_and_partial_delivery_are_blocked(self):
        for output in ["", success(0), success().replace("OK (1 tests)", ""), success().replace("INSTRUMENTATION_CODE: -1", ""), success().replace("OK (1 tests)", "OK (2 tests)")]:
            self.assertEqual(gate.instrumentation_result(output, 0)["status"], "BLOCKED")

    def test_unauthorized_offline_or_multiple_devices_are_rejected(self):
        for devices in ["List of devices attached\n", "a unauthorized", "a offline", "a device\nb device"]:
            with self.assertRaises(gate.Blocked):
                gate.select_device(devices)

    def test_explicit_authorized_serial_selects_one_device(self):
        self.assertEqual(gate.select_device("a device product:x\nb device", "b"), "b")

    def test_complete_package_metadata_is_required(self):
        self.assertEqual(gate.package_metadata("userId=10234 versionCode=19 minSdk=26\nversionName=0.4.12-dev"),
                         {"uid":10234,"version_code":19,"version_name":"0.4.12-dev"})
        with self.assertRaises(gate.Blocked): gate.package_metadata("versionCode=19")


class DevicePreflightTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        root = Path(self.temp.name)
        self.app = root / "app.apk"; self.app.write_bytes(b"app")
        self.tests = root / "tests.apk"; self.tests.write_bytes(b"tests")
        self.args = argparse.Namespace(output=str(root/"evidence"),source_commit="a"*40,
            app_apk=self.app,test_apk=self.tests,serial=None,adb="adb",dedicated_device=False)
        self.calls = []

    def fake_command(self, args, timeout=30):
        self.calls.append(args)
        if args[1:] == ["devices", "-l"]: return "List of devices attached\nserial device"
        tail=args[3:]
        responses={
            ("shell","getprop","ro.build.version.sdk"):"37",
            ("shell","getprop","ro.product.cpu.abilist"):"arm64-v8a",
            ("shell","getprop","ro.product.model"):"Fixture",
            ("shell","getprop","ro.kernel.qemu"):"0",
            ("shell","dumpsys","package",gate.APP):"userId=10234 versionCode=19\nversionName=0.4.12-dev",
            ("shell","pm","path",gate.APP):"package:/data/app/app/base.apk",
            ("shell","pm","path",gate.TEST):"package:/data/app/test/base.apk",
            ("shell","pm","list","instrumentation"):f"instrumentation:{gate.RUNNER} (target={gate.APP})",
        }
        if tail and tail[0]=="pull":
            Path(tail[2]).write_bytes(b"tests" if "/test/" in tail[1] else b"app")
            return ""
        return responses[tuple(tail)]

    def test_preflight_without_acknowledgment_never_starts_instrumentation(self):
        with patch.object(gate, "command", side_effect=self.fake_command), patch.object(gate,"capture_instrumentation") as run:
            result=gate.execute(self.args)
        run.assert_not_called()
        self.assertEqual(result["status"],"BLOCKED")
        self.assertIn("--dedicated-device",result["reason"])
        self.assertEqual(result["apk"]["app"]["installed_sha256"],gate.sha256(self.app))

    def test_installed_apk_mismatch_prevents_execution(self):
        self.app.write_bytes(b"different")
        with patch.object(gate, "command", side_effect=self.fake_command), patch.object(gate,"capture_instrumentation") as run:
            result=gate.execute(self.args)
        run.assert_not_called();self.assertIn("differs",result["reason"])

    def test_missing_device_produces_durable_blocked_evidence(self):
        with patch.object(gate,"command",return_value="List of devices attached\n"):
            result=gate.execute(self.args)
        self.assertEqual(result["status"],"BLOCKED")
        self.assertEqual(json.loads((Path(self.args.output)/"results.json").read_text())["status"],"BLOCKED")

    def test_successful_dedicated_run_uses_only_selected_classes_and_preserves_log(self):
        self.args.dedicated_device=True
        def instrument(args, log):
            self.calls.append(args);log.write_text(success(4));return 0
        with patch.object(gate,"command",side_effect=self.fake_command),patch.object(gate,"capture_instrumentation",side_effect=instrument):
            result=gate.execute(self.args)
        self.assertEqual(result["status"],"VALIDATED")
        invocation=self.calls[-1]
        self.assertIn(",".join(gate.CLASSES),invocation)
        self.assertIn("klyroRuntimeAcceptance",invocation)
        self.assertEqual((Path(self.args.output)/"instrumentation.txt").read_text(),success(4))
        for forbidden in ["install","force-stop","clear","grant","revoke"]:
            self.assertFalse(any(forbidden in call for call in self.calls))

    def test_timeout_keeps_partial_log_without_force_stop(self):
        self.args.dedicated_device=True
        def instrument(args, log):
            log.write_text("partial test log");raise gate.Blocked("Timeout; Android tests may still be running. No force-stop issued.")
        with patch.object(gate,"command",side_effect=self.fake_command),patch.object(gate,"capture_instrumentation",side_effect=instrument):
            result=gate.execute(self.args)
        self.assertEqual(result["status"],"BLOCKED")
        self.assertIn("may still be running",result["reason"])
        self.assertEqual((Path(self.args.output)/"instrumentation.txt").read_text(),"partial test log")

    def test_existing_output_is_never_overwritten(self):
        Path(self.args.output).mkdir()
        sentinel=Path(self.args.output)/"keep";sentinel.write_text("preserve")
        with self.assertRaises(FileExistsError):gate.execute(self.args)
        self.assertEqual(sentinel.read_text(),"preserve")


class BoundedCaptureTest(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup)
        self.log=Path(self.temp.name)/"instrumentation.txt"

    def test_real_stream_preserves_complete_success(self):
        result=gate.capture_instrumentation([sys.executable,"-c","print('complete')"],self.log,timeout=5)
        self.assertEqual(result,0)
        self.assertEqual(self.log.read_text(),"complete\n")

    def test_large_stream_never_writes_beyond_limit_and_stops_only_owned_client(self):
        sentinel=subprocess.Popen([sys.executable,"-c","import time; time.sleep(30)"])
        try:
            with self.assertRaisesRegex(gate.Blocked,"truncated at 4096 bytes"):
                gate.capture_instrumentation([sys.executable,"-c",
                    "import os; os.write(1,b'x'*1000000)"],self.log,timeout=5,limit=4096)
            self.assertEqual(self.log.stat().st_size,4096)
            self.assertEqual(self.log.read_bytes(),b"x"*4096)
            self.assertIsNone(sentinel.poll())
        finally:
            sentinel.kill();sentinel.wait(timeout=5)

    def test_silent_client_timeout_is_bounded_and_preserves_partial_output(self):
        start=time.monotonic()
        with self.assertRaisesRegex(gate.Blocked,"may still be running"):
            gate.capture_instrumentation([sys.executable,"-c",
                "import os,time; os.write(1,b'partial'); time.sleep(30)"],self.log,timeout=0.3)
        self.assertLess(time.monotonic()-start,5)
        self.assertEqual(self.log.read_bytes(),b"partial")


if __name__=="__main__": unittest.main()
