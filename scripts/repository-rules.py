# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Apply or verify the reviewed Myna rulesets; preserve all unrelated rulesets."""
import argparse
import json
from pathlib import Path
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--apply", action="store_true", help="Write the checked-in configurations using an authorized gh login")
args = parser.parse_args()
endpoint = "repos/elide-dev/myna/rulesets"

def api(path, payload=None, method="GET"):
    command = ["gh", "api", path, "--method", method]
    if payload is not None:
        command += ["--input", "-"]
    result = subprocess.run(command, input=json.dumps(payload) if payload is not None else None,
                            capture_output=True, text=True, check=True)
    return json.loads(result.stdout)

def contains(actual, expected):
    if isinstance(expected, dict):
        return isinstance(actual, dict) and all(k in actual and contains(actual[k], v) for k, v in expected.items())
    if isinstance(expected, list):
        return isinstance(actual, list) and (actual == [] if not expected else all(
            any(contains(a, e) for a in actual) for e in expected))
    return actual == expected

existing = api(endpoint + "?includes_parents=false")
for path in sorted((Path(__file__).resolve().parents[1] / ".github/rulesets").glob("*.json")):
    wanted = json.loads(path.read_text())
    matches = [r for r in existing if r["name"] == wanted["name"]]
    if len(matches) > 1:
        raise RuntimeError("Ambiguous existing ruleset name: " + wanted["name"])
    if args.apply:
        result = api(endpoint + ("/" + str(matches[0]["id"]) if matches else ""), wanted,
                     "PUT" if matches else "POST")
    elif matches:
        result = api(endpoint + "/" + str(matches[0]["id"]))
    else:
        raise RuntimeError("Ruleset not installed: " + wanted["name"])
    if not contains(result, wanted):
        raise RuntimeError("Ruleset differs from reviewed configuration: " + wanted["name"])
    print(json.dumps({"name": result["name"], "id": result["id"], "enforcement": result["enforcement"]}))
