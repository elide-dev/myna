# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Rebuild distributables twice in one environment and retain byte/hash evidence.

This is a same-host repeatability check, not a cross-host reproducibility claim.
Run sequentially with other Elide tasks. Published release assets are never read
or modified; all outputs are new local builds under build/.
"""
import hashlib
import json
import os
from pathlib import Path
import platform
import shutil
import subprocess
import tomllib

ROOT = Path(__file__).resolve().parents[1]
work = ROOT / "build/repeatability"
work.mkdir(parents=True, exist_ok=True)
version = tomllib.loads((ROOT / "Cargo.toml").read_text())["workspace"]["package"]["version"]
env = dict(os.environ)
env.setdefault("SOURCE_DATE_EPOCH", subprocess.check_output(
    ["git", "show", "-s", "--format=%ct", "HEAD"], cwd=ROOT, text=True).strip())
files = ["myna", "myna-llvm", "myna.jar", "myna-feature.jar", "myna-values.jar", "myna-truffle.jar",
         f"myna-values-{version}.tar.gz"]
# Identify local inputs as well as HEAD, including new files and dirty edits.
inputs = subprocess.check_output(
    ["git", "ls-files", "-z", "--cached", "--others", "--exclude-standard"], cwd=ROOT).split(b"\0")
source_hashes = {os.fsdecode(name): hashlib.sha256((ROOT / os.fsdecode(name)).read_bytes()).hexdigest()
                 for name in sorted(set(inputs)) if name and (ROOT / os.fsdecode(name)).is_file()}
rounds = []
for number in (1, 2):
    destination = work / str(number)
    destination.mkdir(exist_ok=True)
    with (destination / "build.log").open("w") as log:
        for command in (["elide", "build", "--no-cache", ":jar", ":feature-jar", ":values-jar", ":truffle-jar"],
                        ["scripts/build.sh", "--llvm"], ["scripts/package-values.sh"]):
            subprocess.run(command, cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT, check=True)
    hashes = {}
    for name in files:
        source = ROOT / "build/dist" / name
        shutil.copyfile(source, destination / name)
        hashes[name] = hashlib.sha256(source.read_bytes()).hexdigest()
    rounds.append(hashes)
    print(f"Completed fresh build {number}", flush=True)
different = [name for name in files if rounds[0][name] != rounds[1][name]]
report = {"revision": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip(),
          "source_sha256": source_hashes,
          "platform": platform.platform(), "source_date_epoch": env["SOURCE_DATE_EPOCH"],
          "rounds": rounds, "different": different}
(work / "report.json").write_text(json.dumps(report, indent=2) + "\n")
if different:
    raise SystemExit("Non-repeatable artifacts: " + ", ".join(different))
print("All distributable bytes match; evidence: " + str(work / "report.json"))
