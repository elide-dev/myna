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

plt.rcParams.update({"font.family": "DejaVu Sans", "font.size": 11,
                     "svg.hashsalt": "myna-svm-boundaries", "svg.fonttype": "none"})
fig, ax = plt.subplots(figsize=(11, 6), facecolor="#ffffff")
fig.subplots_adjust(left=.27, right=.94, top=.72, bottom=.25)
colors = ["#087f8c", "#94a3b8"]
panels = [
    ("Direct C", "Normal VM transition", "native", ["generated-normal", "manual-normal"]),
    ("Native leaf", "No VM transition¹", "native", ["generated-leaf", "manual-leaf"]),
    ("AOT FFM", "Build-time adapters", "ffm-aot", ["generated-ffm", "manual-ffm"]),
]
for group, (title, subtitle, mode, cases) in enumerate(panels):
    center = 6 - group * 2.5
    ax.text(-.035, center + .13, title, transform=ax.get_yaxis_transform(),
            ha="right", va="center", fontsize=13, fontweight="bold", color="#172b3a")
    ax.text(-.035, center - .40, subtitle, transform=ax.get_yaxis_transform(),
            ha="right", va="center", fontsize=9, color="#526578")
    for index, case in enumerate(cases):
        values = groups[(mode, case)]
        median = statistics.median(values)
        y = center + (.32 if index == 0 else -.32)
        ax.barh(y, median, height=.44, color=colors[index], zorder=3)
        ax.errorbar(median, y, xerr=[[median-min(values)], [max(values)-median]],
                    fmt="none", ecolor="#243746", capsize=3, linewidth=1, zorder=4)
        ax.text(max(values)+.35, y, f"{median:.2f}", va="center", fontsize=12,
                fontweight="bold" if index == 0 else "normal", color="#172b3a")
ax.set_xlim(0, 20)
ax.set_ylim(-.05, 7.15)
ax.set_xticks([0, 5, 10, 15, 20])
ax.set_yticks([])
ax.tick_params(axis="x", length=0, colors="#526578", labelsize=10, pad=8)
ax.set_xlabel("Nanoseconds per call · lower is better", color="#526578", fontsize=10, labelpad=10)
ax.grid(axis="x", color="#e7edf1", linewidth=.8, zorder=0)
ax.spines[:].set_visible(False)
fig.text(.055, .91, "Native Image call overhead", fontsize=23, fontweight="bold", color="#172b3a")
fig.text(.055, .855, "Myna generated bindings vs. handwritten equivalents", fontsize=12, color="#526578")
from matplotlib.patches import Patch
fig.legend(handles=[Patch(color=colors[0], label="Myna"), Patch(color=colors[1], label="Handwritten")],
           loc="upper left", bbox_to_anchor=(.05, .815), ncol=2, frameon=False, fontsize=11,
           handlelength=1.2, columnspacing=2.5)
revision = data["build"].get("verified_source_commit", data["build"]["revision"])
fig.text(.055, .075,
         f"{data['host']['machine']} · {data['forks']} fresh processes/case · Median latency; whiskers show process min–max\n"
         f"Scalar workload, no control subtraction · Shared-host baseline · Source {revision[:7]}",
         fontsize=9, color="#526578", linespacing=1.6)
fig.text(.055, .015, "¹ Leaf calls require an explicit nonblocking native-leaf contract.", fontsize=8, color="#526578")
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
