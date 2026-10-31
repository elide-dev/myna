# CI and dependency maintenance

`on.push.yml`, `on.pr.yml`, and `on.queue.yml` run the same reusable JVM, native, and container workflows. The `Gate` job requires all of them to succeed, including on merge-queue events. Main builds are not cancelled by newer pushes; superseded PR builds are cancelled. Use the Gate check for branch rules when configuring required checks.

`check.jvm.yml` runs Maven tests and coverage thresholds. `check.native.yml` builds and exercises Myna on Linux and macOS, including LLVM and Native Image interoperability. `job.container.yml` builds and smoke-tests both container variants. `on.release.yml` alone grants package publication permissions, and publication additionally requires a `v*` tag and successful smoke tests. Manual runs on branches only build and test.

Actions are pinned to release commit SHAs; Docker base images include immutable digests. Linux jobs use runner hardening in audit mode. Checkout does not retain credentials. Codecov uses OIDC and the unified coverage/test-results action; uploads do not require a repository secret. Fork PR uploads use Codecov's public-repository tokenless behavior.

Dependabot checks Actions daily and Maven/Docker weekly. Maven plugin versions, including the dependency plugin used by the native tests, live in `pom.xml`. When updating the GraalVM SDK or JUnit, update the matching coordinates in `elide.pkl` too. The GraalVM SDK, native CI distribution, and Docker builder must stay on a compatible release line. Elide's version and download URL must be updated together.

Rust is pinned in `rust-toolchain.toml`. Upgrade Rust only alongside verification that its LLVM major matches the LLVM 23 reader, writer, and linker; native CI checks this explicitly before building. The Ubuntu 24.04 and macOS 15 runners remain the supported platform baselines. Update OS baselines separately from library patches and verify both container libc compatibility and native linker behavior.

Before merging an update, run `mvn -B clean verify`, `actionlint`, and the native/container CI jobs. Inspect job annotations as well as conclusions: successful jobs must not retain deprecation warnings.
