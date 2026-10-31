# CI and dependency maintenance

`on.push.yml`, `on.pr.yml`, and `on.queue.yml` run the same reusable JVM, native, and container workflows. The `Gate` job requires all of them to succeed, including on merge-queue events. Main builds are not cancelled by newer pushes; superseded PR builds are cancelled. Use the Gate check for branch rules when configuring required checks.

`check.jvm.yml` runs Elide JVM tests and coverage reports, followed by the coverage threshold checker. `check.native.yml` builds and exercises Myna on Linux and macOS, including LLVM and Native Image interoperability. `job.container.yml` builds and smoke-tests both container variants. `on.release.yml` alone grants package publication permissions, and publication additionally requires a `v*` tag and successful smoke tests. Manual runs on branches only build and test.

Actions are pinned to release commit SHAs; Docker base images include immutable digests. Linux jobs use runner hardening in audit mode. Checkout does not retain credentials. Codecov uses OIDC and the unified coverage/test-results action; uploads do not require a repository secret. Fork PR uploads use Codecov's public-repository tokenless behavior.

Dependabot checks Actions daily and Maven/Docker weekly. Maven compatibility-build plugin versions live in `pom.xml`. When updating the GraalVM SDK or JUnit, update the matching coordinates in `elide.pkl` too. The GraalVM SDK must remain compatible with Elide's embedded Native Image compiler. Update Elide in both `mise.toml` and the Dockerfile, and regenerate `mise.lock`.

Elide, Rust, CMake, and workflow linters are pinned in `mise.toml` and `mise.lock`. The shared setup action installs them with mise and installs Elide's Python component. No separate JDK or Python setup is required. Upgrade Rust only alongside verification that its LLVM major matches the LLVM 23 reader, writer, and linker; native CI checks this explicitly before building. The Ubuntu 24.04 and macOS 15 runners remain the supported platform baselines. Update OS baselines separately from library patches and verify both container libc compatibility and native linker behavior.

Before merging an update, run `mise run test`, `mise run lint`, and the container CI jobs. Inspect job annotations as well as conclusions: successful jobs must not retain deprecation warnings.

## Elide compatibility notes

JVM tests and JaCoCo XML/HTML reports come from `elide test --coverage`. The Python coverage checker preserves the 85% line and 70% branch thresholds and fails on absent or invalid aggregate counters. Declarative Elide thresholds are requested in [runtime#2691](https://github.com/elide-dev/runtime/issues/2691).

Python integration tests run through Elide's Python component using standard-library unittest. Native Python test discovery is tracked in [runtime#1475](https://github.com/elide-dev/runtime/issues/1475). Elide 1.6.1 does not expose the host environment to Python even with `--allow-env`; `scripts/python.sh` explicitly forwards only the build variables listed there. This gap is tracked in [runtime#2690](https://github.com/elide-dev/runtime/issues/2690).

Elide's embedded Native Image driver compiles both Myna and integration fixtures. Native access is explicitly enabled for its bundled libraries. ML profile inference is temporarily disabled because its inference engine fails to initialize even for Hello World; see [runtime#2692](https://github.com/elide-dev/runtime/issues/2692). Revisit the workaround when upgrading Elide. C/C++, LLVM/LLD, and Rust remain external tools for the native interoperability tests and object applicator.
