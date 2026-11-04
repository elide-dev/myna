"""Assemble the portable value SDK and a manifest for downstream pinning."""
import hashlib
from pathlib import Path
import shutil
import tarfile
import tomllib

root = Path(__file__).resolve().parents[1]
version = tomllib.loads((root / "Cargo.toml").read_text())["workspace"]["package"]["version"]
name = f"myna-values-{version}"
stage = root / "build/package" / name
if stage.exists():
    shutil.rmtree(stage)
stage.mkdir(parents=True)
files = {
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
    shutil.copyfile(source, dest)
manifest = "".join(f"{hashlib.sha256(p.read_bytes()).hexdigest()}  {p.relative_to(stage)}\n"
                   for p in sorted(stage.rglob("*")) if p.is_file())
(stage / "SHA256SUMS").write_text(manifest)
out = root / "build/dist"
out.mkdir(parents=True, exist_ok=True)
archive = out / f"{name}.tar.gz"
with tarfile.open(archive, "w:gz") as tar:
    tar.add(stage, arcname=name)
(out / f"{name}.tar.gz.sha256").write_text(f"{hashlib.sha256(archive.read_bytes()).hexdigest()}  {archive.name}\n")
print(archive)
