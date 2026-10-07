# svmgen

A Native Image command-line generator for Rust / Substrate VM seams. One typed Seam IR produces C headers, Rust raw FFI, Java FFM, `@CFunction` imports, `@CEntryPoint` exports, JSON descriptors, and LLVM 23 contracts.

**`svmgen apply` attaches those contracts to the actual LLVM objects and Rust archive members that participate in ThinLTO.** It updates matching functions and direct calls, verifies ABI signatures, and rebuilds summaries and module hashes before linkage. See [the integration design](docs/thinlto.md).

**Substrate VM is the primary runtime.** Fixed symbols use direct generated C API bindings; explicitly contracted native leaf calls can elide VM transitions. Build-time FFM registration is an optional adapter. See [the performance design](docs/performance.md).

## Build and use

Requirements: Elide 1.5.4+, GraalVM JDK 25 with `native-image`, and a native C toolchain. Building the object applicator also requires CMake and LLVM 23 development libraries.

```sh
scripts/build.sh                     # native executable, generator JAR, Feature JAR
LLVM_CONFIG=/path/to/llvm-config scripts/build.sh --llvm

build/dist/svmgen generate examples/buffer.seam --out build/seam
build/dist/svmgen validate examples/buffer.seam
build/dist/svmgen generate examples/buffer.seam --out build/seam --check
```

Outputs live in `build/dist/`. `elide.pkl` is the primary application build. `pom.xml` supplies conventional JVM test/coverage tooling and a JAR build (`mvn verify`). The Java generator has no runtime dependencies. Native compilation uses Elide's external Native Image driver.

The descriptor target is explicit. Use `--target x86_64-unknown-linux-gnu` to override the example's macOS ARM64 target. The CLI never guesses an ABI from its own host.

```sh
# Compile Rust for linker-driven ThinLTO, then annotate its real archive.
rustc --crate-type=rlib -Clinker-plugin-lto src/lib.rs -o build/libapp.rlib
build/dist/svmgen apply \
  --contracts build/seam/seam.ll \
  --input build/libapp.rlib --output build/libapp.seam.rlib \
  --llvm-tool build/dist/svmgen-llvm
```

Use the rewritten archive/object in the final LLVM 23 / LLD link. A contract-only `.ll` file is an interchange artifact; simply adding it to a link is insufficient. Keep the original object: changed contracts must be applied to fresh compiler output.

## Binding descriptions

The initial frontend is a strict, line-oriented text DSL with `#` comments. See [the DSL reference](docs/dsl.md), [the owned end-to-end fixture](examples/buffer.seam), and [the inventoried Elide LZMA ABI](examples/elide_lzma.seam).

```text
module example abi=1 target=x86_64-unknown-linux-gnu
import readCount symbol=example_read_count return=i64 error=no_failure
  param value type=ptr<i64> nullable=false ownership=borrowed access=read nonnull=true@explicit_contract readonly=true@explicit_contract
end
```

Every generation produces:

- `seam.h`, `seam.rs`, `SeamFFM.java`, `SeamNative.java`, `SeamForeignFeature.java`
- `seam.json` with optimizer-fact provenance, `seam.abi` with a SHA-256 fingerprint
- `seam.ll`, consumed by `svmgen apply` or assembled by LLVM 23's `llvm-as`

Strict mode rejects unapproved facts. `--relaxed` preserves them in JSON and omits them from LLVM. Unsupported shapes and contradictory contracts always fail. Explicit contracts are obligations of the implementation; validation cannot prove an arbitrary native implementation obeys them.

## Tests and reports

```sh
mvn -B verify                  # JVM unit/golden/negative tests and coverage gate
scripts/test.sh                # also builds and tests the native CLI, LLVM integration, Native Image ABI
```

The full suite additionally needs Python 3, Rust using LLVM 23, and LLD 23. Set `LLVM_BIN` to that toolchain's `bin` directory and `LLVM_CONFIG` to its `llvm-config`. The LLVM helper must be built against the same LLVM toolchain used for linking; generic system LLVM is not a hermetic substitute for Elide's toolchain in production.

