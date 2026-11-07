# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Extract named ARM64 scalar adapter call sites without rebuilding or remeasuring."""
import argparse
import hashlib
import json
from pathlib import Path
import re


def digest(path):
    checksum = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            checksum.update(block)
    return checksum.hexdigest()


def functions(path, prefixes):
    blocks = {}
    symbols = set()
    current = None
    format_seen = False
    with path.open() as stream:
        for line in stream:
            line = line.rstrip("\n")
            if "file format " in line:
                file_format = line.partition("file format ")[2].strip()
                if file_format not in ("mach-o arm64", "elf64-littleaarch64"):
                    raise RuntimeError("Unsupported objdump format: " + file_format)
                format_seen = True
            header = re.fullmatch(r"([0-9a-f]+) <(.+)>:", line)
            if header:
                symbol = header[2]
                symbols.add(symbol)
                current = symbol if any(symbol.removeprefix("_").startswith(prefix.removeprefix("_"))
                                        for prefix in prefixes) else None
                if current is not None:
                    blocks[current] = [line]
            elif current is not None:
                blocks[current].append(line)
    if not format_seen:
        raise RuntimeError("Missing objdump file format")
    return blocks, symbols


def instructions(lines):
    result = []
    for line in lines:
        instruction = re.match(r"\s*[0-9a-f]+:\s+[0-9a-f]{8}\s+(.+)", line)
        if instruction and "<unknown>" not in instruction[1]:
            result.append(instruction[1].split(";", 1)[0].strip())
    return result


def normalized(symbol, lines):
    result = []
    for instruction in instructions(lines):
        def target(match):
            name = match[1]
            return "<SELF" + name[len(symbol):] + ">" if name.startswith(symbol) else "<" + name + ">"
        result.append(re.sub(r"\s+", " ", re.sub(r"0x[0-9a-f]+ <([^>]+)>", target, instruction)))
    return result


parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("directory", type=Path)
parser.add_argument("output", type=Path)
args = parser.parse_args()
work = args.directory.resolve()
root = work.parent.parent
build = json.loads((work / "build.json").read_text())
if not build["target"].startswith("aarch64-"):
    raise RuntimeError("This instruction decoder currently requires ARM64 objdump output")
for name, expected in build["assembly"].items():
    if digest(root / name) != expected:
        raise RuntimeError("Assembly changed since build: " + name)
for name in ("native", "ffm-aot"):
    if digest(work / name) != build["binaries"][str((work / name).relative_to(root))]:
        raise RuntimeError("Executable changed since build: " + name)

