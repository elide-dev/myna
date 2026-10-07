"""Prove actual object/rlib changes, optimization effects, and final ThinLTO linkage."""
import hashlib
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
    def c_object(self):
        source = self.work / "caller.c"
        source.write_text('''#include <stdint.h>
extern int64_t seam_inspect(const int64_t *);
int64_t c_probe(void) { int64_t value = 42; (void)seam_inspect(&value); return value; }
''')
        output = self.work / "caller.o"
        run(llvm("clang"), "-O1", "-flto=thin", "-c", source, "-o", output)
        return output
    def optimized(self, path):
        return run(llvm("opt"), "-passes=default<O2>", "-S", path, "-o", "-").stdout
    def test_real_c_and_rust_objects_optimize_and_link(self):
        original = self.c_object()
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
pub extern "C" fn rust_probe() -> i64 {
 let mut value: i64 = 42;
 unsafe { seam_inspect(&raw mut value); }
 value
}
''')
        self.assertRegex(run("rustc", "-vV").stdout, r"LLVM version: 23\.")
        rust_obj = self.work / "rust.o"
        run("rustc", "--edition=2024", "--crate-type=lib", "--emit=obj", "-Copt-level=1", "-Cpanic=abort", "-Clinker-plugin-lto", rust, "-o", rust_obj)
        rust_annotated = self.work / "rust.seam.o"
        self.apply(rust_obj, rust_annotated)
        self.assertIn("load i64", self.optimized(rust_obj))
        self.assertNotIn("load i64", self.optimized(rust_annotated))
        self.assertIn("ret i64 42", self.optimized(rust_annotated))
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
        run(llvm("clang"), f"-fuse-ld={linker}", "-flto=thin", "-O2", save, main_c, annotated, rust_annotated, native_obj, "-o", binary)
        run(binary)
        self.assertEqual(native_hash, hashlib.sha256(native_obj.read_bytes()).hexdigest())
        saved = list(self.work.glob("*.opt.bc"))
        self.assertTrue(saved, "LLD must save optimized ThinLTO bitcode")
        caller_irs = [self.ir(p) for p in saved if "@c_probe" in self.ir(p) or "@rust_probe" in self.ir(p)]
        self.assertTrue(any("captures(none)" in text for text in caller_irs), "attributes survive into ThinLTO backend")
        self.assertTrue(any("ret i64 42" in text for text in caller_irs))

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
        self.assertIn("no standalone LLVM bitcode", self.apply(native, output, ok=False).stdout)
        empty = self.work / "empty.ll"
        empty.write_text(self.contracts.read_text().replace("@seam_inspect", "@not_present"))
        self.assertIn("no contract symbols", self.apply(original, output, empty, ok=False).stdout)

if __name__ == "__main__": main(sys.modules[__name__], "llvm-integration")
