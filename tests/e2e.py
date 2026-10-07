"""Run against the built native CLI, never an in-process generator."""
import json
from pathlib import Path
import sys
import tempfile
import unittest
from support import BINARY, ROOT, TARGET, main, run

class NativeCliTests(unittest.TestCase):
    def test_generate_compile_and_check(self):
        self.assertTrue(BINARY.is_file(), "run scripts/build.sh first")
        self.assertEqual(run(BINARY, "--version").stdout.strip(), "0.3.0")
        with tempfile.TemporaryDirectory(prefix="svmgen e2e ") as tmp:
            out = Path(tmp)
            run(BINARY, "generate", ROOT / "examples/buffer.seam", "--out", out, "--target", TARGET)
            run(BINARY, "generate", ROOT / "examples/buffer.seam", "--out", out, "--target", TARGET, "--check")
            descriptor = json.loads((out / "seam.json").read_text())
            self.assertEqual(descriptor["targetTriple"], TARGET)
            self.assertEqual(descriptor["functions"][1]["abiParameters"][0]["name"], "isolate_thread")
            (out / "check.c").write_text('#include "seam.h"\nint main(void) { return sizeof(struct BufferView) != 24; }\n')
            run("cc", "-std=c11", "-Wall", "-Werror", out / "check.c", "-o", out / "check")
            run(out / "check")
            run("c++", "-std=c++17", "-x", "c++", "-fsyntax-only", out / "check.c")
            run("rustc", "--edition=2024", "--crate-type=lib", out / "seam.rs", "-o", out / "libseam.rlib")
            run("javac", "-d", out / "classes", out / "SeamFFM.java")
            library = out / ("libinspect.dylib" if sys.platform == "darwin" else "libinspect.so")
            run("rustc", "--edition=2024", "--crate-type=cdylib", "--cfg", 'feature="std"', ROOT / "tests/abi/inspect.rs", "-o", library)
            (out / "FfmCheck.java").write_text('''import dev.elide.seam.generated.SeamFFM;
import java.lang.foreign.*;
import java.nio.file.Path;
public class FfmCheck { public static void main(String[] args) throws Throwable {
 if (SeamFFM.BufferView_LAYOUT.byteSize() != 24 || SeamFFM.BufferView_flags_OFFSET != 16) throw new AssertionError();
 if (SeamFFM.sum_DESCRIPTOR.argumentLayouts().size() != 3) throw new AssertionError();
 try (var arena = Arena.ofConfined()) {
   var lookup = SymbolLookup.libraryLookup(Path.of(args[0]), arena);
   var value = arena.allocate(ValueLayout.JAVA_LONG);
   value.set(ValueLayout.JAVA_LONG, 0, 42L);
   long result = (long) SeamFFM.inspect(lookup).invokeExact(value);
   if (result != 42L) throw new AssertionError("FFM -> Rust result");
   System.load(Path.of(args[0]).toAbsolutePath().toString());
   for (int i = 0; i < 100; i++) {
     if (SeamFFM.inspect(value) != 42L) throw new AssertionError("typed FFM -> Rust result");
   }
 }
}}''')
            run("javac", "-cp", out / "classes", "-d", out / "classes", out / "FfmCheck.java")
            run("java", "--enable-native-access=ALL-UNNAMED", "-cp", out / "classes", "FfmCheck", library)
            (out / "seam.h").write_text("drift")
            result = run(BINARY, "generate", ROOT / "examples/buffer.seam", "--out", out, "--target", TARGET, "--check", ok=False)
            self.assertEqual(result.returncode, 1)
            self.assertEqual((out / "seam.h").read_text(), "drift")

    def test_bad_contract_fails_without_output(self):
        with tempfile.TemporaryDirectory() as tmp:
            tmp = Path(tmp)
            source = tmp / "bad.seam"
            source.write_text(f"module bad target={TARGET}\nimport f return=void error=no_failure\nparam p type=ptr<i64> noalias=true@heuristic\nend\n")
            result = run(BINARY, "generate", source, "--out", tmp / "out", "--relaxed", ok=False)
            self.assertEqual(result.returncode, 2)
            self.assertIn("noalias", result.stderr)
            self.assertFalse((tmp / "out").exists())

if __name__ == "__main__": main(sys.modules[__name__], "native-e2e")
