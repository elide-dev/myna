# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Select production sources from JaCoCo's combined test/production report."""
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
# Raw word loads and CFunction calls cannot execute in a stock JVM. Real SVM
# integration tests exercise these classes; do not count them as JVM coverage.
NATIVE_ONLY = {"dev/elide/myna/values/NativeValueTape.java", "dev/elide/myna/values/SimdNativeValueTape.java",
               "dev/elide/myna/truffle/ImageMemory.java"}

def select(report, output):
    root = ET.parse(report).getroot()
    expected = {str(p.relative_to(ROOT / "src" / source / "java"))
                for source in ("main", "feature", "values", "truffle")
                for p in (ROOT / "src" / source / "java").rglob("*.java")} - NATIVE_ONLY
    seen = set()
    result = ET.Element("report", name="Myna production JVM")
    totals = {}
    for package in root.findall("package"):
        selected = ET.Element("package", name=package.attrib["name"])
        files = set()
        counts = {}
        for source in package.findall("sourcefile"):
            name = package.attrib["name"] + "/" + source.attrib["name"]
            if name not in expected:
                continue
            seen.add(name)
            files.add(source.attrib["name"])
            selected.append(source)
            for counter in source.findall("counter"):
                kind = counter.attrib["type"]
                values = [int(counter.attrib[k]) for k in ("missed", "covered")]
                if min(values) < 0:
                    raise ValueError("Negative coverage counter")
                counts[kind] = [a+b for a, b in zip(counts.get(kind, [0, 0]), values)]
        if not files:
            continue
        for cls in package.findall("class"):
            if cls.get("sourcefilename") in files:
                selected.insert(0, cls)
        for kind, (missed, covered) in counts.items():
            ET.SubElement(selected, "counter", type=kind, missed=str(missed), covered=str(covered))
            totals[kind] = [a+b for a, b in zip(totals.get(kind, [0, 0]), [missed, covered])]
        result.append(selected)
    if seen != expected:
        raise ValueError(f"Production sources missing from coverage: {sorted(expected - seen)}")
    for kind, (missed, covered) in totals.items():
        ET.SubElement(result, "counter", type=kind, missed=str(missed), covered=str(covered))
    ET.ElementTree(result).write(output, encoding="utf-8", xml_declaration=True)

if __name__ == "__main__":
    select(ROOT / ".dev/reports/coverage/jvm/jvm-test/jvm-test.xml", ROOT / "build/reports/jvm-production.xml")
