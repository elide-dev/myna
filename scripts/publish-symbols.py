# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Publish matching sidecars through Myna's managed Elide Cloud symbol connection."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import re
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request

class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, file, code, message, headers, new_url):
        return None

HTTP = urllib.request.build_opener(NoRedirect)

def request(origin, path, token=None, body=None, method="POST", raw=False):
    headers = {"User-Agent": "myna-symbol-publisher/1", "Content-Type": "application/octet-stream" if raw else "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    data = body if raw else (json.dumps(body).encode() if body is not None else None)
    # Retry only operations whose identity/content is stable across requests.
    replayable = method in ("GET", "PUT") or path.startswith(("/v1/uploads", "/v1/symbols/"))
    for attempt in range(3):
        try:
            with HTTP.open(urllib.request.Request(origin + path, data=data, headers=headers, method=method), timeout=150) as response:
                content = response.read(1024 * 1024)
                return json.loads(content) if content else {}
        except urllib.error.HTTPError as error:
            code = error.code
            error.close()
            if not replayable or code not in (408, 429, 502, 503, 504) or attempt == 2:
                # Never print request headers, credentials or provider response bodies.
                raise RuntimeError(f"Symbol service {path}: HTTP {code}") from None
        except (urllib.error.URLError, TimeoutError):
            if not replayable or attempt == 2:
                raise RuntimeError(f"Symbol service {path}: network request failed") from None
        time.sleep(2 ** attempt)

def access_token():
    token = os.environ.get("ELIDE_CLOUD_ACCESS_TOKEN")
    if token:
        return token
    key = os.environ["ELIDE_CLOUD_API_KEY"]
    data = urllib.parse.urlencode({"grant_type": "api_key", "api_key": key}).encode()
    req = urllib.request.Request("https://auth.elide.cloud/token", data=data,
                                 headers={"Content-Type": "application/x-www-form-urlencoded", "User-Agent": "myna-symbol-publisher/1"})
    with HTTP.open(req, timeout=30) as response:
        return json.load(response)["access_token"]

def publish(paths, revision=None, target=None, receipt_file=Path("build/reports/symbol-publication.json")):
    revision = revision or subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
    target = target or platform.system().lower() + "-" + platform.machine().lower()
    if not re.fullmatch(r"[0-9a-f]{40}", revision) or target not in (
            "linux-x86_64", "linux-aarch64", "darwin-arm64"):
        raise ValueError("Expected a full source commit and a supported symbol target")
    origin = "https://build.elide.cloud"
    lease = request(origin, "/v1/leases", access_token(), {"project": "myna", "sharing": "org", "writable": True})
    token = lease["token"]
    connection = request(origin, "/v1/connections/debuginfo", token, method="GET")
    if connection.get("state") != "active":
        raise RuntimeError("Myna's managed debuginfo connection is not connected")
    build = revision + "-" + target
    receipts = []
    for path in paths:
        if not path.is_file() or path.suffix not in (".debug", ".dsym"):
            raise ValueError(f"Not a symbol sidecar: {path}")
        digest = hashlib.sha256()
        with path.open("rb") as source:
            for chunk in iter(lambda: source.read(1024 * 1024), b""):
                digest.update(chunk)
        sha = digest.hexdigest()
        remote = f"artifacts/{build}/symbols/{path.name}"
        upload_id = hashlib.sha256((remote + sha).encode()).hexdigest()[:32]
        session = request(origin, "/v1/uploads", token, {
            "id": upload_id, "path": remote, "size": path.stat().st_size, "digest": sha,
            "metadata": {"attributes": {"repository": "elide-dev/myna", "commit": revision, "target": target}},
        })
        if session["state"] != "complete":
            part_bytes = session["partBytes"]
            if not isinstance(part_bytes, int) or not 0 < part_bytes <= 64 * 1024 * 1024:
                raise RuntimeError("Invalid upload part size")
            status = request(origin, f"/v1/uploads/{upload_id}", token, method="GET")
            existing = {part["number"]: part for part in status["parts"]}
            with path.open("rb") as source:
                for number, chunk in enumerate(iter(lambda: source.read(part_bytes), b""), 1):
                    previous = existing.get(number)
                    if previous and previous["digest"] == hashlib.sha256(chunk).hexdigest() and previous["size"] == len(chunk):
                        continue
                    request(origin, f"/v1/uploads/{upload_id}/parts/{number}", token, chunk, "PUT", raw=True)
            request(origin, f"/v1/uploads/{upload_id}/complete", token)
        receipt = request(origin, f"/v1/symbols/{build}/{path.name}", token, {})
        receipts.append({"file": path.name, "sha256": sha, "revision": revision, "target": target, "receipt": receipt})
        receipt_file.parent.mkdir(parents=True, exist_ok=True)
        receipt_file.write_text(json.dumps(receipts, indent=2) + "\n")
        print(json.dumps(receipts[-1]), flush=True)
    return receipts

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("paths", nargs="*", type=Path)
    parser.add_argument("--revision", help="Verified source commit of retained CI artifacts")
    parser.add_argument("--target", help="Verified target of retained CI artifacts")
    parser.add_argument("--receipt-file", type=Path, default=Path("build/reports/symbol-publication.json"))
    args = parser.parse_args()
    paths = args.paths or sorted(Path("build/dist/symbols").glob("*"))
    if not paths:
        raise SystemExit("No symbol sidecars found")
    publish(paths, args.revision, args.target, args.receipt_file)
