# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Build and execute real Native Image C API glue and the hosted Feature."""
import os
import re
import shutil
from pathlib import Path
import sys
import unittest
from values_cpp import build_cpp
from support import BINARY, ROOT, TARGET, main, native_image, run, compile_native
LLVM = Path(os.environ.get("LLVM_BIN", "/opt/homebrew/opt/llvm/bin" if sys.platform == "darwin" else "/usr/lib/llvm-23/bin"))

HEADER = ROOT / "tests/header"


def disassembly(app, prefix, method, evidence):
    """The machine code of one measured method, without trailing alignment padding."""
    symbols = run(LLVM / "llvm-nm", app).stdout.splitlines()
    name = next(line.split()[-1] for line in symbols if f"{prefix}_{method}_" in line)
    listing = run(LLVM / "llvm-objdump", "--disassemble", f"--disassemble-symbols={name}",
                  "--no-show-raw-insn", "--no-leading-addr", app).stdout
    evidence.mkdir(parents=True, exist_ok=True)
    (evidence / f"{method}.disassembly.txt").write_text(listing)
    lines = listing.split(f"<{name}>:", 1)[1].strip().splitlines()
    code = [" ".join(line.split()) for line in lines if line.strip()]
    while code and code[-1].startswith(("int3", "nop", "udf")):
        code.pop()  # alignment padding
    return code


