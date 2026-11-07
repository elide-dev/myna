# Contributing to Myna

Use the pinned tools described in [README](README.md). Run `mise install --locked` and `mise run setup`, then make changes on a branch and open a pull request. Keep discussions respectful and focused on the code and evidence.

Run `mise run lint` and the tests relevant to the change. `mise run test` runs the full suite. Run Elide builds and tests sequentially in one checkout because they share output directories. CI exercises Linux and macOS; native changes need real SVM checks, not just JVM tests. See [AGENTS.md](AGENTS.md) for implementation constraints and [benchmarking](docs/benchmarking.md) for coverage, sanitizers, and reproducible performance checks.

For ABI, allocator, or ownership changes, explain the compatibility impact and failure behavior. Regenerate golden outputs when versioned descriptors change. Add automated tests for major new functionality and include a minimal regression test for a bug fix. For performance claims, preserve raw samples, source and binary hashes, toolchain and CPU identity, and matched transition/ownership contracts. SVM direct-call behavior is the primary target; shared-host timing changes are advisory.

Keep commits focused. A PR should explain the observable problem, final behavior, validation, and remaining limitations. Do not commit secrets, local credentials, generated build trees, or unrelated downstream changes. Use the [security policy](SECURITY.md) for sensitive reports.

Myna is licensed under `Apache-2.0 OR MIT`, at the recipient's option. Contributions to Myna are submitted under those same alternatives unless explicitly stated and agreed otherwise. Preserve third-party copyright and license notices.

## Governance and responsibilities

Myna is maintained by the Elide organization. Sam Gammon ([sgammon](https://github.com/sgammon)) is the project lead and resolves technical or project-direction disputes after discussion in the relevant issue or pull request. Contributors propose changes through pull requests and retain copyright in their contributions. Maintainers review correctness, compatibility, security, tests, and documentation before accepting changes. Follow the [code of conduct](CODE_OF_CONDUCT.md) in all project spaces.

The repository administrators and release maintainers are [sgammon](https://github.com/sgammon), [darvld](https://github.com/darvld), and [TravisCow](https://github.com/TravisCow). They triage issues, handle private security reports, review changes, maintain CI, and publish releases using the [release process](docs/releases.md). CODEOWNERS identifies the required reviewers; these responsibilities do not override repository protections. Contributors should update documentation with behavior changes and link newly attained project achievements from the README within 48 hours.

All three maintainers have repository administration access. On 2026-11-06 the project lead confirmed that both other administrators can operate releases and recover the required external publishing credentials. If any maintainer becomes unavailable, either remaining administrator takes over issue handling, reviews, and releases within one week of confirmation. Recovery instructions stay in the organization's private credential system; never put secrets in issues or this repository. Review access and recovery whenever a maintainer joins or leaves. No individual maintainer's workstation is required to build or release from CI.

## Coding standards

Contributions must follow Google Java Style as enforced by checksum-pinned google-java-format 2.0.0, LLVM C/C++ style with the overrides in `.clang-format`, and Rust Style as enforced by the pinned rustfmt. Run `mise run format` to apply these styles and `mise run check:style` to check them. The gate covers maintained Java source/test sets, the LLVM applicator, C/C++ SDK headers, and workspace Rust. Generated golden outputs and third-party fixtures retain their generator/upstream formatting and are checked by their own tests.

Production Java must compile with `-Xlint:all -Werror`; the LLVM applicator uses `-Wall -Wextra -Wpedantic -Werror`; Rust Clippy runs with `-D warnings`. Fix warnings or document a narrowly scoped suppression next to the affected code. Native security analysis uses the pinned Clang analyzer's checks through `.clang-tidy`; do not disable a check globally to hide an unexplained finding. Run `mise run check:java` and `mise run check:native` before proposing changes to the respective sources. CI enforces these commands. Report suspected exploitable findings through the security process.

## Dependency maintenance

Dependabot security alerts and security updates are enabled; dependency review runs on pull requests. The version-update schedules in `.github/dependabot.yml` cover Cargo, Maven, Actions, containers, benchmark packages and submodules. Maintainers must triage security alerts and either update affected components or document why the reported vulnerability is not exploitable in Myna. Before a release, and at least monthly, also review upstream security notices for the checksum-pinned GraalVM/toolchain bundles, Java formatter, and simdjson conformance fixture in `tests/values_cpp.py`; these explicit downloads are not all covered by package-manager alerts. Record the versions, advisories, and disposition in the release checklist or an issue. Keep their version and checksum declarations together when updating.

## Source licensing notices

Maintained Java, Rust, C/C++, Python, shell, Gradle, Pkl, Docker and CMake source files carry a copyright notice and `SPDX-License-Identifier: Apache-2.0 OR MIT`. New files must preserve this convention. Run `python3 scripts/source-notices.py` to check tracked sources or add `--fix` to insert missing notices; shebangs remain on the first line. Repository hygiene enforces the check.

Vendored sources retain their upstream licenses. Generated seam goldens and archived benchmark source snapshots retain their original bytes and are excluded from this maintained-source check. This check alone does not establish the gold badge requirement for notices in every source file, including generated distributions.
