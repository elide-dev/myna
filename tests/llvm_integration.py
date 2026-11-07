"""Prove actual object/rlib changes, optimization effects, and ThinLTO/full-LTO linkage."""
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import sys
import tempfile
import unittest
from support import BINARY, ROOT, TARGET, main, run

LLVM = Path(os.environ.get("LLVM_BIN", "/opt/homebrew/opt/llvm/bin" if sys.platform == "darwin" else "/usr/lib/llvm-23/bin"))
HELPER = Path(os.environ.get("MYNA_LLVM", ROOT / "build/llvm/myna-llvm"))

def llvm(tool): return LLVM / tool

class LlvmIntegrationTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="myna-llvm-")
        self.addCleanup(self.temp.cleanup)
        self.work = Path(self.temp.name)
        run(BINARY, "generate", ROOT / "examples/buffer.seam", "--out", self.work / "generated", "--target", TARGET)
        self.contracts = self.work / "generated/seam.ll"
        self.assertIn("23.", run(llvm("llvm-config"), "--version").stdout)
    def apply(self, input, output, contracts=None, ok=True):
        return run(BINARY, "apply", "--contracts", contracts or self.contracts, "--input", input, "--output", output, "--llvm-tool", HELPER, ok=ok)
    def ir(self, path):
        return run(llvm("llvm-dis"), path, "-o", "-").stdout
    def c_object(self, mode="thin"):
        source = self.work / "caller.c"
        source.write_text('''#include <stdint.h>
extern int64_t seam_inspect(const int64_t *);
__attribute__((noinline)) int64_t c_probe(void) { int64_t value = 42; (void)seam_inspect(&value); return value; }
''')
        output = self.work / "caller.o"
        run(llvm("clang"), "-O1", f"-flto={mode}", "-c", source, "-o", output)
        return output
    def optimized(self, path):
        return run(llvm("opt"), "-passes=default<O2>", "-S", path, "-o", "-").stdout
    def test_real_c_and_rust_objects_optimize_and_link_thin_lto(self):
        self.prove_lto("thin")

    def test_file_errors_and_malformed_containers_preserve_outputs(self):
        original = self.c_object()
        output = self.work / "existing.o"
        output.write_bytes(b"preserve")
        bad_contract = self.work / "syntax.ll"
        bad_contract.write_text("this is not LLVM IR")
        malformed = self.work / "malformed.a"
        malformed.write_bytes(b"!<arch>\n" + b"invalid archive member" * 4)
        thin = self.work / "thin.a"
        run(llvm("llvm-ar"), "rcsT", thin, original)
        truncated_elf = self.work / "truncated.o"
        truncated_elf.write_bytes(b"\x7fELF\x02\x01\x01" + b"\0" * 9 + b"\x01\0\x3e\0")
        cases = [(self.work / "absent.ll", original, output),
                 (bad_contract, original, output),
                 (self.contracts, self.work / "absent.o", output),
                 (self.contracts, malformed, output),
                 (self.contracts, thin, output),
                 (self.contracts, truncated_elf, output),
                 (self.contracts, original, self.work / "missing-directory/output.o"),
                 (self.contracts, original, self.work)]
        for contract, obj, dest in cases:
            with self.subTest(input=obj.name, contract=contract.name, output=str(dest)):
                result = run(HELPER, "--contracts", contract, "--input", obj, "--output", dest, ok=False)
                self.assertNotEqual(result.returncode, 0)
                self.assertEqual(output.read_bytes(), b"preserve")

    def test_invalid_applied_metadata_and_conflicting_call_attributes(self):
        prefix = f'target triple = "{TARGET}"\n'
        cases = [
            ('declare i64 @seam_inspect(ptr)\n!myna.applied = !{}\n', 'fingerprint'),
            ('@seam_inspect = global i64 0\n', 'alias or non-function'),
            ('declare i64 @seam_inspect(ptr) noreturn\n', 'contradict'),
            ('declare i64 @seam_inspect(ptr)\ndefine i64 @caller(ptr %p) {\n'
             '%r = call i64 @seam_inspect(ptr writeonly %p)\nret i64 %r\n}\n', 'contradict'),
            ('declare i64 @seam_inspect(ptr)\ndefine i64 @caller(ptr %p) {\n'
             '%r = call i64 @seam_inspect(ptr byval(i64) %p)\nret i64 %r\n}\n', 'unsupported ABI'),
        ]
        for i, (body, expected) in enumerate(cases):
            with self.subTest(case=i):
                source = self.work / f"negative-{i}.ll"
                source.write_text(prefix + body)
                obj = source.with_suffix(".bc")
                run(llvm("llvm-as"), source, "-o", obj)
                result = self.apply(obj, self.work / f"negative-{i}.o", ok=False)
                self.assertNotEqual(result.returncode, 0)
                self.assertIn(expected, result.stdout + result.stderr)

    def test_real_c_and_rust_objects_optimize_and_link_full_lto(self):
        self.prove_lto("full")

    def prove_lto(self, mode):
        original = self.c_object(mode)
        original_hash = hashlib.sha256(original.read_bytes()).hexdigest()
        annotated = self.work / "caller.seam.o"
        self.apply(original, annotated)
        self.assertEqual(original_hash, hashlib.sha256(original.read_bytes()).hexdigest())
        self.assertIn("load i64", self.optimized(original))
        self.assertNotIn("load i64", self.optimized(annotated))
        self.assertIn("ret i64 42", self.optimized(annotated))
        self.assertIn("captures(none)", self.ir(annotated))
        rust = self.work / "caller.rs"
        rust.write_text('''#![no_std]
unsafe extern "C" { fn seam_inspect(value: *const i64) -> i64; }
#[unsafe(no_mangle)]
#[inline(never)]
pub extern "C" fn rust_probe() -> i64 {
 let mut value: i64 = 42;
 unsafe { seam_inspect(&raw mut value); }
 value
}
''')
        self.assertRegex(run("rustc", "-vV").stdout, r"LLVM version: 23\.")
        rust_obj = self.work / "rust.o"
        rust_flags = ["--edition=2024", "--crate-type=lib", "-Copt-level=1", "-Cpanic=abort",
                      *(["--emit=obj", "-Clinker-plugin-lto"] if mode == "thin"
                        else ["--emit=llvm-bc", "-Clto=fat"])]
        run("rustc", *rust_flags, rust, "-o", rust_obj)
        rust_annotated = self.work / "rust.seam.o"
        self.apply(rust_obj, rust_annotated)
        self.assertIn("load i64", self.optimized(rust_obj))
        self.assertNotIn("load i64", self.optimized(rust_annotated))
        self.assertIn("ret i64 42", self.optimized(rust_annotated))
        for obj in [original, annotated, rust_obj, rust_annotated]:
            # Rust fat LTO can carry a FULL_LTO_GLOBALVAL_SUMMARY_BLOCK; it is not a
            # ThinLTO input merely because it contains a summary.
            has_thin_summary = "<GLOBALVAL_SUMMARY_BLOCK " in run(llvm("llvm-bcanalyzer"), "-dump", obj).stdout
            self.assertEqual(has_thin_summary, mode == "thin", f"{obj.name}: preserve {mode} LTO mode")
        native = self.work / "native.c"
        native.write_text('#include <stdint.h>\nint64_t seam_inspect(const int64_t *value) { return *value; }\n')
        native_obj = self.work / "native.o"
        run(llvm("clang"), "-O2", "-c", native, "-o", native_obj)
        native_hash = hashlib.sha256(native_obj.read_bytes()).hexdigest()
        main_c = self.work / "main.c"
        main_c.write_text('''#include <stdint.h>
extern int64_t c_probe(void); extern int64_t rust_probe(void);
int main(void) { return c_probe() == 42 && rust_probe() == 42 ? 0 : 1; }
''')
        linker = shutil.which("ld64.lld" if sys.platform == "darwin" else "ld.lld")
        self.assertIsNotNone(linker, "LLD 23 is required")
        binary = self.work / "linked"
        save = "-Wl,-save-temps" if sys.platform == "darwin" else "-Wl,--save-temps"
        link_flags = [f"-fuse-ld={linker}", f"-flto={mode}", "-O2", save]
        run(llvm("clang"), *link_flags, main_c, annotated, rust_annotated, native_obj, "-o", binary)
        run(binary)
        self.assertEqual(native_hash, hashlib.sha256(native_obj.read_bytes()).hexdigest())
        saved = list(self.work.glob("*.opt.bc"))
        self.assertTrue(saved, f"LLD must save optimized {mode} LTO bitcode")
        caller_irs = [self.ir(p) for p in saved if "@c_probe" in self.ir(p) or "@rust_probe" in self.ir(p)]
        self.assertTrue(any("captures(none)" in text for text in caller_irs), f"attributes survive into {mode} LTO backend")
        # FatLTO sysroot libraries can contribute unrelated loads to the merged module.
        # Inspect the probe bodies, not the entire linked runtime.
        probe_bodies = [body for text in caller_irs for body in re.findall(
            r"(?ms)^define [^\n]*@(?:c_probe|rust_probe)[^(]*\([^\n]*\).*?^}", text)]
        self.assertGreaterEqual(len(probe_bodies), 2)
        self.assertTrue(all("load i64" not in body for body in probe_bodies), "both backends eliminate the post-call reload")
        if mode == "thin":
            self.assertTrue(any("ret i64 42" in text for text in caller_irs))
        else:
            self.assertEqual(len(saved), 1, "full LTO optimizes one merged module")
            # Full LTO propagates both constant return values into main, then erases the
            # now-unused probe results. Its final proof is main's unconditional success.
            self.assertRegex(caller_irs[0], r"(?s)define [^\n]*@main\(.*?\n  ret i32 0\n}")
        evidence = ROOT / f"build/reports/{mode}lto" / TARGET
        evidence.mkdir(parents=True, exist_ok=True)
        retained = [original, annotated, rust_obj, rust_annotated, native_obj, binary,
                    self.work / "caller.c", rust, native, main_c, self.contracts]
        for path in retained:
            shutil.copyfile(path, evidence / path.name)
        for label, obj in [("c-before", original), ("c-after", annotated),
                           ("rust-before", rust_obj), ("rust-after", rust_annotated)]:
            (evidence / f"{label}.ll").write_text(self.optimized(obj))
        for i, text in enumerate(caller_irs):
            (evidence / f"linked-{i}.ll").write_text(text)
        for i, path in enumerate(saved):
            shutil.copyfile(path, evidence / f"linked-{i}.bc")
        (evidence / "linked.asm").write_text(run(llvm("llvm-objdump"), "-d", binary).stdout)
        (evidence / "metadata.json").write_text(json.dumps({
            "target": TARGET, "mode": mode, "clang": run(llvm("clang"), "--version").stdout,
            "rust": run("rustc", "-vV").stdout,
            "sha256": {p.name: hashlib.sha256(p.read_bytes()).hexdigest()
                       for p in retained},
            "linker": linker, "flags": link_flags,
            "c_flags": ["-O1", f"-flto={mode}"], "rust_flags": rust_flags,
            "native_flags": ["-O2"],
        }, indent=2) + "\n")

    def test_fat_lto_objects_and_mixed_archives(self):
        # Exercise real ELF containers on every host; execute links on Linux.
        target = TARGET if sys.platform == "linux" else "x86_64-unknown-linux-gnu"
        generated = self.work / "elf-contracts"
        run(BINARY, "generate", ROOT / "examples/buffer.seam", "--out", generated, "--target", target)
        contracts = generated / "seam.ll"
        source = self.work / "fat.c"
        source.write_text("extern long long seam_inspect(const long long *);\n"
                          "__attribute__((noinline)) long long fat_probe(void) { "
                          "long long value = 42; seam_inspect(&value); return value; }\n")
        ordinary_source = self.work / "ordinary.c"
        ordinary_source.write_text(source.read_text().replace("fat_probe", "ordinary_probe"))
        native_source = self.work / "native.c"
        native_source.write_text("long long seam_inspect(const long long *p) { return *p; }\n")
        unrelated_source = self.work / "unrelated.c"
        unrelated_source.write_text("int unrelated(void) { return 7; }\n")
        native = self.work / "native.o"
        run(llvm("clang"), f"--target={target}", "-O1", "-c", native_source, "-o", native)
        native_bytes = native.read_bytes()
        main_c = self.work / "main.c"
        main_c.write_text("extern long long fat_probe(void); extern long long ordinary_probe(void);\n"
                          "int main(void) { return fat_probe() == 42 && ordinary_probe() == 42 ? 0 : 1; }\n")
        for mode in ("thin", "full"):
            with self.subTest(mode=mode):
                fat = self.work / f"fat-{mode}.o"
                flags = [f"--target={target}", "-O1", "-g", f"-flto={mode}"]
                run(llvm("clang"), *flags, "-ffat-lto-objects", "-c", source, "-o", fat)
                unrelated = self.work / f"unrelated-{mode}.o"
                run(llvm("clang"), *flags, "-ffat-lto-objects", "-c", unrelated_source, "-o", unrelated)
                ordinary = self.work / f"ordinary-{mode}.o"
                run(llvm("clang"), *flags, "-c", ordinary_source, "-o", ordinary)
                archive = self.work / f"mixed-{mode}.a"
                run(llvm("llvm-ar"), "rcs", archive, ordinary, fat, native, unrelated)
                original_bc = self.work / f"original-{mode}.bc"
                run(llvm("llvm-objcopy"), f"--dump-section=.llvm.lto={original_bc}", fat)
                self.assertIn("load i64", self.optimized(original_bc))
                annotated_fat = self.work / f"annotated-{fat.name}"
                annotated_archive = self.work / f"annotated-{archive.name}"
                for original, annotated in ((fat, annotated_fat), (archive, annotated_archive)):
                    before = original.read_bytes()
                    self.apply(original, annotated, contracts)
                    self.assertEqual(original.read_bytes(), before)
                    twice = self.work / f"twice-{annotated.name}"
                    self.apply(annotated, twice, contracts)
                    self.assertEqual(annotated.read_bytes(), twice.read_bytes())
                extracted = self.work / f"extracted-{mode}"
                extracted.mkdir()
                run(llvm("llvm-ar"), "x", annotated_archive, cwd=extracted)
                self.assertEqual((extracted / native.name).read_bytes(), native_bytes)
                self.assertEqual((extracted / unrelated.name).read_bytes(), unrelated.read_bytes())
                self.assertEqual((extracted / fat.name).read_bytes(), annotated_fat.read_bytes())
                self.assertIn("!myna.applied", self.ir(extracted / ordinary.name))
                self.assertEqual(run(llvm("llvm-ar"), "t", archive).stdout,
                                 run(llvm("llvm-ar"), "t", annotated_archive).stdout)
                annotated_bc = self.work / f"annotated-{mode}.bc"
                run(llvm("llvm-objcopy"), f"--dump-section=.llvm.lto={annotated_bc}", annotated_fat)
                optimized = self.optimized(annotated_bc)
                self.assertIn("!myna.applied", self.ir(annotated_bc))
                self.assertIn("ret i64 42", optimized)
                self.assertNotIn("load i64", optimized)
                for bc in (original_bc, annotated_bc):
                    self.assertEqual("<GLOBALVAL_SUMMARY_BLOCK " in
                                     run(llvm("llvm-bcanalyzer"), "-dump", bc).stdout, mode == "thin")
                # All native sections, symbols, relocations and debug payloads survive.
                fallbacks = []
                for i, obj in enumerate((fat, annotated_fat)):
                    fallback = self.work / f"fallback-{mode}-{i}.o"
                    run(llvm("llvm-objcopy"), "--remove-section=.llvm.lto", obj, fallback)
                    fallbacks.append(fallback.read_bytes())
                self.assertEqual(*fallbacks)
                evidence = ROOT / "build/reports/fatlto" / target / mode
                evidence.mkdir(parents=True, exist_ok=True)
                retained = [source, ordinary_source, native_source, main_c, contracts, fat, ordinary,
                            native, archive, annotated_fat, annotated_archive, original_bc, annotated_bc]
                if sys.platform == "linux":
                    # The pinned driver must select archive bitcode by default for LTO links.
                    binary = self.work / f"linked-{mode}"
                    run(llvm("clang"), f"-flto={mode}", "-fuse-ld=lld", "-O2", "-Wl,--save-temps",
                        main_c, annotated_archive, "-o", binary)
                    run(binary)
                    saved = list(self.work.glob("*.opt.bc"))
                    irs = [self.ir(path) for path in saved if "@fat_probe" in self.ir(path)]
                    self.assertTrue(irs, "linker must consume embedded fat-object IR")
                    self.assertTrue(any("!myna.applied" in ir for ir in irs))
                    bodies = [body for ir in irs for body in re.findall(
                        r"(?ms)^define [^\n]*@fat_probe[^(]*\([^\n]*\).*?^}", ir)]
                    self.assertTrue(bodies)
                    self.assertTrue(all("load i64" not in body for body in bodies))
                    for i, ir in enumerate(irs):
                        (evidence / f"linked-{i}.ll").write_text(ir)
                    (evidence / "linked.asm").write_text(run(llvm("llvm-objdump"), "-d", binary).stdout)
                    retained.extend([binary, *saved])
                    # The unchanged native fallback also remains linkable without FatLTO selection.
                    fallback_binary = self.work / f"native-linked-{mode}"
                    run(llvm("clang"), "-fuse-ld=lld", "-Wl,--no-fat-lto-objects",
                        main_c, annotated_archive, "-o", fallback_binary)
                    run(fallback_binary)
                for path in retained:
                    shutil.copyfile(path, evidence / path.name)
                (evidence / "optimized.ll").write_text(optimized)
                (evidence / "metadata.json").write_text(json.dumps({
                    "target": target, "mode": mode, "clang": run(llvm("clang"), "--version").stdout,
                    "compile_flags": [*flags, "-ffat-lto-objects"],
                    "sha256": {path.name: hashlib.sha256(path.read_bytes()).hexdigest() for path in retained},
                }, indent=2) + "\n")
                for path in self.work.glob("*.opt.bc"):
                    path.unlink()

    @unittest.skipUnless(sys.platform == "linux", "the shipped ELF sysroot runs on Linux")
    def test_shipped_fat_lto_sysroot_archive(self):
        original = Path(os.environ["ELIDE_TOOLCHAIN_HOME"]) / "sysroot" / TARGET / "usr/lib/libzstd.a"
        self.assertTrue(original.is_file(), f"the pinned toolchain must ship {original}")
        contracts = self.work / "zstd.ll"
        contracts.write_text(self.contracts.read_text() + "\ndeclare i32 @ZSTD_versionNumber() nounwind willreturn nofree\n")
        annotated = self.work / "libzstd.a"
        self.apply(original, annotated, contracts)
        # Ordinary archive lookup (not whole-archive) must find the rewritten member.
        source = self.work / "zstd.c"
        source.write_text("extern unsigned ZSTD_versionNumber(void);\n"
                          "int main(void) { return ZSTD_versionNumber() < 10000; }\n")
        binary = self.work / "zstd"
        run(llvm("clang"), "-flto=thin", "-fuse-ld=lld", "-O2", "-Wl,--save-temps",
            source, annotated, "-o", binary)
        run(binary)
        saved = list(self.work.glob("*.opt.bc"))
        self.assertTrue(any("!myna.applied" in self.ir(path) for path in saved),
                        "the linker must consume annotated sysroot bitcode")
        evidence = ROOT / "build/reports/fatlto" / TARGET / "sysroot"
        evidence.mkdir(parents=True, exist_ok=True)
        for path in [source, contracts, annotated, binary, *saved]:
            shutil.copyfile(path, evidence / path.name)
        (evidence / "metadata.json").write_text(json.dumps({
            "input": str(original), "input_sha256": hashlib.sha256(original.read_bytes()).hexdigest(),
            "output_sha256": hashlib.sha256(annotated.read_bytes()).hexdigest(),
            "clang": run(llvm("clang"), "--version").stdout,
        }, indent=2) + "\n")

    def test_fat_lto_invalid_payloads_fail_atomically(self):
        target = "x86_64-unknown-linux-gnu"
        generated = self.work / "elf-contracts"
        run(BINARY, "generate", ROOT / "examples/buffer.seam", "--out", generated, "--target", target)
        contracts = generated / "seam.ll"
        source = self.work / "fat.c"
        source.write_text("extern long long seam_inspect(const long long *);\n"
                          "long long probe(const long long *p) { return seam_inspect(p); }\n")
        fat = self.work / "fat.o"
        run(llvm("clang"), f"--target={target}", "-flto=thin", "-ffat-lto-objects", "-c", source, "-o", fat)
        annotated = self.work / "annotated.o"
        self.apply(fat, annotated, contracts)
        stale = self.work / "stale.ll"
        stale.write_text(contracts.read_text().replace("readonly", "writeonly"))
        malformed = self.work / "malformed.bc"
        malformed.write_bytes(b"not bitcode")
        corrupt = self.work / "corrupt.o"
        run(llvm("llvm-objcopy"), f"--update-section=.llvm.lto={malformed}", fat, corrupt)
        duplicate = self.work / "duplicate.o"
        run(llvm("llvm-objcopy"), "--rename-section=.comment=.llvm.lto", fat, duplicate)
        cases = [(annotated, stale, "different seam contracts"),
                 (corrupt, contracts, "invalid .llvm.lto"),
                 (duplicate, contracts, "duplicate .llvm.lto")]
        for i, (obj, contract, error) in enumerate(cases):
            archive = self.work / f"invalid-{i}.a"
            run(llvm("llvm-ar"), "rcs", archive, fat, obj)
            for original in (obj, archive):
                output = self.work / "existing-output.o"
                output.write_bytes(b"preserve previous output")
                result = self.apply(original, output, contract, ok=False)
                self.assertNotEqual(result.returncode, 0)
                self.assertIn(error, result.stdout + result.stderr)
                self.assertEqual(output.read_bytes(), b"preserve previous output")

    def test_rlib_members_and_native_archive_members(self):
        source = self.work / "lib.rs"
        source.write_text('''#![no_std]
unsafe extern "C" { fn seam_inspect(p: *const i64) -> i64; }
#[unsafe(no_mangle)] pub unsafe extern "C" fn forward(p: *const i64) -> i64 { unsafe { seam_inspect(p) } }
''')
        archive = self.work / "original.rlib"
        run("rustc", "--edition=2024", "--crate-type=rlib", "-Clinker-plugin-lto", "-Cpanic=abort", source, "-o", archive)
        native = self.work / "extra.c"
        native.write_text("int unrelated(void) { return 7; }\n")
        run(llvm("clang"), "-c", native, "-o", self.work / "extra.o")
        run(llvm("llvm-ar"), "r", archive, self.work / "extra.o")
        rewritten = self.work / "annotated.rlib"
        self.apply(archive, rewritten)
        names = run(llvm("llvm-ar"), "t", archive).stdout.splitlines()
        self.assertEqual(names, run(llvm("llvm-ar"), "t", rewritten).stdout.splitlines())
        for name in ("lib.rmeta", "extra.o"):
            # Binary payloads must survive exactly, including Rust metadata.
            a = self.work / "a"; b = self.work / "b"
            a.mkdir(exist_ok=True); b.mkdir(exist_ok=True)
            run(llvm("llvm-ar"), "x", archive, name, cwd=a)
            run(llvm("llvm-ar"), "x", rewritten, name, cwd=b)
            self.assertEqual((a / name).read_bytes(), (b / name).read_bytes())
        bitcode_name = next(n for n in names if n.endswith(".o") and n != "extra.o")
        run(llvm("llvm-ar"), "x", rewritten, bitcode_name, cwd=self.work)
        self.assertIn("!myna.applied", self.ir(self.work / bitcode_name))
        reapply = self.work / "twice.rlib"
        self.apply(rewritten, reapply)
        self.assertEqual(rewritten.read_bytes(), reapply.read_bytes())

    def test_rejects_objects_annotated_before_project_rename(self):
        original = self.c_object()
        annotated = self.work / "annotated.o"
        self.apply(original, annotated)
        legacy = self.work / "legacy.ll"
        legacy.write_text(self.ir(annotated).replace("myna.applied", "svmgen.applied"))
        run(llvm("llvm-as"), legacy, "-o", self.work / "legacy.o")
        output = self.work / "reapplied.o"
        result = self.apply(self.work / "legacy.o", output, ok=False)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("rebuild from original compiler output", result.stdout)
        self.assertFalse(output.exists())

    def test_rejects_hidden_abi_attributes_and_changed_contract_content(self):
        original = self.c_object()
        annotated = self.work / "annotated.o"
        self.apply(original, annotated)
        # Editing a contract without updating its descriptor fingerprint must still invalidate it.
        edited = self.work / "edited.ll"
        edited.write_text(self.contracts.read_text().replace("readonly", "writeonly"))
        self.assertIn("different seam contracts", self.apply(annotated, self.work / "again.o", edited, ok=False).stdout)
        abi = self.work / "abi.ll"
        abi.write_text(f'target triple = "{TARGET}"\ndeclare i64 @seam_inspect(ptr byval(i64))\n')
        run(llvm("llvm-as"), abi, "-o", self.work / "abi.o")
        self.assertIn("unsupported ABI attribute", self.apply(self.work / "abi.o", self.work / "bad.o", ok=False).stdout)
        wrong_target = self.work / "wrong-target.ll"
        other = "x86_64-unknown-linux-gnu" if TARGET != "x86_64-unknown-linux-gnu" else "aarch64-apple-darwin"
        wrong_target.write_text(self.contracts.read_text().replace(TARGET, other))
        self.assertIn("target mismatch", self.apply(original, self.work / "bad.o", wrong_target, ok=False).stdout)

    def test_malformed_contracts_conflicting_facts_and_output_alias(self):
        original = self.c_object()
        output = self.work / "rejected.o"
        contract = self.contracts.read_text()
        mutations = [
            (re.sub(r'!myna\.contract = !\{!0\}', '', contract), "fingerprint"),
            (contract + "\n@unexpected = global i32 0\n", "function declarations"),
            (contract + "\ndefine i32 @unexpected() { ret i32 0 }\n", "unsupported contract function"),
            (contract + "\ndeclare void @unexpected(i64 signext)\n", "non-narrow"),
        ]
        for i, (text, expected) in enumerate(mutations):
            with self.subTest(expected=expected):
                path = self.work / f"malformed-{i}.ll"
                path.write_text(text)
                output.unlink(missing_ok=True)
                result = self.apply(original, output, path, ok=False)
                self.assertNotEqual(result.returncode, 0)
                self.assertIn(expected, result.stdout + result.stderr)
                self.assertFalse(output.exists())
        before = original.read_bytes()
        alias = self.work / "alias.o"
        alias.hardlink_to(original)
        result = self.apply(original, alias, ok=False)
        self.assertIn("same file", result.stdout + result.stderr)
        self.assertEqual(before, original.read_bytes())
        for attribute in ("writeonly", "readnone"):
            path = self.work / f"{attribute}.ll"
            path.write_text(f'target triple = "{TARGET}"\ndeclare i64 @seam_inspect(ptr {attribute})\n')
            obj = path.with_suffix(".o")
            run(llvm("llvm-as"), path, "-o", obj)
            result = self.apply(obj, output, ok=False)
            if attribute == "writeonly":
                self.assertNotEqual(result.returncode, 0)
                self.assertIn("contradict", result.stdout + result.stderr)
            else:
                self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
                self.assertIn("readnone", self.ir(output))
                output.unlink()

    def test_contract_validation_surface_is_fail_closed(self):
        original = self.c_object()
        prefix = f'target triple = "{TARGET}"\n'
        metadata = '!myna.contract = !{!0}\n!0 = !{!"' + 'a' * 64 + '"}\n'
        cases = [
            ("", metadata, "target triple"),
            ("declare void @f(...)\n", prefix + metadata, "unsupported contract function"),
            ("declare fastcc void @f()\n", prefix + metadata, "unsupported contract function"),
            ("declare void @llvm.donothing()\n", prefix + metadata, "unsupported contract function"),
            ("declare {i32, i32} @f()\n", prefix + metadata, "unsupported return ABI"),
            ("declare nonnull ptr @f()\n", prefix + metadata, "return contracts"),
            ("declare void @f({i32, i32})\n", prefix + metadata, "unsupported parameter ABI"),
            ("declare void @f(ptr addrspace(1))\n", prefix + metadata, "unsupported parameter ABI"),
            ('declare void @f() "arbitrary"\n', prefix + metadata, "string attributes"),
            ('declare void @f(ptr "arbitrary")\n', prefix + metadata, "string attributes"),
            ("declare void @f() noinline\n", prefix + metadata, "unsupported seam attribute"),
            ("declare void @f(ptr noundef)\n", prefix + metadata, "unsupported seam attribute"),
            ("declare void @f(i64 nonnull)\n", prefix + metadata, "invalid contract IR"),
        ]
        for bad in ['!myna.contract = !{}', '!myna.contract = !{!0}\n!0 = !{}',
                    '!myna.contract = !{!0}\n!0 = !{i64 1}',
                    '!myna.contract = !{!0}\n!0 = !{!"short"}',
                    *['!myna.contract = !{!0}\n!0 = !{!"' + c * 64 + '"}' for c in ('/', ':', 'G', 'z')]]:
            cases.append(("declare void @f()\n", prefix + bad + "\n", "fingerprint"))
        for i, (declaration, rest, message) in enumerate(cases):
            with self.subTest(case=i):
                contract = self.work / f"invalid-{i}.ll"
                contract.write_text(prefix + declaration + rest[len(prefix):] if rest.startswith(prefix) else declaration + rest)
                output = self.work / f"invalid-{i}.o"
                result = self.apply(original, output, contract, ok=False)
                self.assertNotEqual(result.returncode, 0)
                self.assertIn(message, result.stdout + result.stderr)
                self.assertFalse(output.exists())
        for i, (body, expected) in enumerate([
            ('target datalayout = "e-p:32:32"\ndeclare i64 @seam_inspect(ptr)\n', "64-bit pointers"),
            ('declare i64 @seam_inspect(ptr)\ndefine i64 @caller(ptr %p) { %r = call fastcc i64 @seam_inspect(ptr %p)\n ret i64 %r }\n', "direct call ABI mismatch"),
        ]):
            source = self.work / f"abi-{i}.ll"
            source.write_text(prefix + body)
            obj = source.with_suffix(".o")
            run(llvm("llvm-as"), source, "-o", obj)
            result = self.apply(obj, self.work / f"abi-output-{i}.o", ok=False)
            self.assertIn(expected, result.stdout + result.stderr)

    def test_merges_compiler_facts_to_the_stronger_one(self):
        # rustc/LLVM may already infer weaker or stronger versions of contracted facts; both hold.
        source = self.work / "inferred.ll"
        source.write_text(f'target triple = "{TARGET}"\n'
                          'declare i64 @seam_inspect(ptr align 16 captures(address) dereferenceable(64))\n'
                          'define i64 @probe(ptr %p) {\n  %r = call i64 @seam_inspect(ptr %p)\n  ret i64 %r\n}\n')
        run(llvm("llvm-as"), source, "-o", self.work / "inferred.o")
        self.apply(self.work / "inferred.o", self.work / "merged.o")
        decl = next(l for l in self.ir(self.work / "merged.o").splitlines() if l.startswith("declare i64 @seam_inspect"))
        for fact in ("align 16", "captures(none)", "dereferenceable(64)", "nonnull", "readonly"):
            self.assertIn(fact, decl)
        self.assertNotIn("captures(address)", decl)

    def test_small_integer_extension_is_verified_not_added(self):
        source = self.work / "small.rs"
        source.write_text("""#![crate_type = "rlib"]
unsafe extern "C" { fn seam_widen(a: i8, b: u8, c: i16, d: u16, e: bool) -> i32; }
#[unsafe(no_mangle)] pub fn widen_probe() -> i32 { unsafe { seam_widen(-1, 255, -2, 65535, true) } }
""")
        rlib = self.work / "libsmall.rlib"
        run("rustc", "--edition=2024", "-O", "-Clinker-plugin-lto", "--target", TARGET, source, "-o", rlib)
        self.assertIn("1 direct calls", self.apply(rlib, self.work / "small.seam.rlib").stdout)
        if TARGET == "aarch64-unknown-linux-gnu":
            return  # AAPCS64 Linux carries no extension, so signedness is invisible here.
        flipped = self.work / "flipped.ll"
        flipped.write_text(self.contracts.read_text().replace("i8 signext %signedByte", "i8 zeroext %signedByte"))
        result = self.apply(rlib, self.work / "bad.rlib", flipped, ok=False)
        self.assertIn("ABI extension mismatch", result.stdout + result.stderr)

    def test_by_value_wrappers_call_annotated_reference_forms(self):
        source = self.work / "byvalue.rs"
        source.write_text("""#![crate_type = "rlib"]
#[allow(dead_code, non_snake_case)]
mod seam { include!(concat!(env!("SEAM_GENERATED"), "/seam.rs")); }
#[unsafe(no_mangle)]
pub fn scale_probe(thread: *mut core::ffi::c_void) -> i64 {
    let pair = seam::Pair { left: 3, right: 4, seamPadding0: [0; 4] };
    unsafe { seam::scale(thread, pair, 10).left }
}
""")
        rlib = self.work / "libbyvalue.rlib"
        os.environ["SEAM_GENERATED"] = str(self.work / "generated")
        run("rustc", "--edition=2024", "-O", "-Clinker-plugin-lto", "--target", TARGET, source, "-o", rlib)
        self.assertIn("1 direct calls", self.apply(rlib, self.work / "byvalue.seam.rlib").stdout)
        members = self.work / "members"
        members.mkdir()
        run(llvm("llvm-ar"), "x", self.work / "byvalue.seam.rlib", cwd=members)
        ir = "".join(self.ir(m) for m in members.iterdir() if m.suffix == ".o")
        call = next(l for l in ir.splitlines() if "call" in l and "@seam_scale_myna_ref" in l)
        for fact in ("readonly", "writeonly", "captures(none)", "dereferenceable(16)", "align 8"):
            self.assertIn(fact, call)
        self.assertNotIn("declare i64 @seam_swap_pair", self.contracts.read_text())

    def test_rejects_wrong_abi_stale_contracts_and_native_objects(self):
        original = self.c_object()
        annotated = self.work / "annotated.o"
        self.apply(original, annotated)
        wrong = self.work / "wrong.ll"
        wrong.write_text(self.contracts.read_text().replace("declare i64 @seam_inspect", "declare i32 @seam_inspect"))
        output = self.work / "bad.o"
        self.assertNotEqual(self.apply(original, output, wrong, ok=False).returncode, 0)
        self.assertFalse(output.exists())
        stale = self.work / "stale.ll"
        stale.write_text(re.sub(r'!0 = !\{!"[0-9a-f]{64}"\}', '!0 = !{!"' + '0' * 64 + '"}', self.contracts.read_text()))
        self.assertIn("different seam contracts", self.apply(annotated, output, stale, ok=False).stdout)
        self.assertNotEqual(self.apply(original, original, ok=False).returncode, 0)
        native = self.work / "native.o"
        run(llvm("clang"), "-c", self.work / "caller.c", "-o", native)
        self.assertIn("no LLVM bitcode", self.apply(native, output, ok=False).stdout)
        empty = self.work / "empty.ll"
        empty.write_text(self.contracts.read_text().replace("@seam_inspect", "@not_present"))
        self.assertIn("no contract symbols", self.apply(original, output, empty, ok=False).stdout)

if __name__ == "__main__": main(sys.modules[__name__], "llvm-integration")