Individual integration tests can run against existing builds:

```sh
SVMGEN_BIN=build/dist/svmgen python3 tests/e2e.py
LLVM_BIN=/path/to/llvm/bin python3 tests/llvm_integration.py
python3 tests/native_image_integration.py
```

| Report | Location |
| --- | --- |
| JVM test XML | `target/surefire-reports/TEST-*.xml` |
| JaCoCo coverage XML | `target/site/jacoco/jacoco.xml` |
| JaCoCo HTML | `target/site/jacoco/index.html` |
| Native CLI / LLVM / Native Image test XML | `build/reports/*.xml` |

Coverage gates are 85% line coverage and 70% branch coverage. Golden outputs are checked in under `tests/golden/`. The LLVM suite proves load elimination in both C and Rust, checks attributes in LLD's saved ThinLTO output, preserves archive metadata/native members, and rejects stale contracts and incompatible ABIs. The Native Image suite runs Rust → Native Image → Rust and tests exception translation and a leaf call with no VM transition. It also builds and runs the generated FFM bindings as a Native Image executable.

CI runs JVM tests and native integration on Linux and macOS, retaining the XML reports and built artifacts. It pins a Rust nightly and requires LLVM major 23; update both together when upgrading the toolchain.

## Downstream distribution

Two Docker targets are provided:

```sh
docker build --target generator -t svmgen:local .
docker build --target llvm23 -t svmgen:local-llvm23 .
docker run --rm --user "$(id -u):$(id -g)" -v "$PWD:/work" svmgen:local \
  generate bindings.seam --out generated --target x86_64-unknown-linux-gnu
```

The `llvm23` variant also supports `apply` and includes the LLVM tools. The default image contains the native generator and its JAR. The Feature JAR is available from `build/dist/` and CI build artifacts. For hermetic builds, build the helper inside your pinned LLVM toolchain image; the convenience LLVM image follows the official LLVM 23 package repository.

The container workflow builds and smoke-tests both variants on pull requests. Pushing a `v*` tag builds, tests, and publishes `ghcr.io/<owner>/<repo>:<tag>` and `<tag>-llvm23` using the repository's `GITHUB_TOKEN`. Images are Linux amd64 initially. No image has been published by this checkout; tag publication requires the repository's Actions/package settings to allow it. Downstream builds should pin an image digest.

## Native Image Feature

Pre-generate and compile the Java glue before building the image. Add `svmgen-feature.jar` to the image builder's classpath and pass:

```text
--features=dev.elide.seam.nativeimage.SeamFeature
-Dsvmgen.input=/absolute/path/bindings.seam
-Dsvmgen.output=/absolute/path/build/seam
```

The Feature exports the validated descriptors during `beforeAnalysis`; it does not compile newly emitted Java during image analysis. It currently consumes explicit DSL facts. Annotation scanning and Graal-proven enrichment are future frontend work.

## Optional FFM and JNI adapters

Typed FFM calls and Native Image downcall-stub registration are implemented. Fuller FFM and Static JNI adapters are future compatibility work, subordinate to the direct Substrate VM path. The [JVM roadmap](docs/jvm-roadmap.md) covers typed FFM bindings, callbacks/lifetimes, explicit JNI lowering, static registration, and contract scope across VM adapters.

## Current scope

The tested subset is C calling convention, `bool`, `char`, 8- to 64-bit and pointer-sized integers with per-target extension, floats, typed pointers, callbacks in both directions, isolate threads as values, opaque handles, naturally aligned records with explicit offsets, and isolate-thread exports with include predicates and abort/integer/null exception translation. Records cross function boundaries by pointer.

By-value aggregates, enums, allocator return contracts, automatic ownership inference, additional address spaces, and YAML remain future work. The LZMA fixture records a real Elide ABI, but the separate Elide checkout has not been migrated. The original [architecture briefing](elide-seam-generator-briefing.md) describes the broader roadmap.
