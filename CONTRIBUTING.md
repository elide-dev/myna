# Contributing to Myna

Use the pinned tools described in [README](README.md). Run `mise install --locked` and `mise run setup`, then make changes on a branch and open a pull request. Keep discussions respectful and focused on the code and evidence.

Run `mise run lint` and the tests relevant to the change. `mise run test` runs the full suite. Run Elide builds and tests sequentially in one checkout because they share output directories. CI exercises Linux and macOS; native changes need real SVM checks, not just JVM tests. See [AGENTS.md](AGENTS.md) for implementation constraints and [benchmarking](docs/benchmarking.md) for coverage, sanitizers, and reproducible performance checks.

For ABI, allocator, or ownership changes, explain the compatibility impact and failure behavior. Regenerate golden outputs when versioned descriptors change. Add automated tests for major new functionality and include a minimal regression test for a bug fix. For performance claims, preserve raw samples, source and binary hashes, toolchain and CPU identity, and matched transition/ownership contracts. SVM direct-call behavior is the primary target; shared-host timing changes are advisory.

Keep commits focused. A PR should explain the observable problem, final behavior, validation, and remaining limitations. Do not commit secrets, local credentials, generated build trees, or unrelated downstream changes. Use the [security policy](SECURITY.md) for sensitive reports.

Myna is licensed under `Apache-2.0 OR MIT`, at the recipient's option. Contributions to Myna are submitted under those same alternatives unless explicitly stated and agreed otherwise. Preserve third-party copyright and license notices.
