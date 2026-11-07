"""Check primary-language style, Java warnings, and native security analysis.

Run through mise. Generated outputs, vendored code and benchmark fixtures are not
formatter inputs; their bytes and upstream styles are intentionally preserved.
"""
import argparse
import hashlib
import os
from pathlib import Path
import subprocess
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
FORMAT_VERSION = "2.0.0"
FORMAT_SHA256 = "c78feb48c549fa7114b7ea11db3b456808f7b371ffe84e6643a44dd2f9588639"


def run(*args):
    subprocess.run(list(map(str, args)), cwd=ROOT, check=True)


def formatter():
    jar = ROOT / f"build/tools/google-java-format-{FORMAT_VERSION}.jar"
    if not jar.exists():
        url = ("https://repo.maven.apache.org/maven2/com/google/googlejavaformat/"
               f"google-java-format/{FORMAT_VERSION}/google-java-format-{FORMAT_VERSION}-all-deps.jar")
        with urllib.request.urlopen(url, timeout=60) as response:
            data = response.read()
        if hashlib.sha256(data).hexdigest() != FORMAT_SHA256:
            raise ValueError("Java formatter checksum mismatch")
        jar.parent.mkdir(parents=True, exist_ok=True)
        jar.write_bytes(data)
    if hashlib.sha256(jar.read_bytes()).hexdigest() != FORMAT_SHA256:
        raise ValueError("Cached Java formatter checksum mismatch")
    return jar


def style(fix=False):
    java = sorted((ROOT / "src").rglob("*.java"))
    native = [ROOT / "llvm/main.cpp", *sorted((ROOT / "include/myna").glob("values.*"))]
    run("java", "-jar", formatter(), *(["--replace"] if fix else ["--dry-run", "--set-exit-if-changed"]), *java)
    run("clang-format", *(["-i"] if fix else ["--dry-run", "--Werror"]), *native)
    run("cargo", "fmt", "--all", *([] if fix else ["--check"]))


def java_warnings():
    home = Path(os.environ["GRAALVM_HOME"])
    sdk = [home / "lib/svm" / name for name in
           ("svm-shared.jar", "svm-guest-staging.jar", "svm-guest.jar")]
    sources = [p for scope in ("main", "feature", "values")
               for p in sorted((ROOT / "src" / scope).rglob("*.java"))]
    run(home / "bin/javac", "-Xlint:all", "-Werror", "-cp", os.pathsep.join(map(str, sdk)),
        "-d", ROOT / "build/quality/classes", *sources)


def native_analysis():
    config = subprocess.check_output([os.environ["LLVM_CONFIG"], "--cmakedir"], text=True).strip()
    build = ROOT / "build/quality/llvm"
    run("cmake", "--fresh", "-S", ROOT / "llvm", "-B", build,
        f"-DLLVM_DIR={config}", f"-DCMAKE_TOOLCHAIN_FILE={ROOT / 'tools/native.cmake'}",
        "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON", "-DCMAKE_BUILD_TYPE=Release")
    run("clang-tidy", ROOT / "llvm/main.cpp", "-p", build)
    # Exercise inline value producers with their real fuzz workload as a TU.
    flags = ["-std=c++17", f"-I{ROOT / 'include'}"]
    if os.environ.get("SDKROOT"):
        flags += ["-isysroot", os.environ["SDKROOT"]]
    run("clang-tidy", ROOT / "tests/fuzz/values.cpp", "--", *flags)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mode", choices=("style", "format", "java", "native"))
    mode = parser.parse_args().mode
    if mode in {"style", "format"}:
        style(mode == "format")
    elif mode == "java":
        java_warnings()
    else:
        native_analysis()
