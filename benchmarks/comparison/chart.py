"""Render measured SVM scalar latency with process variability and provenance."""
import argparse
import hashlib
import json
from pathlib import Path
import statistics

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("results", type=Path)
parser.add_argument("--output", type=Path, default=Path("docs/charts/svm-boundaries"))
args = parser.parse_args()
data = json.loads(args.results.read_text())
if data["smoke"] or data["forks"] < 5 or not data["host"]["cpu"].strip():
    raise SystemExit("A full measured baseline with CPU provenance is required")
groups = {}
for result in data["results"]:
    groups.setdefault((result["mode"], result["case"]), []).append(
        statistics.median(s["ns"] / s["iterations"] for s in result["samples"]))

plt.rcParams.update({"font.family": "DejaVu Sans", "font.size": 10,
                     "svg.hashsalt": "myna-svm-boundaries", "svg.fonttype": "none"})
fig, axes = plt.subplots(2, 2, figsize=(11, 6.5))
panels = [
    ("Direct C: normal transition", "native", ["generated-normal", "manual-normal"], False),
    ("Direct C: native leaf", "native", ["generated-leaf", "manual-leaf"], False),
    ("AOT FFM: build-time adapters", "ffm-aot", ["generated-ffm", "manual-ffm"], False),
    ("AOT FFM: adapter strategy", "ffm-aot", ["generated-ffm", "manual-bound-ffm"], True),
]
labels = {"generated-normal": "Generated", "manual-normal": "Handwritten",
          "generated-leaf": "Generated", "manual-leaf": "Handwritten",
          "generated-ffm": "Generated", "manual-ffm": "Handwritten",
          "manual-bound-ffm": "Runtime-bound"}
for ax, (title, mode, cases, logarithmic) in zip(axes.flat, panels):
    for y, case in enumerate(cases):
        values = groups[(mode, case)]
        median = statistics.median(values)
        color = "#176b91" if case.startswith("generated") else "#a75d23"
        ax.errorbar(median, y, xerr=[[median - min(values)], [max(values) - median]],
                    fmt="D", color=color, capsize=5, markersize=6)
        offsets = [(i - (len(values) - 1) / 2) * .045 for i in range(len(values))]
        ax.scatter(values, [y + offset for offset in offsets], color=color, s=14, alpha=.6)
        ax.annotate(f"{median:,.2f} ns", (median, y), xytext=(0, 17),
                    textcoords="offset points", ha="center", fontsize=10)
    ax.set_yticks(range(len(cases)), [labels[c] for c in cases])
    ax.set_ylim(-.55, 1.65)
    ax.invert_yaxis()
    ax.set_title(title, loc="left", fontweight="bold", fontsize=11)
    ax.set_xlabel("Nanoseconds / call" + (" (log scale)" if logarithmic else ""))
    if logarithmic:
        ax.set_xscale("log")
        ax.set_xlim(5, 20000)
    else:
        ax.set_xlim(0, max(max(groups[(mode, c)]) for c in cases) * 1.25)
    ax.grid(axis="x", alpha=.2)
    ax.spines[["top", "right"]].set_visible(False)
revision = data["build"].get("verified_source_commit", data["build"]["revision"])
fig.suptitle("Myna SVM scalar call latency", x=.04, ha="left", fontsize=17, fontweight="bold")
fig.text(.04, .045,
         f"Source {revision[:12]} · {data['host']['machine']} · {data['forks']} fresh processes per case\n"
         "Diamonds: median of process medians. Dots/ranges: individual processes/min–max. "
         "Shared-host results are advisory; lower is better.", fontsize=9)
fig.tight_layout(rect=(.02, .12, .99, .95), h_pad=2.5, w_pad=3)
args.output.parent.mkdir(parents=True, exist_ok=True)
fig.savefig(args.output.with_suffix(".svg"), metadata={"Date": None})
fig.savefig(args.output.with_suffix(".png"), dpi=160, metadata={"Software": "Myna chart.py"})
plt.close(fig)
svg = args.output.with_suffix(".svg")
svg.write_text("\n".join(line.rstrip() for line in svg.read_text().splitlines()) + "\n")
inputs = [args.results, Path(__file__), Path(__file__).with_name("requirements.txt")]
files = inputs + [args.output.with_suffix(".svg"), args.output.with_suffix(".png")]
root = Path(__file__).resolve().parents[2]
manifest = {str(p.resolve().relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest() for p in files}
args.output.with_suffix(".manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
