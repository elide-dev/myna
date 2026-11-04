"""Pinned upstream simdjson conformance fixture; not a production JSON parser."""
import hashlib
import os
from pathlib import Path
import sys
import unittest
from support import ROOT, main, run

VERSION = "5.1.1"
HASHES = {
    "simdjson.h": "f3e5ee5f39051960b5ce65762ee4b6919caf270675eb52240ebd92f21a5bcefd",
    "simdjson.cpp": "3ea5f501cef6c2e16e28095b566feeb56021ca473aaf50aad32102f45d734195",
}

def source():
    destination = ROOT / f"build/simdjson-{VERSION}"
    destination.mkdir(parents=True, exist_ok=True)
    for name, digest in HASHES.items():
        path = destination / name
        if not path.exists():
            temporary = path.with_suffix(path.suffix + ".download")
            run("curl", "--fail", "--silent", "--show-error", "--location",
                "--retry", "3", "--retry-all-errors", "--connect-timeout", "30",
                "--max-time", "180", "--output", temporary,
                f"https://raw.githubusercontent.com/simdjson/simdjson/v{VERSION}/singleheader/{name}")
            data = temporary.read_bytes()
            temporary.unlink()
            if hashlib.sha256(data).hexdigest() != digest:
                raise AssertionError(f"simdjson checksum mismatch: {name}")
            path.write_bytes(data)
        if hashlib.sha256(path.read_bytes()).hexdigest() != digest:
            raise AssertionError(f"simdjson checksum mismatch: {path}")
    return destination

def build_cpp(work):
    work.mkdir(parents=True, exist_ok=True)
    upstream = source()
    objects = []
    for name, path in [("producer", ROOT / "tests/values/cpp/producer.cpp"), ("simdjson", upstream / "simdjson.cpp")]:
        obj = work / f"{name}.o"
        run(os.environ.get("CXX", "c++"), "-std=c++17", "-O2", "-fPIC",
            "-I", ROOT / "include", "-I", upstream, "-c", path, "-o", obj)
        objects.append(obj)
    return objects

class CppValueTests(unittest.TestCase):
    def test_sanitized_direct_producer_and_simdjson(self):
        work = ROOT / "build/values-cpp"
        work.mkdir(parents=True, exist_ok=True)
        upstream = source()
        app = work / "test"
        run(os.environ.get("CXX", "c++"), "-std=c++17", "-O1", "-g",
            "-fsanitize=address,undefined", "-DMYNA_STANDALONE_TEST",
            # LLVM 23.1.4 ld64.lld + ASan loses C++ exception catches on macOS.
            # Keep pinned Clang/compiler-rt; Apple's linker preserves unwinding.
            *(["-fuse-ld=/usr/bin/ld"] if sys.platform == "darwin" else []),
            "-I", ROOT / "include", "-I", upstream,
            ROOT / "tests/values/cpp/producer.cpp", upstream / "simdjson.cpp", "-o", app)
        self.assertIn("conformance passed", run(app).stdout)
        c_file = work / "layout.c"
        c_file.write_text('#include <myna/values.h>\nint main(void) { return MYNA_VALUE_ABI != 3; }\n')
        run(os.environ.get("CC", "cc"), "-std=c11", "-Wall", "-Wextra", "-Werror",
            "-I", ROOT / "include", c_file, "-o", work / "layout")
        run(work / "layout")

if __name__ == "__main__":
    main(sys.modules[__name__], "values-cpp")
