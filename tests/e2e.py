"""Run against the built native CLI, never an in-process generator."""
import json
import os
from pathlib import Path
import sys
import tempfile
import unittest
from support import BINARY, ROOT, TARGET, main, run

class NativeCliTests(unittest.TestCase):
    def test_callback_records_are_rejected(self):
        with tempfile.TemporaryDirectory(prefix="myna-callback-record-") as tmp:
            work = Path(tmp)
            for signature in ("callback RecordCallback return=Record\nend\n",
                              "callback RecordCallback return=i64\n  param value type=Record\nend\n"):
                with self.subTest(signature=signature):
                    source = work / "records.seam"
                    source.write_text(f"module callback_records target={TARGET}\n"
                        "struct Record size=8 align=8\n  field value type=i64 offset=0\nend\n" + signature)
                    rejected = run(BINARY, "generate", source, "--out", work / "generated", ok=False)
                    self.assertNotEqual(rejected.returncode, 0)
                    self.assertIn("by-value records are function parameters and returns only; use ptr<T>",
                                  rejected.stdout + rejected.stderr)

    def test_jni_wrapper_locals_do_not_shadow_native_symbols(self):
        with tempfile.TemporaryDirectory(prefix="myna-jni-names-") as tmp:
            work = Path(tmp)
            definitions = [f"module jni_names target={TARGET}\n"]
            for symbol in ("env", "clazz", "arg0", "arg1", "pointer", "length", "empty", "error"):
                name = "call" + symbol.capitalize()
                definitions.append(f"""borrow {name} bytes length=count lifetime=call
import {name} symbol={symbol} return=u64 error=no_failure nounwind=true@explicit_contract native_leaf=false@explicit_contract
  param bytes type=ptr<u8> nullable=false ownership=borrowed access=read nocapture=true@explicit_contract readonly=true@explicit_contract
  param count type=u64
end
""")
            definitions.append("""struct Record size=8 align=8
  field value type=i64 offset=0
end
import recordResult symbol=result return=Record error=no_failure
  param value type=Record
end
""")
            source = work / "names.seam"
            source.write_text("".join(definitions))
            generated = work / "generated"
            run(BINARY, "generate", source, "--jni", "--out", generated)
            home = Path(os.environ["GRAALVM_HOME"])
            run("cc", "-std=c11", "-Wall", "-Werror", "-fsyntax-only", "-I", home / "include",
                "-I", home / "include" / ("darwin" if sys.platform == "darwin" else "linux"),
                generated / "seam-jni.c")
            run("javac", "-d", work / "classes", generated / "MynaFFM.java", generated / "MynaNativeJNI.java")

    def test_record_allocator_parameter_collision(self):
        with tempfile.TemporaryDirectory() as tmp:
            work = Path(tmp)
            source = work / "collision.seam"
            source.write_text(f"""module collision target={TARGET}
struct Record size=8 align=8
  field value type=i64 offset=0
end
import transform return=Record error=no_failure
  param allocator type=Record
  param allocator_ type=i32
end
""")
            run(BINARY, "generate", source, "--out", work / "generated")
            run("javac", "-d", work / "classes", work / "generated/MynaFFM.java")

    def test_scoped_ffm_callbacks(self):
        from callback_fixture import build_callback_fixture
        _, classes, library = build_callback_fixture()
        self.assertIn("Scoped callbacks, native threads and exception status passed",
                      run("java", "--enable-native-access=ALL-UNNAMED", "-cp", classes,
                          "fixture.callbacks.CallbackMain", library).stdout)

    def test_explicit_ffm_library_binding(self):
        from ffm_binding import build_binding_fixture
        _, classes, library = build_binding_fixture()
        self.assertIn("Explicit FFM library lifetime and buffer borrowing passed",
                      run("java", "--enable-native-access=ALL-UNNAMED", "-cp", classes,
                          "FfmBindingMain", library).stdout)

    def test_generate_compile_and_check(self):
        self.assertTrue(BINARY.is_file(), "run scripts/build.sh first")
        self.assertEqual(run(BINARY, "--version").stdout.strip(), "0.9.1")
        with tempfile.TemporaryDirectory(prefix="myna e2e ") as tmp:
            out = Path(tmp)
            run(BINARY, "generate", ROOT / "examples/buffer.seam", "--jni", "--out", out, "--target", TARGET)
            run(BINARY, "generate", ROOT / "examples/buffer.seam", "--out", out, "--target", TARGET, "--check")
            descriptor = json.loads((out / "seam.json").read_text())
            self.assertEqual(descriptor["targetTriple"], TARGET)
            self.assertEqual(descriptor["functions"][1]["abiParameters"][0]["name"], "isolate_thread")
            (out / "check.c").write_text('#include "seam.h"\nint main(void) { return sizeof(struct BufferView) != 24; }\n')
            run("cc", "-std=c11", "-Wall", "-Werror", out / "check.c", "-o", out / "check")
            run(out / "check")
            run("c++", "-std=c++17", "-x", "c++", "-fsyntax-only", out / "check.c")
            run("rustc", "--edition=2024", "--crate-type=lib", out / "seam.rs", "-o", out / "libseam.rlib")
            run("javac", "-d", out / "classes", out / "MynaFFM.java", out / "MynaNativeJNI.java", ROOT / "tests/abi/FfmMain.java")
            library = out / ("libinspect.dylib" if sys.platform == "darwin" else "libinspect.so")
            os.environ["SEAM_GENERATED"] = str(out)
            run("rustc", "--edition=2024", "--crate-type=cdylib", "-C", "opt-level=2", "--cfg", 'feature="std"', ROOT / "tests/abi/inspect.rs", "-o", library)
            (out / "FfmCheck.java").write_text('''import dev.elide.myna.generated.MynaFFM;
import java.lang.foreign.*;
import java.nio.file.Path;
public class FfmCheck { public static void main(String[] args) throws Throwable {
 if (MynaFFM.BufferView_LAYOUT.byteSize() != 24 || MynaFFM.BufferView_flags_OFFSET != 16) throw new AssertionError();
 if (MynaFFM.sum_DESCRIPTOR.argumentLayouts().size() != 3) throw new AssertionError();
 try (var arena = Arena.ofConfined()) {
   var lookup = SymbolLookup.libraryLookup(Path.of(args[0]), arena);
   var value = arena.allocate(ValueLayout.JAVA_LONG);
   value.set(ValueLayout.JAVA_LONG, 0, 42L);
   long result = (long) MynaFFM.inspect(lookup).invokeExact(value);
   if (result != 42L) throw new AssertionError("FFM -> Rust result");
    System.load(Path.of(args[0]).toAbsolutePath().toString());
    for (int i = 0; i < 100; i++) {
      if (MynaFFM.inspect(value) != 42L) throw new AssertionError("typed FFM -> Rust result");
    }
    if (MynaFFM.widen((byte) 0, (byte) 200, (short) 0, (short) 0, false) != 200) throw new AssertionError("ffm u8 zero-extend");
    if (MynaFFM.widen((byte) 0, (byte) 0, (short) 0, (short) 0xFFFF, false) != 65535) throw new AssertionError("ffm u16 zero-extend");
    if (MynaFFM.widen((byte) -1, (byte) 255, (short) -2, (short) 65535, true) != 66787) throw new AssertionError("ffm narrow aggregate");
  }
 }
}''')
            run("javac", "-cp", out / "classes", "-d", out / "classes", out / "FfmCheck.java")
            run("java", "--enable-native-access=ALL-UNNAMED", "-cp", out / "classes", "FfmCheck", library)
            run("java", "--enable-native-access=ALL-UNNAMED", "-cp", out / "classes", "FfmMain", library)
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
