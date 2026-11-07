# Third-party components

Myna's own code is available under `Apache-2.0 OR MIT`. Dependencies retain their own licenses; Myna's dual-license grant does not replace those terms.

- Rust dependencies are recorded in `Cargo.lock` and package manifests. The portable SDK includes the `myna-values` crate; its Rust dependencies are resolved by Cargo, not vendored into that archive.
- The C++ simdjson conformance source is downloaded at a pinned version with checked hashes by `tests/values_cpp.py`. It is a test dependency, not bundled into Myna's value ABI.
- `third-party/codspeed-jvm` and its nested submodules are benchmark dependencies. Their license texts remain in the respective upstream trees, including `jmh-fork/LICENSE` and the instrument-hooks `LICENSE-APACHE` / `LICENSE-MIT` files. They are not part of the portable value SDK.
- LLVM, GraalVM, Elide, compiler runtimes, and container base packages have separate distribution terms. Container SBOMs inventory the resulting image contents; source SBOMs and package manifests describe different scopes and are not interchangeable.

Preserve upstream notices when redistributing a bundled dependency or toolchain. Review the SBOM and the original package's license text for the exact distribution being shipped.

- The Truffle Native Image regression fixture uses checksum-pinned GraalVM 25.5.5-elide.24 Java dependencies listed in `tests/values/truffle-deps.json`. They are test-only and are not bundled in the portable value SDK.