class NativeImageIntegrationTests(unittest.TestCase):
    def test_scoped_ffm_callbacks_and_static_jni(self):
        from callback_fixture import build_callback_fixture
        work, classes, library = build_callback_fixture()
        native_image("-O1", "--enable-native-access=ALL-UNNAMED", "-cp", classes,
                     "--features=fixture.callbacks.CallbackApiForeignFeature",
                     "-H:+UnlockExperimentalVMOptions", f"-H:CLibraryPath={work}",
                     "-H:-UnlockExperimentalVMOptions", "fixture.callbacks.CallbackMain", "-o", work / "callback-app")
        self.assertIn("Scoped callbacks, native threads and exception status passed",
                      run(work / "callback-app", library, "jni").stdout)

    def test_static_jni_archive(self):
        work = ROOT / "build/static-jni-test"
        work.mkdir(parents=True, exist_ok=True)
        generated = work / "generated"
        run(BINARY, "generate", ROOT / "tests/ownership/ownership.seam", "--jni", "--out", generated, "--target", TARGET)
        run("rustc", "--edition=2024", "--crate-type=lib", generated / "seam-jni.rs", "-o", work / "libjni_bindings.rlib")
        obj = work / "ownership.o"
        run("rustc", "--edition=2024", "--crate-type=lib", "--emit=obj", "-Cpanic=abort",
            "-Copt-level=2", "-Crelocation-model=pic", ROOT / "tests/ownership/native.rs", "-o", obj)
        home = Path(os.environ["GRAALVM_HOME"])
        wrapper = work / "jni.bc"
        run("cc", "-O2", "-flto=thin", "-fPIC", "-I", home / "include", "-I",
            home / "include" / ("darwin" if sys.platform == "darwin" else "linux"),
            "-c", generated / "seam-jni.c", "-o", wrapper)
        annotated = work / "jni.annotated.bc"
        run(BINARY, "apply", "--contracts", generated / "seam-jni.ll", "--input", wrapper,
            "--output", annotated, "--llvm-tool", ROOT / "build/dist/myna-llvm")
        ir = run(LLVM / "llvm-dis", annotated, "-o", "-").stdout
        (work / "jni.annotated.ll").write_text(ir)
        self.assertIn("captures(none)", ir)
        self.assertIn("Java_fixture_owned_NativeApiJNI_checksum", ir)
        probe = work / "probe.c"
        probe.write_text('#include "seam.h"\nuint8_t contract_probe(void) { uint8_t value = 42; '
                         '(void)buffer_checksum(&value, 1); return value; }\n')
        run("cc", "-O1", "-flto=thin", "-I", generated, "-c", probe, "-o", work / "probe.bc")
        run(BINARY, "apply", "--contracts", generated / "seam-jni.ll", "--input", work / "probe.bc",
            "--output", work / "probe.annotated.bc", "--llvm-tool", ROOT / "build/dist/myna-llvm")
        before = run(LLVM / "opt", "-passes=default<O2>", "-S", work / "probe.bc", "-o", "-").stdout
        after = run(LLVM / "opt", "-passes=default<O2>", "-S", work / "probe.annotated.bc", "-o", "-").stdout
        (work / "probe.before.ll").write_text(before)
        (work / "probe.after.ll").write_text(after)
        self.assertIn("load i8", before)
        self.assertNotIn("load i8", after)
        self.assertIn("ret i8 42", after)
        run("cc", "-O2", "-flto=thin", "-I", generated, "-I", home / "include", "-I",
            home / "include" / ("darwin" if sys.platform == "darwin" else "linux"),
            "-c", ROOT / "tests/ownership/jni_checks.c", "-o", work / "checks.bc")
        run("ar", "rcs", work / "libownership_demo_jni.a", annotated, obj, work / "checks.bc")
        (work / "libownership_demo_jni.a.symbols").write_text(
            (generated / "libownership_demo_jni.a.symbols").read_text() + "Java_JniMain_nativeErrors\n")
        classes = work / "classes"
        # The generated properties register the context's header check: a JNI-only image compiles its
        # class too, and with no Native Image class in the image it has nothing to check.
        compile_native(classes, generated, generated / "NativeApiJNI.java", generated / "NativeApiDirectives.java",
                       ROOT / "tests/ownership/JniMain.java")
        native_image("-O1", "-cp", classes, "-H:+UnlockExperimentalVMOptions", f"-H:CLibraryPath={work}",
                     "-H:-UnlockExperimentalVMOptions", "JniMain", "-o", work / "jni-app")
        self.assertIn("Static JNI scalar, ownership and direct buffer calls passed", run(work / "jni-app").stdout)
        (work / "jni-app.disassembly.txt").write_text(run(LLVM / "llvm-objdump", "--disassemble", work / "jni-app").stdout)

    def test_explicit_ffm_library_binding(self):
        from ffm_binding import build_binding_fixture
        work, classes, library = build_binding_fixture()
        native_image("-O1", "--enable-native-access=ALL-UNNAMED", "-cp", classes,
                     "--features=fixture.owned.NativeApiForeignFeature,fixture.owned.ConfinedApiForeignFeature", "FfmBindingMain",
                     "-o", work / "binding-app")
        self.assertIn("Explicit FFM library lifetime and buffer borrowing passed",
                      run(work / "binding-app", library).stdout)

    def test_binding_abi2_default_leaf_static_linkage_and_gc(self):
        work = ROOT / "build/fast-test"
        fixture = ROOT / "tests/fast"
        generated = work / "generated"
        classes = work / "classes"
        shutil.rmtree(work, ignore_errors=True)
        work.mkdir(parents=True)
        run(BINARY, "generate", fixture / "fast.seam", "--out", generated, "--target", TARGET)
        run("cc", "-O3", "-fPIC", "-Wall", "-Werror", "-Wmissing-prototypes", "-I", generated,
            "-c", fixture / "native.c", "-o", work / "native.o")
        run("ar", "rcs", work / "libfastfixture.a", work / "native.o")
        compile_native(classes, generated, generated / "FastApi.java", generated / "FastApiDirectives.java",
                       *sorted(fixture.glob("*.java")))
        app = work / "fast-app"
        # Configuration travels with the compiled classes. No manual symbol/export/init flags.
        native_image("-O2", "-cp", classes, f"-Dmyna.fast.libraryPath={work}", "fixture.fast.FastMain", "-o", app)
        self.assertIn("Binding ABI 2 default leaf, static linkage, lazy symbols and GC passed", run(app).stdout)

    def test_serde_value_tape(self):
        work = ROOT / "build/values-test"
        generated = work / "generated"
        fixture = ROOT / "tests/values"
        work.mkdir(parents=True, exist_ok=True)
        # The packaged descriptor as consumers get it: the generated context supplies seam.h.
        run(BINARY, "generate", ROOT / "examples/values.seam", "--out", generated, "--target", TARGET)
        run("cargo", "build", "--locked", "--release", "-p", "myna-values-fixture")
        classes = work / "classes"
        if classes.exists():
            shutil.rmtree(classes)
        classes.mkdir(parents=True)
        runtime = ROOT / "src/values/java/dev/elide/myna/values"
        compile_native(classes, generated, generated / "ValueApi.java", generated / "ValueApiDirectives.java",
            *sorted(runtime.glob("*.java")), fixture / "ValuesMain.java")
        archive = ROOT / "target/release/libmyna_values_fixture.a"
        objects = build_cpp(work)
        linker = [f"-H:NativeLinkerOption={archive}", *[f"-H:NativeLinkerOption={obj}" for obj in objects]]
        if sys.platform != "darwin":
            linker += ["-H:NativeLinkerOption=-ldl", "-H:NativeLinkerOption=-lpthread", "-H:NativeLinkerOption=-lm", "-H:NativeLinkerOption=-lc++"]
        else:
            linker += ["-H:NativeLinkerOption=-lc++"]
        app = work / "values-app"
        native_image("-O2", "-cp", classes,
            "-H:+UnlockExperimentalVMOptions", *linker, "-H:-UnlockExperimentalVMOptions",
            "fixture.values.ValuesMain", "-o", app)
        self.assertIn("Serde Rust -> SVM value mapping passed", run(app).stdout)
        # Compile an actual Truffle call target; plain SVM calls cannot catch missing
        # deoptimization targets caused by analysis-time trivial inlining.
        import hashlib
        import json
        deps = []
        for item in json.loads((fixture / "truffle-deps.json").read_text()):
            jar = ROOT / "build/truffle-deps" / item["url"].rsplit("/", 1)[1]
            jar.parent.mkdir(parents=True, exist_ok=True)
            if not jar.exists() or hashlib.sha256(jar.read_bytes()).hexdigest() != item["sha256"]:
                run("curl", "-fsSL", item["url"], "-o", jar)
            if hashlib.sha256(jar.read_bytes()).hexdigest() != item["sha256"]:
                raise AssertionError("Truffle dependency checksum mismatch")
            deps.append(jar)
        compile_native(classes, generated, fixture / "TruffleValuesMain.java", classpath=[classes, *deps])
        truffle_app = work / "truffle-values-app"
        native_image("-O2", "--add-exports=org.graalvm.truffle.runtime/com.oracle.truffle.runtime=ALL-UNNAMED",
            "--initialize-at-build-time=fixture.values.TruffleValuesMain$Read,dev.elide.myna.values",
            "-cp", os.pathsep.join(map(str, [classes, *deps])),
            "-H:+UnlockExperimentalVMOptions", *linker, "-H:-UnlockExperimentalVMOptions",
            "fixture.values.TruffleValuesMain", "-o", truffle_app)
        self.assertIn("Truffle reader runtime compilation passed", run(truffle_app, cwd=work).stdout)


    def test_records_are_cstructs(self):
        """Records are @CStruct interfaces: StackValue and SizeOf agree with SIZE, every carrier keeps
        its raw bits in both directions, and each @CField accessor is the single access at the
        descriptor's offset that the raw offset accessor was."""
        work = ROOT / "build/records-test"
        fixture = ROOT / "tests/records"
        generated = work / "generated"
        shutil.rmtree(work, ignore_errors=True)
        work.mkdir(parents=True)
        run(BINARY, "generate", fixture / "records.seam", "--out", generated, "--target", TARGET)
        run("cc", "-O2", "-fPIC", "-Wall", "-Werror", "-Wmissing-prototypes", "-I", generated,
            "-c", fixture / "native.c", "-o", work / "native.o")
        classes = work / "classes"
        compile_native(classes, generated, generated / "RecordsApi.java", generated / "RecordsApiDirectives.java",
                       fixture / "RecordsMain.java")
        app = work / "records-app"
        native_image("-O2", "-cp", classes,
                     "-H:+UnlockExperimentalVMOptions", "-H:-DeleteLocalSymbols", f"-H:NativeLinkerOption={work / 'native.o'}",
                     "-H:-UnlockExperimentalVMOptions", "fixture.records.RecordsMain", "-o", app)
        self.assertIn("Native Image @CStruct records passed", run(app).stdout)
        # Evidence: the measured methods' machine code, retained under build/reports.
        evidence = ROOT / "build/reports/native-image/records"
        def body(method):
            return disassembly(app, "RecordsMain", method, evidence)
        # Each @CField accessor against the raw offset access it replaced: one access at the
        # descriptor's offset (x86-64 and AArch64 spellings) and otherwise the same code.
        narrow = re.compile(r"(movs[bw]l %a[lx], %eax|sxt[bh] w\d+, w\d+)$")
        offsets = {"U64": 8, "F64": 32, "Next": 40, "U32": 60, "F32": 64, "I16": 68, "U16": 70,
                   "Char": 73, "I8": 74, "U8": 75, "Bool": 72}
        for access in ("read", "write"):
            for kind, offset in offsets.items():
                if access == "write" and kind not in ("U64", "Next", "U32", "U16", "U8", "Bool"):
                    continue
                name = access + kind
                field, raw = body(name), body("raw" + name[0].upper() + name[1:])
                at = [i for i in field if f"{offset:#x}(%rdi)" in i or f"[x0, #{offset:#x}]" in i or f"[x0, #{offset}]" in i]
                self.assertEqual(len(at), 1, field)
                if access == "read" and kind in ("I16", "U16", "Char", "I8", "U8"):
                    # Known SVM gap: a narrow C-interface read is a CInterfaceReadNode, which the
                    # backend's sign-extending-load match rule does not fold, so one register
                    # extension follows the (already extending) load. Fold it and this fails.
                    extra = [i for i in field if narrow.match(i)]
                    self.assertEqual(len(extra), 1, field)
                    field = [i for i in field if i is not extra[0]]
                if name == "writeBool":
                    # Better than before: a Java boolean is already 0 or 1, so the store needs no
                    # `value ? 1 : 0` normalization; the code is the raw form without it.
                    self.assertLess(len(field), len(raw), field)
                    self.assertTrue(set(field) - set(raw) == {at[0]}, field)
                    continue
                self.assertEqual(field, raw, f"@CField {name} must compile to the raw offset access's code")

    def test_bemo_context_delegates(self):
        """Bemo names its own c_context: the generated context delegates its library path (static
        linkage), macros, options and headers to it; tests/bemo/context.h fails the C query unless
        all of them arrive, before seam.h."""
        work = ROOT / "build/bemo-test"
        work.mkdir(parents=True, exist_ok=True)
        generated = work / "generated"
        fixture = ROOT / "tests/bemo"
        run(BINARY, "generate", fixture / "bemo.seam", "--out", generated, "--target", TARGET)
        obj = work / "bemo.o"
        run("rustc", "--edition=2024", "--crate-type=lib", "--emit=obj", "-Cpanic=abort", "-Crelocation-model=pic", fixture / "native.rs", "-o", obj)
        run("ar", "rcs", work / "libbemofixture.a", obj)
        classes = work / "classes"
        # Renamed/removed entry-point classes must not survive an earlier fixture build.
        if classes.exists():
            shutil.rmtree(classes)
        classes.mkdir()
        compile_native(classes, generated, generated / "BemoImports.java", generated / "BemoImportsDirectives.java",
                       *sorted(fixture.glob("*.java")))
        app = work / "bemo-app"
        # No native-linker flags: @CContext supplies the path and @CLibrary selects the archive.
        native_image( "-O1", "-cp", classes,
            "--initialize-at-build-time=fixture.bemo.BemoMain$Literals",
            f"-Dmyna.bemo.libraryPath={work}", f"-Dmyna.bemo.contextHeader={fixture / 'context.h'}",
            "fixture.bemo.BemoMain", "-o", app)
        self.assertIn("Bemo static linkage and handwritten callbacks passed", run(app).stdout)

    def test_consumer_ownership_and_off_heap_buffers(self):
        work = ROOT / "build/ownership-test"
        work.mkdir(parents=True, exist_ok=True)
        generated = work / "generated"
        run(BINARY, "generate", ROOT / "tests/ownership/ownership.seam", "--out", generated, "--target", TARGET)
        fixture = work / "ownership.o"
        run("rustc", "--edition=2024", "--crate-type=lib", "--emit=obj", "-Cpanic=abort", "-Copt-level=2", "-Crelocation-model=pic",
            ROOT / "tests/ownership/native.rs", "-o", fixture)
        os.environ["SEAM_GENERATED"] = str(generated)
        driver = work / "driver"
        run("rustc", "--edition=2024", ROOT / "tests/ownership/driver.rs", "-C", f"link-arg={fixture}", "-o", driver)
        self.assertIn("Rust ownership passed", run(driver).stdout)
        # A borrowed resource must prevent moving/dropping its owner while the borrow is used.
        invalid = work / "borrow_escape.rs"
        invalid.write_text('mod seam { include!(concat!(env!("SEAM_GENERATED"), "/seam.rs")); }\n'
            'fn main() { let owner = seam::ownership::Ticket::create().unwrap(); '
            'let borrow = owner.borrow(); drop(owner); let _ = borrow.as_ptr(); }\n')
        rejected = run("rustc", "--edition=2024", "--emit=metadata", invalid, "-o", work / "invalid.rmeta", ok=False)
        self.assertNotEqual(rejected.returncode, 0)
        self.assertIn("cannot move out of `owner` because it is borrowed", rejected.stderr)
        # Confined resources must not accidentally implement Send.
        confined = work / "confined.seam"
        confined.write_text((ROOT / "tests/ownership/ownership.seam").read_text()
            .replace("threading=shared", "threading=confined")
            .replace("java_class=NativeApi", "java_class=ConfinedApi"))
        run(BINARY, "generate", confined, "--out", work / "confined", "--target", TARGET)
        os.environ["SEAM_GENERATED"] = str(work / "confined")
        invalid.write_text('mod seam { include!(concat!(env!("SEAM_GENERATED"), "/seam.rs")); }\n'
            'fn main() { let owner = seam::ownership::Ticket::create().unwrap(); '
            'std::thread::spawn(move || drop(owner)); }\n')
        rejected = run("rustc", "--edition=2024", "--emit=metadata", invalid, "-o", work / "invalid.rmeta", ok=False)
        self.assertNotEqual(rejected.returncode, 0)
        self.assertIn("cannot be sent between threads safely", rejected.stderr)
        # A wrapper that passes a pointer through cannot vouch for it, so only that call is unsafe.
        os.environ["SEAM_GENERATED"] = str(generated)
        invalid.write_text('mod seam { include!(concat!(env!("SEAM_GENERATED"), "/seam.rs")); }\n'
            'fn main() { let mut output = [0u8; 1]; let _ = seam::ownership::checksum(&output); '
            'let _ = seam::ownership::transfer(&[], &mut output, core::ptr::null_mut()); }\n')
        rejected = run("rustc", "--edition=2024", "--emit=metadata", invalid, "-o", work / "invalid.rmeta", ok=False)
        self.assertNotEqual(rejected.returncode, 0)
        self.assertEqual(rejected.stderr.count("error[E0133]"), 1, rejected.stderr)
        self.assertIn("call to unsafe function `transfer` is unsafe", rejected.stderr)
        classes = work / "classes"
        # Renamed/removed entry-point classes must not survive an earlier fixture build.
        if classes.exists():
            shutil.rmtree(classes)
        classes.mkdir()
        compile_native(classes, generated, generated / "NativeApi.java", generated / "NativeApiOwnership.java",
                       generated / "NativeApiDirectives.java", work / "confined/ConfinedApi.java",
                       work / "confined/ConfinedApiOwnership.java", work / "confined/ConfinedApiDirectives.java",
                       ROOT / "tests/ownership/ConfinedOwner.java", ROOT / "tests/ownership/OwnershipMain.java")
        shutil.copytree(work / "confined/META-INF", classes / "META-INF", dirs_exist_ok=True)
        app = work / "ownership-app"
        native_image( "-O1", "-cp", classes,
            "-H:+UnlockExperimentalVMOptions", f"-H:NativeLinkerOption={fixture}", "-H:-UnlockExperimentalVMOptions",
            "OwnershipMain", "-o", app)
        self.assertIn("Native Image consumer ownership and off-heap buffers passed", run(app).stdout)

    def test_generated_aot_ffm_stubs(self):
        work = ROOT / "build/aot-ffm-test"
        work.mkdir(parents=True, exist_ok=True)
        generated = work / "generated"
        run(BINARY, "generate", ROOT / "examples/buffer.seam", "--jni", "--out", generated, "--target", TARGET)
        classes = work / "classes"
        # Renamed/removed entry-point classes must not survive an earlier fixture build.
        if classes.exists():
            shutil.rmtree(classes)
        classes.mkdir()
        # The generated properties register the context's header check; without the Native Image
        # class in the image it has nothing to check, so FFM-only images compile just its class.
        compile_native(classes, generated, generated / "MynaFFM.java", generated / "MynaNativeJNI.java",
                       generated / "MynaForeignFeature.java", generated / "MynaNativeDirectives.java",
                       ROOT / "tests/abi/FfmMain.java")
        library = work / ("libinspect.dylib" if sys.platform == "darwin" else "libinspect.so")
        os.environ["SEAM_GENERATED"] = str(generated)
        run("rustc", "--edition=2024", "--crate-type=cdylib", "-C", "opt-level=2", "--cfg", 'feature="std"', ROOT / "tests/abi/inspect.rs", "-o", library)
        run("rustc", "--edition=2024", "--crate-type=lib", "--emit=obj", "-Cpanic=abort", "-Copt-level=2",
            "-Crelocation-model=pic", ROOT / "tests/abi/inspect.rs", "-o", work / "inspect.o")
        home = Path(os.environ["GRAALVM_HOME"])
        run("cc", "-O2", "-flto=thin", "-I", home / "include", "-I", home / "include" / ("darwin" if sys.platform == "darwin" else "linux"),
            "-c", generated / "seam-jni.c", "-o", work / "jni.bc")
        run(BINARY, "apply", "--contracts", generated / "seam-jni.ll", "--input", work / "jni.bc",
            "--output", work / "jni.annotated.bc", "--llvm-tool", ROOT / "build/dist/myna-llvm")
        (work / "libbuffer_jni.a").unlink(missing_ok=True)
        run("ar", "rcs", work / "libbuffer_jni.a", work / "jni.annotated.bc", work / "inspect.o")
        shutil.copyfile(generated / "libbuffer_jni.a.symbols", work / "libbuffer_jni.a.symbols")
        native_image( "-O1", "--enable-native-access=ALL-UNNAMED", "-cp", classes,
            "-H:+UnlockExperimentalVMOptions", f"-H:CLibraryPath={work}", "-H:-UnlockExperimentalVMOptions",
            "--features=dev.elide.myna.generated.MynaForeignFeature", "FfmMain", "-o", work / "ffm-app")
        self.assertIn("AOT FFM -> Rust passed", run(work / "ffm-app", library, "jni").stdout)

    def test_rust_native_image_rust(self):
        work = ROOT / "build/native-image-test"
        work.mkdir(parents=True, exist_ok=True)
        generated = work / "generated"
        # The hosted Feature consumes the exact same target-adjusted DSL as the generator.
        source = work / "buffer.seam"
        source.write_text((ROOT / "examples/buffer.seam").read_text().replace("aarch64-apple-darwin", TARGET))
        run(BINARY, "generate", source, "--out", generated)
        # Elide supplies the Graal SDK to its compiler and native-image builder.
        classes = work / "classes"
        # Renamed/removed entry-point classes must not survive an earlier fixture build.
        if classes.exists():
            shutil.rmtree(classes)
        classes.mkdir()
        compile_native(classes, generated, generated / "MynaNative.java", generated / "MynaNativeDirectives.java",
                       ROOT / "tests/abi/BufferOps.java")
        inspector = work / "inspect.o"
        os.environ["SEAM_GENERATED"] = str(generated)
        run("rustc", "--edition=2024", "--crate-type=lib", "--emit=obj", "-Cpanic=abort", "-Copt-level=2", "-Crelocation-model=pic", ROOT / "tests/abi/inspect.rs", "-o", inspector)
        feature = ROOT / "build/dist/myna-feature.jar"
        feature_output = work / "feature-output"
        native_image( "--shared", "-O1", "-cp", os.pathsep.join([str(classes), str(feature)]),
            "--features=dev.elide.myna.nativeimage.MynaFeature", f"-Dmyna.input={source}", f"-Dmyna.output={feature_output}",
            "-H:+UnlockExperimentalVMOptions", f"-H:NativeLinkerOption={inspector}", "-H:-UnlockExperimentalVMOptions",
            "-o", work / "libseamfixture")
        self.assertEqual((generated / "seam.json").read_bytes(), (feature_output / "seam.json").read_bytes())
        os.environ["SEAM_GENERATED"] = str(generated)
        driver = work / "driver"
        run("rustc", "--edition=2024", "-Awarnings", ROOT / "tests/abi/driver.rs", "-L", work, "-l", "dylib=seamfixture", "-C", f"link-arg=-Wl,-rpath,{work}", "-o", driver)
        self.assertIn("round trip passed", run(driver).stdout)
        c_driver = work / "c-driver"
        run(LLVM / "clang", "-O2", "-I", generated, ROOT / "tests/abi/driver.c", "-L", work, "-lseamfixture",
            f"-Wl,-rpath,{work}", "-o", c_driver)
        self.assertIn("narrow returns passed", run(c_driver).stdout)


