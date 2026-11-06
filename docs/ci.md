# CI and dependency maintenance

`on.push.yml`, `on.pr.yml`, and `on.queue.yml` run the same reusable JVM, native, and container workflows. The `Gate` job requires all of them to succeed, including on merge-queue events. Main builds are not cancelled by newer pushes; superseded PR builds are cancelled. Use the Gate check for branch rules when configuring required checks.

`check.jvm.yml` runs Elide JVM tests and coverage reports, followed by the coverage threshold checker. `check.native.yml` builds and exercises Myna on Linux and macOS, including LLVM and Native Image interoperability. `job.container.yml` builds and smoke-tests both container variants. `on.release.yml` alone grants package publication permissions, and publication additionally requires a `v*` tag and successful smoke tests. Manual runs on branches only build and test.

Actions are pinned to release commit SHAs; Docker base images include immutable digests. Linux jobs use runner hardening in audit mode. Checkout does not retain credentials. Codecov uses OIDC and the unified coverage/test-results action; uploads do not require a repository secret. Fork PR uploads use Codecov's public-repository tokenless behavior.

Dependabot checks Actions daily and Maven, Cargo, Docker, chart dependencies, and benchmark submodules weekly. Maven compatibility-build plugin versions live in `pom.xml`. When updating the GraalVM SDK or JUnit, update the matching coordinates in `elide.pkl` too. The GraalVM SDK must match the external distribution pinned in `tools/graalvm.json`. Update Elide in both `mise.toml` and the Dockerfile, and regenerate `mise.lock`.

Elide, elide-dev/toolchain, Rust, CMake, and workflow linters are pinned in `mise.toml` and `mise.lock`. The shared setup action installs them with mise and installs Elide's Python component. No separate JDK or Python setup is required. Upgrade Rust only alongside verification that its LLVM major matches the LLVM 23 reader, writer, and linker; native CI checks this explicitly before building. The Ubuntu 24.04 and macOS 15 runners remain the supported platform baselines. Update OS baselines separately from library patches and verify both container libc compatibility and native linker behavior.

Before merging an update, run `mise run test`, `mise run lint`, and the container CI jobs. Inspect job annotations as well as conclusions: successful jobs must not retain deprecation warnings.

## Elide compatibility notes

JVM tests and JaCoCo XML/HTML reports come from `elide test --coverage`. The production-source coverage checker enforces 99% line and 93% branch thresholds and fails on absent or invalid aggregate counters. Declarative Elide thresholds are requested in [runtime#2691](https://github.com/elide-dev/runtime/issues/2691).

Python integration tests run through Elide's Python component using standard-library unittest. Native Python test discovery is tracked in [runtime#1475](https://github.com/elide-dev/runtime/issues/1475). The Python launcher does not expose the host environment to Python even with `--allow-env`; `scripts/python.sh` explicitly forwards only the build variables listed there. This gap is tracked in [runtime#2690](https://github.com/elide-dev/runtime/issues/2690).

The checksum-pinned elide-dev/graal 25.5.5-elide.24 Native Image driver compiles Myna and integration fixtures. Elide 1.6.2 embeds an older driver, so builds invoke the external driver explicitly. C/C++ and LLVM/LLD use elide-dev/toolchain 2026.11.2, including its sanitizer configurations. The SDK setup action caches verified distributions. Benchmark macro runners use host Python and pinned GraalVM directly because the Elide CLI currently faults on those ARM runners.

CodSpeed maintains Myna simulation and wall-time workloads. The separate boundary-comparison workflow runs uninstrumented comparisons on dedicated hardware. SVM generated versus handwritten Native Image C calls is the primary performance target; JVM results are secondary. See [benchmarking](benchmarking.md) for rerun commands and measurement limits.


`hygiene.yml` checks licensing metadata, local documentation links, chart input/output hashes, and workflow syntax. It renders the charts again using hash-locked dependencies. `dependency-review.yml` blocks newly introduced high/critical known vulnerabilities on pull requests. Scorecard runs on main and weekly, publishing its report and retaining the JSON. CodeQL is intentionally not configured.

Repository rulesets require pull requests, an approving review, resolved review threads, linear history, and passing `Gate`, `Repository hygiene`, and `Dependency review` checks on up-to-date branches. Version tags cannot be updated or deleted. Rulesets are applied after these checks are available; see `.github/rulesets/` for the exact configurations. Performance timing is not a required check on shared runners. Administrators can manage repository rules through GitHub; no workflow gets an admin credential.

Use [release verification](releases.md) for artifact attestations and SBOMs. Do not claim that a historical locally built artifact has CI build provenance simply by uploading it to CI.

## Scorecard evidence and remaining limits

OpenSSF Scorecard runs on main and retains its JSON report. Container validation
has explicit read-only permissions; only tag publication grants package and
attestation writes. The release entrypoint contains the actual publishing actions,
so permission exemptions do not depend on detecting a reusable workflow. Archive
publication retains its signed provenance bundle and a discoverable in-toto envelope.

Hadolint runs in the required Repository hygiene check on every PR, main push,
and merge group, including shell checks inside Dockerfile commands. Its action and underlying image are pinned. OS package versions follow
Ubuntu security updates within the pinned base release; DL3008 is the sole documented
exception. This is Dockerfile analysis, not a claim of comprehensive Java/C++ SAST.
Rust Clippy and workflow actionlint remain separate existing checks. CodeQL stays disabled.

The November 6, 2026 baseline was 6.1/10. Do not interpret improvements as a full
security audit: the score includes repository age, recent independent review history,
and best-practices badge certification. The repository must age past 90 days for
Maintained credit. Code-Review requires real independent reviews of final revisions;
admin merges do not earn that credit. Branch-Protection currently requires one
code-owner approval; its highest tier requires two independent approvals. The
Best Practices badge must be completed with truthful maintainer evidence, not just
added to the README. Historical unsigned releases continue to affect Signed-Releases;
never regenerate their bytes or manufacture CI provenance to improve the score.
