"""Enforce Myna's coverage floor on Elide's aggregate JaCoCo XML report.

Remove this compatibility gate when elide-dev/runtime#2691 is available.
"""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

THRESHOLDS = {"LINE": 85, "BRANCH": 70}
DEFAULT_REPORT = Path(".dev/reports/coverage/jvm/jvm-test/jvm-test.xml")


def check_coverage(report):
    try:
        root = ET.parse(report).getroot()
    except ET.ParseError as error:
        raise ValueError(f"Invalid coverage XML: {error}") from error
    if root.tag != "report":
        raise ValueError("Expected a JaCoCo report")
    results = {}
    for kind, minimum in THRESHOLDS.items():
        counters = root.findall(f"counter[@type='{kind}']")
        if len(counters) != 1:
            raise ValueError(f"Expected one aggregate {kind} counter")
        counter = counters[0]
        try:
            missed, covered = int(counter.attrib["missed"]), int(counter.attrib["covered"])
        except (KeyError, ValueError) as error:
            raise ValueError(f"Invalid {kind} counter") from error
        total = missed + covered
        if missed < 0 or covered < 0 or total <= 0:
            raise ValueError(f"Invalid or empty {kind} coverage")
        percentage = 100 * covered / total
        # Compare integer counts so an exact threshold cannot fail through rounding.
        if 100 * covered < minimum * total:
            raise ValueError(f"{kind} coverage {percentage:.2f}% is below {minimum}%")
        results[kind] = percentage
    return results


if __name__ == "__main__":
    try:
        results = check_coverage(Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_REPORT)
        for kind, percentage in results.items():
            print(f"{kind}: {percentage:.2f}% (minimum {THRESHOLDS[kind]}%)")
    except (OSError, ValueError) as error:
        print(f"Coverage check failed: {error}", file=sys.stderr)
        raise SystemExit(1)
