# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Regression checks for transient publication failures and durable receipts."""
import importlib.util
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import urllib.error

spec = importlib.util.spec_from_file_location("publisher", Path(__file__).resolve().parents[1] / "scripts/publish-symbols.py")
publisher = importlib.util.module_from_spec(spec)
spec.loader.exec_module(publisher)


class PublicationTest(unittest.TestCase):
    def failure(self, code):
        return urllib.error.HTTPError("https://build.elide.cloud/test", code, "unavailable", {}, io.BytesIO())

    def test_transient_failure_replays_same_request(self):
        with patch.object(publisher.HTTP, "open", side_effect=[self.failure(503), io.BytesIO(b'{"status":"exists"}')]) as request, patch.object(publisher.time, "sleep"):
            self.assertEqual(publisher.request("https://build.elide.cloud", "/v1/symbols/build/app.debug", "secret", {}), {"status": "exists"})
            self.assertEqual(request.call_count, 2)
            self.assertEqual(request.call_args_list[0].args[0].data, request.call_args_list[1].args[0].data)

    def test_authorization_and_lease_failures_are_not_retried(self):
        for path, code in (("/v1/symbols/build/app.debug", 403), ("/v1/leases", 503)):
            with self.subTest(path=path), patch.object(publisher.HTTP, "open", side_effect=self.failure(code)) as request:
                with self.assertRaisesRegex(RuntimeError, f"HTTP {code}"):
                    publisher.request("https://build.elide.cloud", path, "secret", {})
                self.assertEqual(request.call_count, 1)

    def test_retries_are_bounded(self):
        with patch.object(publisher.HTTP, "open", side_effect=urllib.error.URLError("private provider detail")) as request, patch.object(publisher.time, "sleep"):
            with self.assertRaisesRegex(RuntimeError, "network request failed") as result:
                publisher.request("https://build.elide.cloud", "/v1/uploads/id", "secret", method="GET")
            self.assertEqual(request.call_count, 3)
            self.assertNotIn("secret", str(result.exception))
            self.assertNotIn("private provider detail", str(result.exception))

    def test_successful_receipt_survives_later_failure(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            paths = [root / "one.debug", root / "two.debug"]
            for path in paths:
                path.write_bytes(b"fixture")
            receipt_file = root / "receipts.json"

            def request(origin, path, *args, **kwargs):
                if path == "/v1/leases": return {"token": "lease-secret"}
                if path == "/v1/connections/debuginfo": return {"state": "active"}
                if path == "/v1/uploads": return {"state": "complete"}
                if path.endswith("one.debug"): return {"status": "created", "debugId": "abc12345"}
                raise RuntimeError("second upload unavailable")

            with patch.object(publisher, "access_token", return_value="secret"), patch.object(publisher, "request", side_effect=request), patch("builtins.print"):
                with self.assertRaisesRegex(RuntimeError, "second upload unavailable"):
                    publisher.publish(paths, "a" * 40, "linux-x86_64", receipt_file)
            receipts = json.loads(receipt_file.read_text())
            self.assertEqual([r["file"] for r in receipts], ["one.debug"])
            self.assertNotIn("secret", receipt_file.read_text())


if __name__ == "__main__":
    unittest.main()
