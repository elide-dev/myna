# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Write CodSpeed's walltime schema using native measured regions, not process time.

Schema matches the pinned JMH fork's io.codspeed.result.WalltimeResults.
Raw samples are retained separately for inspection and future reanalysis.
"""
import json
import os
from pathlib import Path
import statistics
import subprocess


def report(raw):
    samples = raw["samples_ns"]
    values = [ns // raw["iterations"] for ns in samples]
    q1, median, q3 = statistics.quantiles(values, n=4, method="exclusive")
    mean, stdev = statistics.mean(values), statistics.stdev(values)
    stats = dict(min_ns=min(values), max_ns=max(values), mean_ns=mean,
                 stdev_ns=stdev, q1_ns=q1, median_ns=median, q3_ns=q3,
                 rounds=len(samples), total_time=sum(samples) / 1e9,
                 iqr_outlier_rounds=sum(v < q1 - 1.5 * (q3 - q1) or
                                       v > q3 + 1.5 * (q3 - q1) for v in values),
                 stdev_outlier_rounds=sum(abs(v - mean) > 3 * stdev for v in values),
                 iter_per_round=raw["iterations"], warmup_iters=raw["warmup_iters"])
    return {"creator": {"name": "myna-native", "version": "1.0.0", "pid": raw["pid"]},
            "instrument": {"type": "walltime"},
            "benchmarks": [{"name": raw["case"],
                            "uri": "benchmarks/native/values.cpp::" + raw["case"],
                            "config": {"max_rounds": len(samples)}, "stats": stats}]}


if __name__ == "__main__":
    output = Path(os.environ["CODSPEED_PROFILE_FOLDER"]) / "results"
    output.mkdir(parents=True, exist_ok=True)
    retained = Path("build/bench-native/results")
    retained.mkdir(parents=True, exist_ok=True)
    for name in ("inline", "owned", "borrowed"):
        raw = json.loads(subprocess.check_output(
            ["build/bench-native/values", name, "--require-instrumentation"],
            env={**os.environ, "MYNA_WALLTIME": "1"}, text=True))
        if raw["case"] != name or len(raw["samples_ns"]) != 30 or min(raw["samples_ns"]) <= 0:
            raise RuntimeError("Invalid native measurement output")
        payload = json.dumps(report(raw), indent=2) + "\n"
        (output / f"{raw['pid']}.json").write_text(payload)
        (retained / f"{name}-codspeed.json").write_text(payload)
        (retained / f"{name}-samples.json").write_text(json.dumps(raw, indent=2) + "\n")
        print(payload)
