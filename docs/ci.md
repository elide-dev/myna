# CI and dependency maintenance

`on.push.yml`, `on.pr.yml`, and `on.queue.yml` run the same reusable JVM, native, and container workflows. The `Gate` job requires all of them to succeed, including on merge-queue events. Main builds are not cancelled by newer pushes; superseded PR builds are cancelled. Use the Gate check for branch rules when configuring required checks.

`check.jvm.yml` runs Elide JVM tests and coverage reports, followed by the coverage threshold checker. `check.native.yml` builds and exercises Myna on Linux and macOS, including LLVM and Native Image interoperability. `job.container.yml` builds and smoke-tests both container variants. `on.release.yml` alone grants package publication permissions, and publication additionally requires a `v*` tag and successful smoke tests. Manual runs on branches only build and test.

Actions are pinned to release commit SHAs; Docker base images include immutable digests. Linux jobs use runner hardening in audit mode. Checkout does not retain credentials. Codecov uses OIDC and the unified coverage/test-results action; uploads do not require a repository secret. Fork PR uploads use Codecov's public-repository tokenless behavior.

Dependabot checks Actions daily and Maven/Docker weekly. Maven compatibility-build plugin versions live in `pom.xml`. When updating the GraalVM SDK or JUnit, update the matching coordinates in `elide.pkl` too. The GraalVM SDK must match the external distribution pinned in `tools/graalvm.json`. Update Elide in both `mise.toml` and the Dockerfile, and regenerate `mise.lock`.

Elide, elide-dev/toolchain, Rust, CMake, and workflow linters are pinned in `mise.toml` and `mise.lock`. The shared setup action installs them with mise and installs Elide's Python component. No separate JDK or Python setup is required. Upgrade Rust only alongside verification that its LLVM major matches the LLVM 23 reader, writer, and linker; native CI checks this explicitly before building. The Ubuntu 24.04 and macOS 15 runners remain the supported platform baselines. Update OS baselines separately from library patches and verify both container libc compatibility and native linker behavior.

Before merging an update, run `mise run test`, `mise run lint`, and the container CI jobs. Inspect job annotations as well as conclusions: successful jobs must not retain deprecation warnings.

## Elide compatibility notes

JVM tests and JaCoCo XML/HTML reports come from `elide test --coverage`. The production-source coverage checker enforces 99% line and 93% branch thresholds and fails on absent or invalid aggregate counters. Declarative Elide thresholds are requested in [runtime#2691](https://github.com/elide-dev/runtime/issues/2691).

Python integration tests run through Elide's Python component using standard-library unittest. Native Python test discovery is tracked in [runtime#1475](https://github.com/elide-dev/runtime/issues/1475). The Python launcher does not expose the host environment to Python even with `--allow-env`; `scripts/python.sh` explicitly forwards only the build variables listed there. This gap is tracked in [runtime#2690](https://github.com/elide-dev/runtime/issues/2690).

The checksum-pinned elide-dev/graal 25.5.5-elide.24 Native Image driver compiles Myna and integration fixtures. Elide 1.6.2 embeds an older driver, so builds invoke the external driver explicitly. C/C++ and LLVM/LLD use elide-dev/toolchain 2026.11.1, including its sanitizer configurations. The SDK setup action caches verified distributions. Benchmark macro runners use host Python and pinned GraalVM directly because the Elide CLI currently faults on those ARM runners.

CodSpeed maintains Myna simulation and wall-time workloads. The separate boundary-comparison workflow runs uninstrumented comparisons on dedicated hardware. SVM generated versus handwritten Native Image C calls is the primary performance target; JVM results are secondary. See [benchmarking](benchmarking.md) for rerun commands and measurement limits.