selectors = {
    "native": {
        "generated-normal": "_Native$GeneratedNormal_run_",
        "manual-normal": "_Native$ManualNormal_run_",
        "generated-leaf": "_Native$GeneratedLeaf_run_",
        "manual-leaf": "_Native$ManualLeaf_run_",
        "manual-leaf-stub": "_Native_leaf_",
        "generated-static-jni": "_Native$GeneratedStaticJni_run_",
        "manual-static-jni": "_Native$ManualStaticJni_run_",
        "generated-jni-java-stub": "_GeneratedJNI_step_",
        "manual-jni-java-stub": "_ManualJNI_step_",
        "generated-jni-c-wrapper": "_Java_bench_GeneratedJNI_step__J",
        "manual-jni-c-wrapper": "_Java_bench_ManualJNI_step",
    },
    "ffm-aot": {
        "generated-ffm": "_Jvm$GeneratedFfm_run_",
        "manual-ffm": "_Jvm$ManualFfm_run_",
        "generated-library-ffm": "_Jvm$GeneratedLibraryFfm_run_",
        "manual-library-ffm": "_Jvm$ManualLibraryFfm_run_",
        "generated-critical-ffm": "_Jvm$GeneratedCriticalFfm_run_",
        "manual-bound-critical-ffm": "_Jvm$ManualBoundCriticalFfm_run_",
        "manual-bound-ffm": "_Jvm$ManualBoundFfm_run_",
        "generated-typed-critical-ffm": "_Jvm$GeneratedTypedCriticalFfm_run_",
        "manual-unbound-critical-ffm": "_Jvm$ManualUnboundCriticalFfm_run_",
    },
}
selection = {}
normalized_code = {}
assembly = []
absence = {}
for mode, cases in selectors.items():
    blocks, symbols = functions(work / f"{mode}.asm", cases.values())
    for case, prefix in cases.items():
        matches = [symbol for symbol in blocks if symbol.removeprefix("_").startswith(prefix.removeprefix("_"))]
        if len(matches) != 1:
            raise RuntimeError(f"Expected one symbol for {mode}/{case}: {matches}")
        symbol = matches[0]
        lines = blocks[symbol]
        code = instructions(lines)
        if not code:
            raise RuntimeError(f"No decoded ARM64 instructions for {mode}/{case}")
        key = f"{mode}/{case}"
        normalized_code[key] = normalized(symbol, lines)
        selection[key] = {
            "symbol": symbol,
            "instructions_including_nops": len(code),
            "nops": sum(instruction == "nop" for instruction in code),
            "direct_call_targets": sorted(set(match[1] for line in code
                if (match := re.search(r"^bl\s+0x[0-9a-f]+ <([^>]+)>", line)))),
            "indirect_call_sites_including_cold_paths": sum(bool(re.match(r"blr\s", line)) for line in code),
            "normalized_sha256": hashlib.sha256("\n".join(normalized_code[key]).encode()).hexdigest(),
        }
        assembly.extend([f"; {key}", *lines, ""])
    if mode == "native":
        for prefix in ("_Generated_leaf_", "_Generated$leaf_Leaf_invoke_"):
            absence[prefix] = not any(symbol.removeprefix("_").startswith(prefix.removeprefix("_")) for symbol in symbols)
            if not absence[prefix]:
                raise RuntimeError("Generated leaf adapter unexpectedly retained: " + prefix)

pairs = {}
for title, left, right in (
        ("normal-direct", "native/generated-normal", "native/manual-normal"),
        ("jni-c-wrappers", "native/generated-jni-c-wrapper", "native/manual-jni-c-wrapper")):
    equal = normalized_code[left] == normalized_code[right]
    pairs[title] = {"left": left, "right": right, "equal_after_target_normalization": equal}
    if not equal:
        raise RuntimeError("Previously equivalent scalar instruction sequence changed: " + title)

asm_path = args.output.with_suffix(".asm")
asm_path.parent.mkdir(parents=True, exist_ok=True)
asm_path.write_text("\n".join(assembly).rstrip() + "\n")
evidence = {
    "schema": 1,
    "target": build["target"],
    "build": build,
    "extractor_sha256": digest(Path(__file__)),
    "extraction_command": ["python3", str(Path(__file__).resolve()), str(work), str(args.output)],
    "selected_assembly_sha256": digest(asm_path),
    "selection": selection,
    "normalized_pairs": pairs,
    "absent_generated_leaf_stubs": absence,
    "limits": [
        "Static instruction counts include cold paths and alignment nops; they are not dynamic instruction counts.",
        "Target normalization removes disassembly addresses, preserves target symbol names, and makes self-relative labels explicit.",
        "Direct normal loops and JNI C wrappers match after only target normalization; other call sites are retained without an equivalence claim.",
        "Leaf and critical loops retain safepoint checks between bounded loop chunks; lack of a normal transition is not lack of GC cooperation.",
        "Allocation calls on cold error paths do not establish steady-state allocation; use per-sample allocation counters.",
    ],
}
args.output.with_suffix(".json").write_text(json.dumps(evidence, indent=2) + "\n")
for key, value in selection.items():
    print(key, value["instructions_including_nops"], "instructions", value["nops"], "nops")
print(json.dumps(pairs))
