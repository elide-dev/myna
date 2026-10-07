# Migrating to Myna 0.5.0

The project formerly named `svmgen` is now **Myna**. Version 0.5.0 renames its public integration points:

| Previous | Myna |
| --- | --- |
| `svmgen`, `svmgen-llvm` | `myna`, `myna-llvm` |
| `dev.elide:svmgen` | `dev.elide:myna` |
| `svmgen.jar`, `svmgen-feature.jar` | `myna.jar`, `myna-feature.jar` |
| Java package `dev.elide.seam` | `dev.elide.myna` |
| Hosted `dev.elide.seam.nativeimage.SeamFeature` | `dev.elide.myna.nativeimage.MynaFeature` |
| `-Dsvmgen.input`, `-Dsvmgen.output` | `-Dmyna.input`, `-Dmyna.output` |
| Default `SeamNative`, `SeamFFM`, `SeamForeignFeature`, `SeamOwnership` | `MynaNative`, `MynaFFM`, `MynaForeignFeature`, `MynaOwnership` |
| Default generated package `dev.elide.seam.generated` | `dev.elide.myna.generated` |
| Test overrides `SVMGEN_BIN`, `SVMGEN_LLVM` | `MYNA_BIN`, `MYNA_LLVM` |
| By-value shim suffix `_svmgen_ref` | `_myna_ref` |
| LLVM metadata `svmgen.contract`, `svmgen.applied` | `myna.contract`, `myna.applied` |

Rebuild the generator and helper, regenerate bindings into a fresh output directory, update imports/build rules/Feature options, and rebuild consumers together. Remove old generated Java files when updating an existing output directory; the generator does not delete arbitrary files. Clean compiled class directories too: stale entry-point classes can otherwise collide with the renamed classes when building a Native Image shared library. ABI fingerprints change with the generator version. Previously annotated objects are rejected by the new applicator: apply new contracts to original compiler output, not cached objects modified by the old tool.

The `.seam` descriptor format, language-neutral Seam IR, `seam.h`/`seam.rs`/`seam.ll`/`seam.json`/`seam.abi` filenames, explicit `java_package`/`java_class` settings, and caller-defined symbol names remain supported. The synthesized by-value shim suffix is the symbol-level naming change.

Container publication derives the GHCR repository name from GitHub's repository name. After the GitHub repository rename, subsequent publications use the new name. No container or release is published merely by changing these source files.
