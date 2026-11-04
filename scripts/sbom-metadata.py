"""Add authoritative workspace license declarations missing from Cargo.lock scans."""
import argparse
import json
from pathlib import Path
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("sbom", type=Path)
args = parser.parse_args()
metadata = json.loads(subprocess.check_output(
    ["cargo", "metadata", "--locked", "--no-deps", "--format-version", "1"], text=True))
members = set(metadata["workspace_members"])
licenses = {(p["name"], p["version"]): p["license"]
            for p in metadata["packages"] if p["id"] in members}
if not licenses or not all(licenses.values()):
    raise SystemExit("Workspace packages must declare their licenses")
document = json.loads(args.sbom.read_text())
if document.get("spdxVersion") != "SPDX-2.3":
    raise SystemExit("Expected Syft SPDX 2.3 output")
found = set()
for package in document["packages"]:
    key = (package["name"], package.get("versionInfo"))
    # Syft omits purls for local path crates; require the root Cargo.lock origin.
    if key in licenses and package.get("sourceInfo") == "acquired package info from rust cargo manifest: /Cargo.lock":
        package["licenseDeclared"] = licenses[key]
        package.setdefault("annotations", []).append({
            "annotationType": "OTHER", "annotator": "Tool: myna-sbom-metadata",
            "annotationDate": document["creationInfo"]["created"],
            "comment": "Declared license resolved from cargo metadata for this workspace package; not a license conclusion about dependencies.",
        })
        found.add(key)
if found != licenses.keys():
    raise SystemExit("Source SBOM omitted workspace packages: " + str(licenses.keys() - found))
document["creationInfo"]["creators"].append("Tool: myna-sbom-metadata")
args.sbom.write_text(json.dumps(document, indent=2) + "\n")
print(f"Resolved declared licenses for {len(found)} workspace packages; dependency licenses unchanged")
