"""Install the checksummed GraalVM used for Native Image and JVM benchmarks."""
import os
import hashlib
import json
from pathlib import Path
import platform
import tarfile
import tempfile
import subprocess

ROOT = Path(__file__).resolve().parents[1]
spec = json.loads((ROOT / "tools/graalvm.json").read_text())
target = spec["platforms"][f"{platform.system()}-{platform.machine()}"]
name = f"graalvm-{spec['edition']}-{spec['version']}-{target['archive']}"
directory = ROOT / ".dev/toolchains"
directory.mkdir(parents=True, exist_ok=True)
destination = directory / name
home_relative = Path("Contents/Home") if platform.system() == "Darwin" else Path(".")
if not (destination / ".myna-sha256").exists():
    cache = ROOT / "build/downloads"
    cache.mkdir(parents=True, exist_ok=True)
    archive = cache / f"{name}.tar.gz"
    def digest(path):
        with path.open("rb") as source:
            return hashlib.file_digest(source, "sha256").hexdigest()
    if not archive.exists() or digest(archive) != target["sha256"]:
        url = f"https://maven.elide.dev/gvm-elide/{spec['version']}/dist/{name}.tar.gz"
        print(f"Downloading {url}", flush=True)
        subprocess.run(["curl", "--fail", "--location", "--retry", "3", "--silent",
                        "--show-error", "--output", str(archive), url], check=True)
    if digest(archive) != target["sha256"]:
        raise RuntimeError("GraalVM archive checksum mismatch")
    with tempfile.TemporaryDirectory(dir=directory) as staging:
        with tarfile.open(archive) as source:
            source.extractall(staging, filter="data")
        extracted = Path(staging) / name
        if not (extracted / home_relative / "bin/native-image").is_file():
            raise RuntimeError("GraalVM archive has an unexpected layout")
        (extracted / ".myna-sha256").write_text(target["sha256"] + "\n")
        extracted.rename(destination)
if (destination / ".myna-sha256").read_text().strip() != target["sha256"]:
    raise RuntimeError("Installed GraalVM does not match the pinned archive")
link = directory / "graalvm"
if link.is_symlink():
    link.unlink()
elif link.exists():
    raise RuntimeError(f"Refusing to replace non-symlink {link}")
link.symlink_to(Path(destination.name) / home_relative, target_is_directory=True)
print(destination / home_relative)

# Compiler subprocesses launched by Native Image do not preserve custom env vars.
# Resolve the pinned installation through a local link as well.
if os.environ.get("ELIDE_TOOLCHAIN_HOME"):
    native = directory / "native"
    if native.is_symlink():
        native.unlink()
    elif native.exists():
        raise RuntimeError(f"Refusing to replace non-symlink {native}")
    native.symlink_to(os.environ["ELIDE_TOOLCHAIN_HOME"], target_is_directory=True)
