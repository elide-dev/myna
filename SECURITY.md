# Security policy

Report suspected vulnerabilities privately through [GitHub private vulnerability reporting](https://github.com/elide-dev/myna/security/advisories/new). Do not put exploitable details, private inputs, credentials, or crash dumps containing application data in public issues.

The latest published release is supported for security fixes. Older releases may receive fixes at maintainer discretion; consumers should upgrade to the latest compatible release. Maintainers will coordinate disclosure and publish advisories for confirmed vulnerabilities. This project does not promise a fixed response deadline.

Include the affected version/commit, platform and toolchain, a minimal reproduction, expected versus observed behavior, and the relevant ownership or ABI contract. Sanitizer diagnostics and reduced fuzz inputs are particularly useful. State whether the failure is in the generator, LLVM object application, native value producer/consumer, or generated bindings.

Generated optimizer and native-leaf contracts are assertions supplied by the application author. Myna validates their consistency, but cannot prove that arbitrary native code obeys them. Borrowed data must outlive every consumer, custom allocators must satisfy their contracts, and direct SVM memory views require valid addresses and lengths. Incorrect application contracts can cause memory unsafety; validation bypasses or unsafe behavior under valid contracts are in scope for private reports.

See [native validation](docs/benchmarking.md) for sanitizer/fuzzer commands and their limits. Native-library instrumentation does not imply that SVM's managed heap is instrumented.