class GeneratedContextTests(unittest.TestCase):
    """The generated C context: seam.h reaches the image build and nothing else, its fingerprint is
    checked there, and the @CConstant accessors fold to the descriptor's literals."""

    @classmethod
    def setUpClass(cls):
        cls.work = ROOT / "build/header-test"
        shutil.rmtree(cls.work, ignore_errors=True)
        # The builder JVM's temporary directory, so leftovers are this suite's alone.
        cls.tmp = cls.work / "tmp"
        cls.tmp.mkdir(parents=True)
        cls.generated = cls.work / "generated"
        run(BINARY, "generate", HEADER / "header.seam", "--out", cls.generated, "--target", TARGET)
        cls.classes = cls.work / "classes"
        compile_native(cls.classes, cls.generated, cls.generated / "HeaderApi.java",
                       cls.generated / "HeaderApiDirectives.java", HEADER / "HeaderMain.java",
                       HEADER / "DirectivesProbe.java")
        cls.app = cls.work / "header-app"
        native_image("-O2", "-cp", cls.classes, *cls.builder_tmp(cls),
                     "-H:+UnlockExperimentalVMOptions", "-H:+PrintAnalysisCallTree", "-H:-DeleteLocalSymbols",
                     "-H:-UnlockExperimentalVMOptions", "fixture.header.HeaderMain", "-o", cls.app)

    def builder_tmp(self):
        return (f"-J-Djava.io.tmpdir={self.tmp}",)

    def test_header_check_passes(self):
        """No consumer context: layouts and constants come from the embedded seam.h, and each
        @CConstant accessor compiles to the same code as its literal."""
        out = run(self.app).stdout
        self.assertIn("analysisMagic() == 0x3141514b: true", out)
        self.assertIn("Generated C context, header check and @CConstant accessors passed", out)
        evidence = ROOT / "build/reports/native-image/header"
        for name in ("magic", "level", "limit"):
            constant = disassembly(self.app, "HeaderMain", name + "CConstant", evidence)
            literal = disassembly(self.app, "HeaderMain", name + "Literal", evidence)
            self.assertEqual(constant, literal, f"@CConstant {name} must compile to its literal's code")

    def test_header_text_build_time_only(self):
        """The context and the header text it embeds stay in the image builder: the context class is
        not reachable, and no line of seam.h is in the image."""
        reports = sorted(self.work.rglob("used_classes_*.txt"))
        self.assertEqual(len(reports), 1, reports)
        # Each line is `<class loader>:<class>`.
        used = [line.rsplit(":", 1)[-1] for line in reports[0].read_text().splitlines()]
        self.assertIn("fixture.header.HeaderMain", used)  # the report lists reached classes
        self.assertEqual([c for c in used if "Directives" in c], [])
        methods = next(self.work.rglob("used_methods_*.txt")).read_text()
        self.assertNotIn("HeaderApiDirectives", methods)
        evidence = ROOT / "build/reports/native-image/header"
        evidence.mkdir(parents=True, exist_ok=True)
        (evidence / "used_classes.txt").write_text("\n".join(c for c in used if c.startswith("fixture.")) + "\n")
        header = (self.generated / "seam.h").read_text().splitlines()
        abi = next(line for line in header if line.startswith("#define HEADER_FIXTURE_SEAM_ABI "))
        image = self.app.read_bytes()
        self.assertIn(b"Generated C context, header check and @CConstant accessors passed", image)
        for line in (abi, "SEAM_ASSERT(sizeof(void *) == 8, \"seam target requires 64-bit pointers\");",
                     "struct Pair {"):
            self.assertNotIn(line.encode(), image, line)
        if shutil.which("strings"):
            strings = run("strings", "-a", self.app).stdout.splitlines()
            (evidence / "strings-seam.txt").write_text(
                "".join(f"{line}\n" for line in strings if "SEAM" in line or "seam" in line))
            self.assertNotIn(abi, strings)

    def test_header_check_rejects_stale_header(self):
        """This generation's bindings with another generation's context: the build fails, naming both
        fingerprints."""
        source = self.work / "stale.seam"
        source.write_text((HEADER / "header.seam").read_text().replace("value failed = 1", "value failed = 2"))
        stale = self.work / "stale"
        run(BINARY, "generate", source, "--out", stale, "--target", TARGET)
        bindings, header = ((d / "seam.abi").read_text().strip() for d in (self.generated, stale))
        self.assertNotEqual(bindings, header)
        classes = self.work / "stale-classes"
        compile_native(classes, self.generated, self.generated / "HeaderApi.java", stale / "HeaderApiDirectives.java",
                       HEADER / "HeaderMain.java")
        result = native_image("-O1", "-cp", classes, *self.builder_tmp(), "fixture.header.HeaderMain",
                              "-o", self.work / "stale-app", ok=False)
        output = result.stdout + result.stderr
        self.assertNotEqual(result.returncode, 0, output)
        self.assertIn(f"seam.h fingerprint {header} differs from generated bindings {bindings}", output)

    def test_large_header(self):
        """A seam.h over 64 KiB, beyond one class-file string constant, is embedded in pieces."""
        lines = ["module large_fixture target=aarch64-apple-darwin java_package=fixture.large java_class=LargeApi"]
        for index in range(160):
            lines.append(f"struct Record{index} size=64 align=8")
            lines += [f"  field value{field} type=i64 offset={field * 8}" for field in range(8)]
            lines.append("end")
        source = self.work / "large.seam"
        source.write_text("\n".join(lines) + "\n")
        generated = self.work / "large"
        run(BINARY, "generate", source, "--out", generated, "--target", TARGET)
        self.assertGreater(len((generated / "seam.h").read_bytes()), 64 * 1024)
        main = self.work / "LargeMain.java"
        main.write_text("package fixture.large;\nimport org.graalvm.nativeimage.c.struct.SizeOf;\n"
                        "public final class LargeMain { public static void main(String[] args) {\n"
                        "  System.out.println(\"large \" + SizeOf.get(LargeApi.Record0.class) + \" \""
                        " + SizeOf.get(LargeApi.Record159.class)); } }\n")
        classes = self.work / "large-classes"
        compile_native(classes, generated, generated / "LargeApi.java", generated / "LargeApiDirectives.java", main)
        app = self.work / "large-app"
        native_image("-O1", "-cp", classes, *self.builder_tmp(), "fixture.large.LargeMain", "-o", app)
        self.assertIn("large 64 64", run(app).stdout)

    def test_no_temp_dirs_left(self):
        """The context writes seam.h once per builder JVM and removes it when that JVM exits, after a
        failed build as after a good one."""
        self.test_header_check_rejects_stale_header()  # also a failing build in this directory
        probe = run(Path(os.environ["GRAALVM_HOME"]) / "bin/java", f"-Djava.io.tmpdir={self.tmp}", "-cp", self.classes,
                    "fixture.header.DirectivesProbe", self.generated / "seam.h").stdout
        self.assertIn(f"header {self.tmp}", probe)
        self.assertEqual(sorted(p.name for p in self.tmp.glob("myna-seam*")), [])

if __name__ == "__main__": main(sys.modules[__name__], "native-image-integration")
