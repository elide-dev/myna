# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Regression checks for the uninstrumented TruffleData benchmark runner."""
from contextlib import ExitStack, redirect_stderr
import hashlib
import io
import importlib.util
from pathlib import Path
import runpy
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("upcall_records", Path(__file__).parents[1] / "benchmarks/upcall-records/run.py")
runner = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runner)


class RecordRunnerTest(unittest.TestCase):
    def test_verified_cache_survives_failed_downloads_and_repairs_corruption(self):
        good = b"verified release bytes"
        checksum = hashlib.sha256(good).hexdigest()
        with tempfile.TemporaryDirectory() as tmp, patch.object(runner, "DEPS", Path(tmp)):
            destination = Path(tmp) / f"truffledata-json-{runner.VERSION}.jar"

            def interrupt(*args):
                args[-1].write_bytes(b"partial")
                raise OSError("interrupted")

            with patch.object(runner, "run", side_effect=interrupt), self.assertRaises(OSError):
                runner.artifact("json", ".jar", checksum)
            self.assertEqual(list(Path(tmp).iterdir()), [])

            destination.write_bytes(b"old incomplete file")
            with patch.object(runner, "run", side_effect=lambda *args: args[-1].write_bytes(b"wrong")), self.assertRaisesRegex(RuntimeError, "checksum"):
                runner.artifact("json", ".jar", checksum)
            self.assertEqual(destination.read_bytes(), b"old incomplete file")
            with patch.object(runner, "run", side_effect=lambda *args: args[-1].write_bytes(good)) as download:
                self.assertEqual(runner.artifact("json", ".jar", checksum), destination)
                self.assertEqual(runner.artifact("json", ".jar", checksum), destination)
                download.assert_called_once()
            self.assertEqual(destination.read_bytes(), good)

    def test_timeout_kills_and_reaps_the_child_before_leaving_popen(self):
        with patch.object(runner.subprocess, "Popen") as popen:
            process = popen.return_value.__enter__.return_value
            process.communicate.side_effect = [subprocess.TimeoutExpired("measure-records", 0.4), ("", "")]
            with self.assertRaises(subprocess.TimeoutExpired):
                runner.sample("bound", 35, 32, timeout=0.4)
            self.assertEqual([call[0] for call in process.method_calls], ["communicate", "kill", "communicate"])
            popen.return_value.__exit__.assert_called_once()

    def test_cpu_pinning_requires_platform_support(self):
        with ExitStack() as restore, patch.object(sys, "argv", ["run.py", "run", "--cpu", "1"]):
            if hasattr(runner.os, "sched_setaffinity"):
                restore.callback(setattr, runner.os, "sched_setaffinity", runner.os.sched_setaffinity)
                del runner.os.sched_setaffinity
            with redirect_stderr(io.StringIO()) as stderr, self.assertRaises(SystemExit) as stopped:
                runpy.run_path(spec.origin, run_name="__main__")
        self.assertEqual(stopped.exception.code, 2)
        self.assertIn("measurements on Linux", stderr.getvalue())

    def test_unverified_push_sample_is_rejected(self):
        with patch.object(runner.subprocess, "Popen") as popen:
            process = popen.return_value.__enter__.return_value
            process.returncode = 0
            output = "1\n" * 12
            report = "verified push rows=35 compiled=35 spans=2 last=3 hash=451"
            process.communicate.return_value = output, report
            with self.assertRaisesRegex(RuntimeError, "inlined spans"):
                runner.sample("push", 35, 32)
            process.communicate.return_value = output, report + " inlined_spans=2"
            self.assertEqual(len(runner.sample("push", 35, 32)["ns"]), 12)


if __name__ == "__main__":
    unittest.main()
