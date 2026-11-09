# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Check primary-language style, Java warnings, and native security analysis.

Run through mise. Generated outputs, vendored code and benchmark fixtures are not
formatter inputs; their bytes and upstream styles are intentionally preserved.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
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


def rust_toolchain():
    """Keep rustc on elide-dev/toolchain's recommended pin so Rust bitcode stays readable by its LLVM."""
    def env(name):
        value = os.environ.get(name)
        if not value:
            raise SystemExit(f"{name} is not set; run this check through mise so the toolchain environment loads")
        return value
    pin = re.search(r'^rust\s*=\s*"([^"]+)"', (ROOT / "mise.toml").read_text(), re.M)
    if not pin:
        raise SystemExit("mise.toml has no [tools] rust pin")
    recommended = (Path(env("ELIDE_TOOLCHAIN_HOME")) / "rustversion").read_text().strip()
    if pin.group(1) != recommended:
        raise SystemExit(f"mise.toml pins Rust {pin.group(1)}; elide-dev/toolchain recommends {recommended}")
    rustc = subprocess.check_output(["rustc", "-vV"], cwd=ROOT, text=True)
    rust_llvm = re.search(r"^LLVM version: (\d+)", rustc, re.M)
    if not rust_llvm:
        raise SystemExit(f"rustc -vV reports no LLVM version:\n{rustc}")
    bundle_llvm = int(subprocess.check_output([env("LLVM_CONFIG"), "--version"], text=True).split(".")[0])
    if int(rust_llvm.group(1)) > bundle_llvm:
        raise SystemExit(f"rustc uses LLVM {rust_llvm.group(1)}, newer than the toolchain's LLVM {bundle_llvm}")


def style(fix=False):
    rust_toolchain()
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
    # myna-truffle compiles against the public Truffle API with the Truffle DSL processor. Elide
    # resolves both into its dependency cache; `restricted` is MemorySegment.reinterpret, which the
    # native buffer view needs, and `processing` is javac's note that the processor leaves @Cached
    # to the library exports it generates.
    graal = ROOT / ".dev/dependencies/m2/org/graalvm"
    version = json.loads((ROOT / "tools/truffle.json").read_text())["version"]
    processor = sorted(graal.glob(f"truffle/truffle-dsl-processor/{version}/truffle-dsl-processor-*.jar"))
    truffle = [jar for jar in sorted(graal.rglob(f"*/{version}/*.jar"))
               if "dsl-processor" not in jar.name and not jar.name.endswith(("-sources.jar", "-javadoc.jar"))]
    if not processor or not truffle:
        raise RuntimeError("Truffle dependencies are missing; run `elide build :truffle-jar` first")
    run(home / "bin/javac", "-Xlint:all,-restricted,-processing", "-Werror",
        "-processorpath", str(processor[-1]), "-cp", os.pathsep.join(map(str, truffle)),
        "-d", ROOT / "build/quality/truffle", *sorted((ROOT / "src/truffle").rglob("*.java")))


def compiler_includes():
    # clang-tidy does not execute compiler wrappers from compile_commands.json.
    # Ask the actual pinned driver for its implicit C++/sysroot search paths.
    probe = subprocess.run([os.environ["CXX"], "-E", "-x", "c++", "-", "-v"],
                           input="", text=True, capture_output=True, check=True)
    flags, collecting = [], False
    for line in probe.stderr.splitlines():
        if line.strip() == "#include <...> search starts here:":
            collecting = True
        elif collecting and line.strip() == "End of search list.":
            break
        elif collecting:
            path = line.strip()
            framework = path.endswith(" (framework directory)")
            path = path.removesuffix(" (framework directory)")
            flags += ["-iframework" if framework else "-isystem", path]
    if not flags:
        raise RuntimeError("Pinned C++ driver returned no include search paths")
    return flags


def native_analysis():
    config = subprocess.check_output([os.environ["LLVM_CONFIG"], "--cmakedir"], text=True).strip()
    build = ROOT / "build/quality/llvm"
    run("cmake", "--fresh", "-S", ROOT / "llvm", "-B", build,
        f"-DLLVM_DIR={config}", f"-DCMAKE_TOOLCHAIN_FILE={ROOT / 'tools/native.cmake'}",
        "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON", "-DCMAKE_BUILD_TYPE=Release")
    includes = compiler_includes()
    run("clang-tidy", ROOT / "llvm/main.cpp", "-p", build,
        *(f"--extra-arg={flag}" for flag in includes))
    # Exercise inline value producers with their real fuzz workload as a TU.
    flags = ["-std=c++17", f"-I{ROOT / 'include'}", *includes]
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
