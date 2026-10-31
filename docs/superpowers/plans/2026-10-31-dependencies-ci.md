# Dependency and CI Refresh Implementation Plan

> Execute in this session with the existing build and integration suites as the validation gates.

**Goal:** Upgrade dependencies and GitHub Actions, follow Elide's reusable-workflow conventions, and finish with green, warning-free jobs on main.

**Architecture:** Separate event entrypoints (`on.*`) from reusable JVM, native, and container jobs. Use current stable releases with immutable action/image pins, scoped permissions, runner hardening, and Dependabot updates.

**Constraints:** Preserve Java 25 and LLVM 23 compatibility, both native platforms, coverage thresholds, all existing tests, and tag-only container publication. Keep Maven and Elide dependency declarations aligned. Do not publish packages during validation.

## Tasks

- [ ] Upgrade Maven dependencies/plugins, Elide, GraalVM, Rust, and container images. Centralize the dependency plugin version in Maven. Verify `mvn -B clean verify` and native builds/tests.
- [ ] Extract reusable workflows with push, PR, merge-queue, and release entrypoints. Update and pin all actions, replace deprecated Codecov uploads, scope permissions, and add timeouts and runner hardening. Validate with actionlint.
- [ ] Add Dependabot for Actions, Maven, and Docker, document the workflow/toolchain maintenance contract, and ensure future dependency changes are visible to CI.
- [ ] Review the complete diff, run the branch CI including both container variants, inspect check annotations, fix any failures/warnings, then integrate and confirm main CI.

## Review focus

- Fork/Dependabot PRs must build without publishing or requiring repository secrets.
- Merge queue must run the same checks and fail its gate on failed/cancelled dependencies.
- Container publication must require a version tag and successful smoke tests.
- Native bitcode producers and consumers must all use LLVM 23.
- Coverage and test results must still upload after failed tests when reports exist.

## Evidence

Baseline: main commit 685083e passed all 183 JVM tests and both native CI jobs in run 41207232883. Its warning annotations identify deprecated actions/setup-java v4, codecov/test-results-action v1, and Node 20 actions.
