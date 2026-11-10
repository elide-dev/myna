# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Verify sidecar identity: ELF build IDs must exist, and a Mach-O UUID must not name two dSYMs."""
import hashlib
import os
from pathlib import Path
import platform
import shutil
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

if platform.system() == "Darwin":
    # Mach-O images hold no DWARF, so line-only source edits keep the linker's UUID while
    # changing the dSYM. llvm/debug-identity.cmake must give such builds distinct UUIDs.
    work = ROOT / "build/symbol-tests"
    shutil.rmtree(work, ignore_errors=True)
    project = work / "project"
    project.mkdir(parents=True)
    (project / "CMakeLists.txt").write_text(
        "cmake_minimum_required(VERSION 3.20)\nproject(identity LANGUAGES C)\n"
        "add_executable(app main.c)\ntarget_compile_options(app PRIVATE -g)\n"
        "option(BIND \"Bind the UUID to debug information\" OFF)\nif(BIND)\n"
        f"  include(\"{ROOT / 'llvm/debug-identity.cmake'}\")\n  myna_bind_debug_identity(app)\nendif()\n")

    def build(name, bind, prefix):
        # One source and build path for every variant: only the line numbers may differ.
        # Start from an empty build tree; same-second rewrites must not reuse stale outputs.
        shutil.rmtree(work / "cmake", ignore_errors=True)
        (project / "main.c").write_text(prefix + "int main(void) { return 0; }\n")
        run("cmake", "-S", project, "-B", work / "cmake", f"-DBIND={bind}", "-DCMAKE_BUILD_TYPE=Release",
            f"-DCMAKE_TOOLCHAIN_FILE={ROOT / 'tools/native.cmake'}")
        run("cmake", "--build", work / "cmake")
        (work / name).mkdir()
        binary = work / name / "app"
        shutil.copy2(work / "cmake/app", binary)
        uuid = run(ROOT / "scripts/symbols.sh", binary).stdout.strip().rsplit(" ", 1)[-1]
        assert len(uuid) == 36, uuid
        run(os.path.join(os.environ["LLVM_BIN"], "llvm-dwarfdump"), "--verify", "--quiet", work / name / "symbols/app.dsym")
        run(binary)
        return uuid, (work / name / "symbols/app.dsym").read_bytes(), binary.read_bytes()

    for bind in ("OFF", "ON"):
        first, again, moved = (build(f"{bind}-{name}", bind, prefix)
                               for name, prefix in (("first", ""), ("again", ""), ("moved", "/* moved */\n")))
        assert first == again, "Identical inputs must reproduce the executable, UUID and dSYM"
        assert first[1] != moved[1], "Fixture must change debug information"
        if bind == "OFF":
            # Canary: without the binding this toolchain reuses one UUID for both dSYMs.
            assert first[0] == moved[0] and first[2] == moved[2], "Canary no longer reproduces the shared UUID"
        else:
            assert first[0] != moved[0], "Different dSYM bytes share one Mach-O UUID"
    print("Mach-O debug-identity canary and distinct-UUID checks passed")
