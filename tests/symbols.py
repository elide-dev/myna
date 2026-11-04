"""Verify ELF sidecar identity and reject absent build IDs before modifying binaries."""
import hashlib
import os
from pathlib import Path
import platform
from support import ROOT, run

if platform.system() == "Linux":
    work = ROOT / "build/symbol-tests"
    work.mkdir(parents=True, exist_ok=True)
    source = work / "main.c"
    source.write_text("int main(void) { return 0; }\n")
    for name, flag in (("identified", "sha1"), ("unidentified", "none")):
        binary = work / name
        run(os.environ["CC"], "-g", f"-Wl,--build-id={flag}", source, "-o", binary)
        before = hashlib.sha256(binary.read_bytes()).digest()
        result = run(ROOT / "scripts/symbols.sh", binary, ok=False)
        if name == "unidentified":
            assert result.returncode != 0, "Missing build ID must fail"
            assert hashlib.sha256(binary.read_bytes()).digest() == before, "Rejected binary was modified"
        else:
            assert result.returncode == 0, result.stdout + result.stderr
            assert (work / "symbols/identified.debug").is_file()
            run(binary)
    print("ELF matching-ID and missing-ID checks passed")
