# Migrating to Myna 0.8.0 / binding ABI 2

Binding ABI 2 makes the inlineable native leaf path the default for imports. Regenerate bindings and rebuild consumers together; do not combine old generated artifacts with new descriptors. Canonical descriptor schema 3 records `bindingAbiVersion: 2` and each import's effective `nativeCallPolicy`. Generated Java, C and Rust expose `BINDING_ABI_VERSION`. A module's `abi=...` remains its independent application protocol version. The value-tape layout remains ABI v3.

## Audit imports

Default imports promise that every execution is short, bounded, nonblocking and cannot reenter Java. For operations outside that contract, explicitly request a normal VM transition:

```text
import process symbol=native_process return=i32 error=no_failure native_leaf=false@explicit_contract
end
```

Audit allocation, locks, I/O, callbacks, unbounded input processing, cleanup and error paths. `noreturn` imports must opt out. Native implementations must never unwind language exceptions through the boundary. Leaf policy does not establish LLVM effects such as `nounwind` or `nofree`. Exports keep their normal isolate-entry behavior. FFM calls retain their own transition machinery.

Existing explicit `native_leaf=true@explicit_contract` declarations still work. Missing declarations now select the fast path. Relaxed mode never treats an unapproved explicit leaf fact as sufficient evidence to remove transitions.

## Compile and package generated bindings

Use the pinned Elide Graal distribution from `mise run setup`. After generation:

```sh
mise exec -- scripts/python.sh scripts/compile-native-bindings.py \
  --out build/classes --generated build/seam path/to/Application.java
```

Supply additional Java dependencies with `--classpath`. Repeat `--generated` for multiple modules. The helper compiles generated Java files with three compile-only SDK jars from `$GRAALVM_HOME/lib/svm`: `svm-shared.jar`, `svm-guest-staging.jar`, and `svm-guest.jar`. It copies generated `META-INF/native-image` resources into the output directory. Include those resources when creating a JAR.

Equivalent Maven/Gradle integrations must supply those jars only for compilation and package the generated metadata. Do not bundle Graal implementation jars or add them to Native Image's application classpath. The metadata supplies the required builder module export and class-initialization policy automatically. The backend uses internal APIs of elide-dev/graal 25.5.5-elide.28; compiler changes require integration and performance tests.

Native linking still uses the module's `c_context` / `c_library` configuration or explicit native linker inputs. Ordinary calls obtain the relocated address through Graal's symbolic-address intrinsic; the pointer accessor uses a separate lazy runtime holder. Addresses are never captured from the build process; unused imports do not require their symbols. Libraries must remain valid for every call.

## Specialized callers

Ordinary calls keep their generated Java method names and ABI carriers. Myna emits a typed `<name>_Leaf` interface and `<name>LeafPointer()` accessor for each default leaf. Resolve the pointer before entering a bounded uninterruptible caller. First-use holder initialization can require Java runtime work and must remain interruptible.

The default wrapper and pointer adapter are inlineable. This removes the extra ordinary `@CFunction` leaf adapter stub while retaining the interruptible caller's safepoints. No compiler safety check is disabled. See [performance design](performance.md) and the [repeatable call-floor harness](../benchmarks/floor/README.md).
