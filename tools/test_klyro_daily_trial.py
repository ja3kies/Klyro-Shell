"""Operational Pixel daily-trial contract: deterministic local test, no Android required.

The passing fixture proves that the script generates and verifies a live
counter, NOT that Android actually kept Klyro alive while backgrounded.
"""
import os
from pathlib import Path
import signal
import subprocess
import tempfile
import time
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / "scripts/klyro-daily-trial.sh"


class DailyTrialAcceptanceTest(unittest.TestCase):
    def test_shell_syntax(self):
        subprocess.run(["bash", "-n", str(SCRIPT)], check=True)

    def test_no_package_mutation_or_force_stop(self):
        body = SCRIPT.read_text()
        for forbidden in ("pkg install", "pkg upgrade", "apt install",
                          "apt upgrade", "am force-stop", "pm clear",
                          "adb uninstall", "rm -rf /data"):
            self.assertNotIn(forbidden, body)
        self.assertIn("NOT TESTED", body)
        self.assertIn("Interactive Bash parent PID unchanged", body)

    def test_run_verify_without_prepare_reports_failure(self):
        with tempfile.TemporaryDirectory() as d:
            env = {**os.environ, "HOME": d}
            p = subprocess.run(["bash", str(SCRIPT), "verify"],
                               env=env, capture_output=True, text=True, timeout=15)
            self.assertNotEqual(p.returncode, 0)
            self.assertIn("prior prepare state missing", p.stdout)

    def test_prepare_and_verify_live_heartbeat_and_keep_parent_identity(self):
        with tempfile.TemporaryDirectory() as d:
            env = {**os.environ, "HOME": d}
            process_pid = None
            try:
                prepared = subprocess.run(["bash", str(SCRIPT), "prepare"],
                                          env=env, capture_output=True,
                                          text=True, timeout=15)
                self.assertEqual(prepared.returncode, 0, prepared.stderr)
                state = Path(d) / "klyro-qa/daily-trial/state"
                self.assertTrue(state.is_file())
                vals = dict(line.split("=", 1) for line in state.read_text().splitlines())
                self.assertEqual(int(vals["shell_pid"]), os.getpid())
                process_pid = int(vals["job_pid"])
                # Confirm that the test's counter actually advances over time.
                time.sleep(2.5)
                verified = subprocess.run(["bash", str(SCRIPT), "verify"],
                                          env=env, capture_output=True,
                                          text=True, timeout=15)
                self.assertEqual(verified.returncode, 0, verified.stdout + verified.stderr)
                self.assertIn("PASS  Interactive Bash parent PID unchanged", verified.stdout)
                self.assertIn("PASS  background job produced", verified.stdout)
                self.assertIn("RESULT pass=", verified.stdout)
                log = Path(d) / "klyro-qa/daily-trial/background.log"
                values = [line.split() for line in log.read_text().splitlines()]
                self.assertGreaterEqual(len(values), 2)
                self.assertTrue(all(len(item) == 2 for item in values))
            finally:
                # Cleanup only this fixture's child PID, not any user's process.
                if process_pid is not None:
                    try:
                        os.kill(process_pid, signal.SIGTERM)
                    except ProcessLookupError:
                        pass


if __name__ == "__main__":
    unittest.main()
