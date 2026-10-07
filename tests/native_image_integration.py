"""Build and execute real Native Image C API glue and the hosted Feature."""
import os
from pathlib import Path
import sys
import unittest
from support import BINARY, ROOT, TARGET, main, run
LLVM = Path(os.environ.get("LLVM_BIN", "/opt/homebrew/opt/llvm/bin" if sys.platform == "darwin" else "/usr/lib/llvm-23/bin"))

class NativeImageIntegrationTests(unittest.TestCase):
    def test_consumer_ownership_and_off_heap_buffers(self):
        work = ROOT / "build/ownership-test"
        work.mkdir(parents=True, exist_ok=True)
        generated = work / "generated"
        run(BINARY, "generate", ROOT / "examples/ownership.seam", "--out", generated, "--target", TARGET)
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
        confined.write_text((ROOT / "examples/ownership.seam").read_text().replace("threading=shared", "threading=confined"))
        run(BINARY, "generate", confined, "--out", work / "confined", "--target", TARGET)
        os.environ["SEAM_GENERATED"] = str(work / "confined")
        invalid.write_text('mod seam { include!(concat!(env!("SEAM_GENERATED"), "/seam.rs")); }\n'
            'fn main() { let owner = seam::ownership::Ticket::create().unwrap(); '
            'std::thread::spawn(move || drop(owner)); }\n')
        rejected = run("rustc", "--edition=2024", "--emit=metadata", invalid, "-o", work / "invalid.rmeta", ok=False)
        self.assertNotEqual(rejected.returncode, 0)
        self.assertIn("cannot be sent between threads safely", rejected.stderr)
        run("mvn", "-q", "org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath", "-Dmdep.outputFile=build/sdk-classpath.txt", "-DincludeScope=compile")
        sdk = (ROOT / "build/sdk-classpath.txt").read_text().strip()
        classes = work / "classes"
        classes.mkdir(exist_ok=True)
        run("javac", "-cp", sdk, "-d", classes, generated / "NativeApi.java", generated / "NativeApiOwnership.java", ROOT / "tests/ownership/OwnershipMain.java")
        app = work / "ownership-app"
        run("native-image", "--no-fallback", "-O1", "-cp", classes,
            "-H:+UnlockExperimentalVMOptions", f"-H:NativeLinkerOption={fixture}", "-H:-UnlockExperimentalVMOptions",
            "OwnershipMain", "-o", app)
        self.assertIn("Native Image consumer ownership and off-heap buffers passed", run(app).stdout)

    def test_generated_aot_ffm_stubs(self):
        work = ROOT / "build/aot-ffm-test"
        work.mkdir(parents=True, exist_ok=True)
        generated = work / "generated"
        run(BINARY, "generate", ROOT / "examples/buffer.seam", "--out", generated, "--target", TARGET)
        run("mvn", "-q", "org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath", "-Dmdep.outputFile=build/sdk-classpath.txt", "-DincludeScope=compile")
        sdk = (ROOT / "build/sdk-classpath.txt").read_text().strip()
        classes = work / "classes"
        classes.mkdir(exist_ok=True)
        run("javac", "-cp", sdk, "-d", classes, generated / "SeamFFM.java", generated / "SeamForeignFeature.java", ROOT / "tests/abi/FfmMain.java")
        library = work / ("libinspect.dylib" if sys.platform == "darwin" else "libinspect.so")
        os.environ["SEAM_GENERATED"] = str(generated)
        run("rustc", "--edition=2024", "--crate-type=cdylib", "--cfg", 'feature="std"', ROOT / "tests/abi/inspect.rs", "-o", library)
        run("native-image", "--no-fallback", "-O1", "--enable-native-access=ALL-UNNAMED", "-cp", classes,
            "--features=dev.elide.seam.generated.SeamForeignFeature", "FfmMain", "-o", work / "ffm-app")
        self.assertIn("AOT FFM -> Rust passed", run(work / "ffm-app", library).stdout)

    def test_rust_native_image_rust(self):
        work = ROOT / "build/native-image-test"
        work.mkdir(parents=True, exist_ok=True)
        generated = work / "generated"
        run(BINARY, "generate", ROOT / "examples/buffer.seam", "--out", generated, "--target", TARGET)
        # Resolve pinned SDK dependencies from the test build for javac. Graal supplies them to its builder.
        run("mvn", "-q", "org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath", "-Dmdep.outputFile=build/sdk-classpath.txt", "-DincludeScope=compile")
        sdk = (ROOT / "build/sdk-classpath.txt").read_text().strip()
        classes = work / "classes"
        classes.mkdir(exist_ok=True)
        run("javac", "-cp", sdk, "-d", classes, generated / "SeamNative.java", ROOT / "tests/abi/BufferOps.java")
        inspector = work / "inspect.o"
        os.environ["SEAM_GENERATED"] = str(generated)
        run("rustc", "--edition=2024", "--crate-type=lib", "--emit=obj", "-Cpanic=abort", "-Copt-level=2", "-Crelocation-model=pic", ROOT / "tests/abi/inspect.rs", "-o", inspector)
        feature = ROOT / "build/dist/svmgen-feature.jar"
        # The hosted Feature consumes the exact same target-adjusted DSL as the generator.
        source = work / "buffer.seam"
        source.write_text((ROOT / "examples/buffer.seam").read_text().replace("aarch64-apple-darwin", TARGET))
        feature_output = work / "feature-output"
        run("native-image", "--shared", "--no-fallback", "-O1", "-cp", os.pathsep.join([str(classes), str(feature)]),
            "--initialize-at-build-time=dev.elide.seam.generated.SeamNative$Literals",
            "--features=dev.elide.seam.nativeimage.SeamFeature", f"-Dsvmgen.input={source}", f"-Dsvmgen.output={feature_output}",
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

if __name__ == "__main__": main(sys.modules[__name__], "native-image-integration")
