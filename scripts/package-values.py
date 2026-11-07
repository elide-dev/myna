# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Assemble the portable value SDK and a manifest for downstream pinning."""
import gzip
import hashlib
import os
from pathlib import Path
import shutil
import tarfile
import tomllib
from reproducible_zip import normalize

root = Path(__file__).resolve().parents[1]
version = tomllib.loads((root / "Cargo.toml").read_text())["workspace"]["package"]["version"]
name = f"myna-values-{version}"
stage = root / "build/package" / name
if stage.exists():
    shutil.rmtree(stage)
stage.mkdir(parents=True)
files = {
    **{root / name: name for name in ("LICENSE", "LICENSE-APACHE", "LICENSE-MIT", "THIRD_PARTY.md")},
    root / ".dev/artifacts/jar/myna-values/myna-values.jar": "myna-values.jar",
    root / f"target/package/{name}.crate": f"{name}.crate",
    root / "docs/values.md": "docs/values.md",
    root / "examples/values.seam": "examples/values.seam",
}
for path in (root / "include/myna").glob("values.*"):
    files[path] = f"include/myna/{path.name}"
for source, destination in files.items():
    dest = stage / destination
    dest.parent.mkdir(parents=True, exist_ok=True)
    if source.suffix == ".jar":
        normalize(source, dest)
    else:
        shutil.copyfile(source, dest)
manifest = "".join(f"{hashlib.sha256(p.read_bytes()).hexdigest()}  {p.relative_to(stage)}\n"
                   for p in sorted(stage.rglob("*")) if p.is_file())
(stage / "SHA256SUMS").write_text(manifest)
out = root / "build/dist"
out.mkdir(parents=True, exist_ok=True)
archive = out / f"{name}.tar.gz"
epoch = int(os.environ.get("SOURCE_DATE_EPOCH", "0"))
def normalized(info):
    info.uid = info.gid = 0
    info.uname = info.gname = ""
    info.mtime = epoch
    info.mode = 0o755 if info.isdir() else 0o644
    return info

with archive.open("wb") as raw:
    with gzip.GzipFile(filename="", mode="wb", fileobj=raw, mtime=epoch) as compressed:
        with tarfile.open(fileobj=compressed, mode="w", format=tarfile.PAX_FORMAT) as tar:
            tar.add(stage, arcname=name, filter=normalized)
(out / f"{name}.tar.gz.sha256").write_text(f"{hashlib.sha256(archive.read_bytes()).hexdigest()}  {archive.name}\n")
print(archive)
