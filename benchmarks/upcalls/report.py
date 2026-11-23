# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Summarize a full SVM upcall run and verify the table published in the README."""
import argparse
import json
import re
import statistics
from pathlib import Path

from run import CASES, ROOT, WORK

BASELINE = ROOT / "benchmarks/upcalls/baselines/readme.json"
START = "<!-- upcall-results:start -->"
END = "<!-- upcall-results:end -->"
ROWS = [
    ("Native C control", "control"),
    ("Static FFM reducer", "static"),
    ("Generic dynamic FFM reducer", "dynamic"),
    ("Receiver-bound FFM reducer", "bound"),
    ("Explicit isolate-thread reducer", "isolate"),
    ("Static void hook", "static-hook"),
    ("Generic dynamic void hook", "dynamic-hook"),
    ("Receiver-bound void hook", "bound-hook"),
    ("Static Truffle callback, enter per call", "guest-each"),
    ("Receiver-bound Truffle callback, enter per call", "guest-bound-each"),
    ("Static Truffle callback, enter per batch", "guest-batch"),
    ("Receiver-bound Truffle callback, enter per batch", "guest-bound-batch"),
]


def collect(directory):
    samples = {}
    for case in CASES:
        samples[case] = []
        for fork in range(5):
            record = json.loads((directory / f"{case}-{fork}.json").read_text())
            if record["case"] != case:
                raise ValueError(f"unexpected case in {case}-{fork}")
            samples[case].append(record)
    return {"metadata": json.loads((directory / "metadata.json").read_text()), "samples": samples}


def table(data):
    medians = {}
    for case, records in data["samples"].items():
        if len(records) != 5 or any(len(r["samples_ns"]) != 12 or min(r["samples_ns"]) <= 0
                                   or r["iterations"] <= 0 for r in records):
            raise ValueError(f"incomplete five-process measurement for {case}")
        medians[case] = statistics.median(statistics.median(r["samples_ns"]) / r["iterations"]
                                          for r in records)
    lines = ["| Upcall path | Calling thread (ns/call) | Native worker (ns/call) |",
             "|---|---:|---:|"]
    displayed = set()
    for name, prefix in ROWS:
        caller = prefix + "-caller"
        worker = prefix + "-worker"
        displayed.add(caller)
        value = "—"
        if prefix != "isolate":
            displayed.add(worker)
            value = f"{medians[worker]:,.2f}"
        lines.append(f"| {name} | {medians[caller]:,.2f} | {value} |")
    displayed.update(("static-setup", "dynamic-setup", "bound-setup"))
    if displayed != set(CASES) or displayed != medians.keys():
        raise ValueError("README rows and measured cases differ")
    lines += ["", "Creating stubs and closing their arena cost "
              f"{medians['static-setup']:,.0f} ns per static registration, "
              f"{medians['dynamic-setup']:,.0f} ns per generic dynamic registration, and "
              f"{medians['bound-setup']:,.0f} ns per receiver-bound registration."]
    return "\n".join(lines)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["collect", "update-readme", "check-readme"])
    args = parser.parse_args()
    if args.action == "collect":
        data = collect(WORK / "results")
        summary = table(data)
        (WORK / "baseline.json").write_text(json.dumps(data, indent=2) + "\n")
        (WORK / "summary.md").write_text(summary + "\n")
    else:
        summary = table(json.loads(BASELINE.read_text()))
        readme = ROOT / "README.md"
        old = readme.read_text()
        new, matches = re.subn(f"{START}.*?{END}", f"{START}\n\n{summary}\n\n{END}", old, flags=re.S)
        if matches != 1:
            raise SystemExit("expected exactly one upcall results section")
        if args.action == "update-readme":
            readme.write_text(new)
        elif old != new:
            raise SystemExit("regenerate the README with benchmarks/upcalls/report.py update-readme")
