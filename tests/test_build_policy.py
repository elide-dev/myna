"""Regression checks for native flag preservation and deterministic SDK packaging."""
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tarfile
import tempfile
import unittest
import tomllib
import zipfile

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("native_image_build", ROOT / "scripts/native-image-build.py")
native_image = importlib.util.module_from_spec(spec)
spec.loader.exec_module(native_image)


class BuildPolicyTest(unittest.TestCase):
    def test_container_toolchain_checksums_match_locked_release(self):
        config = tomllib.loads((ROOT / "mise.toml").read_text())
        version = config["tools"]["github:elide-dev/toolchain"]["version"]
        lock = tomllib.loads((ROOT / "mise.lock").read_text())
        docker = (ROOT / "Dockerfile").read_text()
        self.assertIn(f"/releases/download/v{version}/elide-toolchain-{version}-linux-", docker)
        seen = set()
        for entry in lock["tools"]["github:elide-dev/toolchain"]:
            self.assertEqual(entry["version"], version)
            for platform, artifact in entry.items():
                if platform in ("platforms.linux-x64", "platforms.linux-arm64"):
                    self.assertIn(artifact["checksum"].removeprefix("sha256:"), docker)
                    seen.add(platform)
        self.assertEqual(seen, {"platforms.linux-x64", "platforms.linux-arm64"})

    def test_elide_wrapper_forwards_build_flags_and_source_epoch(self):
        with tempfile.TemporaryDirectory() as temporary:
            tool = Path(temporary) / "elide"
            tool.write_text(f"#!{sys.executable}\nimport json, sys\nprint(json.dumps(sys.argv[1:]))\n")
            tool.chmod(0o755)
            flags = {"CPPFLAGS": '-I"/include path"', "CFLAGS": "-g -O1",
                     "CXXFLAGS": "-g -O2", "LDFLAGS": "-Wl,-z,relro", "SOURCE_DATE_EPOCH": "1234"}
            result = subprocess.run(["bash", str(ROOT / "scripts/python.sh"), "unused.py"],
                                    env=dict(os.environ, **flags, PATH=temporary + os.pathsep + os.environ["PATH"]),
                                    text=True, capture_output=True, check=True)
            args = json.loads(result.stdout)
            forwarded = {args[i + 1]: args[i + 2] for i, arg in enumerate(args) if arg == "--env"}
            for key, value in flags.items():
                self.assertEqual(forwarded[key], value)

    def test_native_flags_preserve_quoting_and_order_without_shell_execution(self):
        flags = native_image.native_flags({"CPPFLAGS": '-I"/path with spaces"',
                                          "CFLAGS": '-g -DNAME="$(echo unsafe)"',
                                          "LDFLAGS": '-Wl,-z,relro -L"/lib path"'})
        self.assertEqual(flags, ["-H:CCompilerOption=-I/path with spaces", "-H:CCompilerOption=-g",
                                 "-H:CCompilerOption=-DNAME=$(echo unsafe)",
                                 "-H:NativeLinkerOption=-Wl,-z,relro",
                                 "-H:NativeLinkerOption=-L/lib path"])

    def test_archive_ignores_mtimes_permissions_and_umask_but_tracks_content(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            script = root / "scripts/package-values.py"
            script.parent.mkdir()
            shutil.copyfile(ROOT / "scripts/package-values.py", script)
            shutil.copyfile(ROOT / "scripts/reproducible_zip.py", script.parent / "reproducible_zip.py")
            (root / "Cargo.toml").write_text('[workspace.package]\nversion="1.2.3"\n')
            inputs = ["LICENSE", "LICENSE-APACHE", "LICENSE-MIT", "THIRD_PARTY.md",
                      ".dev/artifacts/jar/myna-values/myna-values.jar",
                      "target/package/myna-values-1.2.3.crate", "docs/values.md",
                      "examples/values.seam", "include/myna/values.h", "include/myna/values.hpp"]
            for name in inputs:
                path = root / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(name.encode())
            jar = root / ".dev/artifacts/jar/myna-values/myna-values.jar"
            with zipfile.ZipFile(jar, "w") as archive:
                archive.writestr(zipfile.ZipInfo("Example.class", (2020, 1, 1, 0, 0, 0)), b"class bytes")
            env = dict(os.environ, SOURCE_DATE_EPOCH="1234567890")
            def package():
                subprocess.run([sys.executable, str(script)], env=env, check=True, capture_output=True)
                return (root / "build/dist/myna-values-1.2.3.tar.gz").read_bytes()
            first = package()
            with zipfile.ZipFile(jar, "w") as archive:
                archive.writestr(zipfile.ZipInfo("Example.class", (2026, 1, 1, 0, 0, 0)), b"class bytes")
            for name in inputs:
                os.utime(root / name, (1700000000, 1700000000))
                (root / name).chmod(0o600)
            old_mask = os.umask(0o077)
            try:
                self.assertEqual(first, package())
            finally:
                os.umask(old_mask)
            archive = root / "build/dist/myna-values-1.2.3.tar.gz"
            with tarfile.open(archive) as tar:
                for member in tar:
                    self.assertEqual(member.mtime, 1234567890)
                    self.assertEqual((member.uid, member.gid), (0, 0))
                    self.assertEqual(member.mode, 0o755 if member.isdir() else 0o644)
            (root / "include/myna/values.h").write_text("changed ABI")
            self.assertNotEqual(hashlib.sha256(first).digest(), hashlib.sha256(package()).digest())


if __name__ == "__main__":
    unittest.main()
