"""Compare compatible saved runs without rebuilding or remeasuring."""
import argparse
import json
import math
from pathlib import Path
import statistics

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("baseline", type=Path)
parser.add_argument("candidate", type=Path)
args = parser.parse_args()
baseline, candidate = [json.loads(p.read_text()) for p in (args.baseline, args.candidate)]

def profile(data):
    build = data["build"]
    return {"schema": build["schema"], "workload": build["workload"],
            "target": build["target"], "versions": build["versions"], "graalvm": build["graalvm"],
            "host": data["host"], "iterations": data["iterations"],
            "workloads": {k: v for k, v in build["sources"].items()
                          if k.startswith("benchmarks/comparison/") and Path(k).suffix in (".java", ".c", ".seam")}}

if baseline["smoke"] or candidate["smoke"]:
    raise RuntimeError("Smoke runs are not performance baselines")
if profile(baseline) != profile(candidate):
    raise RuntimeError("Mismatched workload, runtime, compiler, hardware or measurement profile")

def medians(data):
    groups = {}
    for case in data["results"]:
        values = [s["ns"] / s["iterations"] for s in case["samples"]]
        if not values or not all(math.isfinite(v) and v > 0 for v in values):
            raise RuntimeError("Invalid samples")
        groups.setdefault((case["mode"], case["case"]), []).append(statistics.median(values))
    if any(len(v) < 5 for v in groups.values()):
        raise RuntimeError("At least five process medians are required per case")
    return {k: statistics.median(v) for k, v in groups.items()}

before, after = medians(baseline), medians(candidate)
if before.keys() != after.keys():
    raise RuntimeError("Mismatched case sets")
print("| Mode | Case | Baseline ns/op | Candidate ns/op | Change |")
print("|---|---|---:|---:|---:|")
for key, old in sorted(before.items(), key=lambda item: ({"native": 0, "ffm-aot": 1, "jvm": 2}[item[0][0]], item[0][1])):
    new = after[key]
    print(f"| {key[0]} | {key[1]} | {old:.3f} | {new:.3f} | {(new/old-1)*100:+.2f}% |")
