# trufflecheck baseline

This is the first report from [trufflecheck](https://github.com/elide-dev/trufflecheck) over Myna. Nothing has
been fixed yet. The report records where the code stands, which findings are real, and the order in which to work
through them. Run it again with `mise run check:trufflecheck` (see [Running the check](#running-the-check)).

## What was run

- **Tree measured:** an unpushed local branch that merges `origin/stack/batch-borrows` (#143, `d720aaf`) with
  `origin/stack/truffle-batch-cells` (`6dd8798`), `origin/stack/nativebytes-scope` (`bf04de2`),
  `origin/stack/ffm-typed-pointers` (`05cb766`), `origin/stack/generator-perf` (`e470eac`) and
  `origin/stack/ownership-leftovers` (`0a5f0f4`). The merge result is `d546dad`. Two merge conflicts were resolved
  only for this measurement: `docs/releases/unreleased.md` (both sides kept) and `JavaFfm.java`, where both the
  `slotRead` and `heldConstructor` methods were kept. `elide build` of all four JARs succeeded on the merge.
- **trufflecheck 0.1.1**, as pinned downloads (not a git submodule):
  - `trufflecheck-errorprone-0.1.1.jar`, sha256 `d2c57d8ba701f81ff07383ce4bbeed0905b653f256a6f860da10ecb608d21dde`.
    The copy on maven.elide.dev is byte-identical to the release asset.
  - `trufflecheck-profiles-0.1.1.tar.gz`, sha256 `c815ce50b4930e97a6e5678387d62aa6f6705032222b923489a907f5519a3882`.
- **Provenance:** `gh attestation verify trufflecheck-errorprone-0.1.1.jar --repo elide-dev/trufflecheck
  --signer-workflow elide-dev/trufflecheck/.github/workflows/release.yml --predicate-type https://slsa.dev/provenance/v1`
  passed. The signer was `elide-dev/trufflecheck/.github/workflows/release.yml@refs/heads/main` at
  source commit `e467e7ca0f4c1c17813e6710ec7461e8762d478c`, in run 43958808013. The profiles tarball also verifies.
- **Analyzers,** pinned by sha256 in [`tools/trufflecheck.json`](../tools/trufflecheck.json):
  - Error Prone 2.51.0 (`error_prone_core-2.51.0-with-dependencies`, `9a6aee2a…`);
  - `dataflow-errorprone` 3.41.0-eisop1 (`10434fba…`);
  - NullAway 0.14.3 (`440d3217…`) with `dataflow-nullaway` 4.3.1 (`dc7b9ced…`);
  - JSpecify 1.0.1 (`070d75f2…`).

  Truffle and the SDK are the existing `tools/truffle.json` pins (Elide GraalVM 25.5.5-elide.28).
- **Compilers and tools:** javac 25.0.4 from the pinned Elide GraalVM; clippy 0.1.101 (nightly-2026-10-09); and
  cargo-deny 0.20.2, now pinned in `mise.toml`.
- **Java:** the complete `profiles/jvm/errorprone.args` (every trufflecheck rule at its default severity, plus
  NullAway and the profile's Error Prone checks), with the policy in
  [`trufflecheck.properties`](../trufflecheck.properties). The check compiles five sets:
  - **runtime:** `src/truffle/java`, with the Truffle DSL processor. DSL output is excluded.
  - **cells:** the generated `CellApiTruffle.java` from three fixtures: `tests/truffle/cells.seam` (SVM),
    `tests/truffle/cells.seam` with `truffle_backend=ffm` (FFM), and `examples/truffle.seam` (SVM). Each is compiled
    with the binding sources it calls, as `tests/truffle_integration.py` does. Findings in those binding sources
    (`CellApi.java`, `CellApiFFM.java`, `CellApiOwnership.java`, `CellApiDirectives.java`) are counted separately,
    as **cell-bindings**.
  - **java:** `src/main`, `src/values` and `src/feature`.
- **Rust:**
  - `cargo clippy --workspace --all-targets` with `profiles/rust/clippy.toml` (through `CLIPPY_CONF_DIR`), and every
    lint in `profiles/rust/lints.toml` passed as `-W`. Each finding keeps the level the profile gives it, so `deny`
    is reported as an error. The workspace's own lint configuration is unchanged.
  - The same flags through `clippy-driver` over the generated `seam.rs` of `tests/ownership/ownership.seam` and
    `tests/truffle/cells.seam`, each compiled as a lib.
  - `cargo deny check` with `profiles/rust/deny.toml`.

## Totals

| Area | Errors | Warnings |
| :--- | ---: | ---: |
| Truffle runtime (`src/truffle`) | 51 | 79 |
| Generated cells (`*Truffle.java`) | 7 | 20 |
| Generated cell bindings (`CellApi*.java`, not cells) | 515 | 114 |
| Fixture source (`tests/truffle/Directives.java`) | 0 | 2 |
| Generator, values and Feature Java | 108 | 122 |
| Rust workspace (clippy) | 50 | 118 |
| Rust workspace (cargo-deny) | 0 | 0 |
| Generated `seam.rs` (ownership and cells) | 119 | 213 |
| **Total** | **850** | **668** |

The headline count is misleading. Of the 850 errors, 614 are `TruffleStackfulException` outside compiled code, and
168 are `unsafe_code` in FFI code, which the profile expects to carry a per-crate `allow` with a reason. **Ten
runtime errors and four cell errors need a code change or a closer look; the others need policy or suppression.**

cargo-deny is clean: no advisories, bans or source findings, and no licence errors (25 licence `help` and 10 `note`
diagnostics only).

## Findings by area

Triage key: **TP** true positive, **FP** false positive (with the reason), **INV** needs investigation. Paths are
relative to `src/truffle/java/dev/elide/myna/truffle/` for the runtime, and to `build/trufflecheck/cells/<fixture>/`
for generated code. Line numbers are from `d546dad`.

### Truffle runtime: 51 errors, 79 warnings

| Rule | Severity | Count |
| :--- | :--- | ---: |
| `TruffleStackfulException` | error | 28 |
| `TruffleSkippedException` | error | 7 |
| `TruffleMissingBoundary` | error | 5 |
| `MustBeClosedChecker` | error | 4 |
| `TruffleChildWriteWithoutInsert` | error | 3 |
| `TruffleStaticContextState` | error | 3 |
| Error Prone crash (`MustBeClosedChecker`) | error | 1 |
| `RequireExplicitNullMarking` | warning | 32 |
| `ReturnMissingNullable` | warning | 25 |
| `OperatorPrecedence` | warning | 7 |
| `FieldMissingNullable` | warning | 6 |
| `UnusedException` | warning | 6 |
| `NonFinalStaticField`, `ReferenceEquality`, `UnusedVariable` | warning | 1 each |

**Errors:**

- **`TruffleChildWriteWithoutInsert`** (3):
  - `AlignedBytesCoercion.java:256` and `BytesCoercion.java:234`: **FP.** The code is
    `guestText = node = insert(new GuestText())`, so the child is adopted. The rule doesn't follow a chained
    assignment.
  - `Coercion.java:32`: **FP.** The code is `library = insert(…); interop = library;`, so the child is adopted
    through a local. The rule doesn't follow the local.
  - Both shapes are worth reporting upstream.
- **`TruffleSkippedException` + `TruffleStackfulException` + `TruffleMissingBoundary`** in node constructors
  (`AlignedBytesCoercion.java:79,80`, `BytesCoercion.java:76`, `HeldBytesCoercion.java:53`,
  `IntegerCoercion.java:27`, `OverAlignedBytesCoercion.java:61,62`):
  - **FP in practice.** These are argument checks that run when the generator's cell builds the node, never on a
    partially evaluated path.
  - The rule treats a node constructor as compiled code, because a node allocated inside a specialization would be.
  - Fix cheaply: either add `CompilerAsserts.neverPartOfCompilation()` at the head of the check, or suppress with a
    reason.
- **`TruffleMissingBoundary`** at `NativeCellRootNode.java:42`: **FP.** It flags `toString()` on a `RootNode`, which
  partial evaluation never reaches. Mark it `@TruffleBoundary` to make that explicit. That costs nothing.
- **`TruffleStaticContextState`** (3):
  - `BatchCoercion.java:168` (`Sentinel.THREAD`): **FP by design.** It is a never-started thread used only as an
    identity for which no confined segment is accessible. It is initialized at run time, and holds no context.
  - `Borrow.java:608` (`Scratches.CURRENT`, a `ThreadLocal`): **INV, probably intended.** The scratch stack is
    per-thread native memory, not per-context state, and myna-truffle has no `TruffleLanguage`, so
    `ContextThreadLocal` isn't available. Confirm that sharing across contexts on one thread is safe, then suppress
    with a reason.
  - `NativeBytes.java:72` (`unowned`, lazily created, and also `NonFinalStaticField`): **INV.** The lazy write is
    unsynchronized. A race only creates a second unstarted thread, but the field should move to a run-time
    initialized holder, like `Sentinel`.
- **`TruffleStackfulException`,** the other 21 sites:
  - **TP by the rule's definition, none on a compiled path.** They are:
    - `AssertionError` after `transferToInterpreter()` (`AlignedBytesCoercion.java:295`,
      `OverAlignedBytesCoercion.java:184`);
    - `@TruffleBoundary` copies and misuse diagnostics (`Borrow.java:275-284`, `:934`, `:939`);
    - API-misuse checks on host-facing factories (`Borrow.java:371`, `NativeBytes.java:112,113,119`,
      `NativeConstants.java:47-59`, `NativeResource.java:216,328`);
    - the static endianness guard (`NativeBytes.java:38`);
    - causes built for `NativeCellException.memory` (`NativeBytes.java:202,204`).
  - Settle a policy: stackless subclasses for errors a guest can trigger, and a reasoned suppression for
    programming errors.
- **`MustBeClosedChecker`** at `Borrow.java:504,515,528,555`: **FP.** Each `Arena` is handed to a `Copy` that owns
  it and closes it on release. That ownership transfer is deliberate.
- **Error Prone crash** at `NativeStruct.java:83`:
  - `MustBeClosedChecker` threw `IllegalArgumentException` while building its suggested fix for
    `shared ? Arena.ofShared() : Arena.ofConfined()`. Two fixes edit the same range. javac stops at the crash.
  - The check reruns the compilation without that one check, so the classes after it are still analyzed.
    Every other check still sees every file. Only `MustBeClosedChecker` stops covering the classes after the crash.
  - **Upstream Error Prone bug,** not a Myna finding. Report it with this ternary as the reproducer.

**Warnings:**

- Nullness, 63 in total: `RequireExplicitNullMarking`, `ReturnMissingNullable` and `FieldMissingNullable`.
  - **TP, adoption backlog.** Myna has no `@NullMarked` code, so NullAway (`OnlyNullMarked=true`) checks nothing
    yet.
  - Mark `dev.elide.myna.truffle` `@NullMarked` in `package-info.java` before fixing these one by one. JSpecify
    would become a compile-only dependency.
- `OperatorPrecedence`, `Borrow.java:874-898` (7): **TP, style.** These are mixed `&`/`|`/shift expressions in the
  scratch-offset arithmetic. Parenthesize them.
- `UnusedException` (6), at `BatchCoercion.java:87,98,138`, `Borrow.java:431`, `HeldBytesCoercion.java:126` and
  `NativeStruct.java:159`: **INV.** Each rethrows without the cause. For guest-facing errors that may be deliberate,
  but chaining the cause costs nothing off the fast path.
- `ReferenceEquality` at `NativeResource.java:263`: **FP.** It is a deliberate identity check before
  `addSuppressed`, which rejects self-suppression.
- `UnusedVariable` at `Borrow.java:74` (`LENT` is never read): **TP.** Remove it or use it.

### Generated cells: 7 errors, 20 warnings

The generator emits these, from `TruffleCells.java`, so each finding repeats once per fixture.

- **`TruffleGenericCatch`** at `cells/CellApiTruffle.java:890,1783` and `cells-ffm/CellApiTruffle.java:1308,2818`
  (4 errors), with **`Finally`** warnings at the next line in each:
  - **TP.** A cell that creates a resource releases its borrow in `finally`, catches `RuntimeException` from
    `release()`, and throws `NativeResource.closeAfterFailure(owner$, failure)` from the `finally`.
  - On the compiled path, that catch also takes `ControlFlowException` and `AbstractTruffleException`, and the
    throw from `finally` discards any exception already propagating from `invoke`.
  - Narrow the catch to what `Borrow.release()` throws, and don't throw from `finally` while another exception is
    in flight. **Fix this first among the generated code.**
- **`TruffleStackfulException`** at `cells/CellApiTruffle.java:91`, `cells-ffm/CellApiTruffle.java:94` and
  `example/CellApiTruffle.java:41` (3 errors): **TP, low impact.** The `IllegalArgumentException("no Truffle cell")`
  comes from the host-side name lookup, not from compiled code. Suppress it with a reason, or make it stackless.
- **`UnusedVariable` `adopted`,** at `cells/CellApiTruffle.java:984,1763` and `cells-ffm/CellApiTruffle.java:1504,2798`
  (4): **TP.** Out-struct creates set `adopted = true` but never read it, so the generator leaves a dead local.
- **`MutablePublicArray` `CELLS`** (3): **TP, minor.** A public `static final String[]` can be mutated. Expose an
  unmodifiable list or a copy.
- Nullness (9): **TP, backlog,** as for the runtime.
- No `TruffleMissingBoundary`, `TruffleNodeChildren`, `TruffleCompilationFinal*`, `TruffleFrameEscape` or
  `TruffleHostValueInImageHeap` findings in any cell. The generated cells pass the partial-evaluation rules.

### Generated cell bindings: 515 errors, 114 warnings

These are the binding classes the cells call (`CellApi`, `CellApiFFM`, `CellApiOwnership`, `CellApiDirectives`).
They aren't Truffle code, but FFM cells call `CellApiFFM` methods from compiled code.

- **`TruffleStackfulException`** (508; 467 in `CellApiFFM.java`):
  - 310 are `UnsatisfiedLinkError`s. Most are in `orElseThrow` lambdas for symbol lookup and `downcallHandle`
    linking. The others are `if (this.<symbol>_address == null)` guards for a missing symbol. **FP in practice:**
    they fire only when a symbol is absent, and never on a successful call.
  - 157 are argument validation in FFM wrappers: `IllegalArgumentException("…")`, `!isNative()`,
    `!scope().isAlive()`, `WrongThreadException`, `isReadOnly()`, and null-address checks. **INV:** check whether FFM
    cells validate before calling these wrappers. If a guest value can reach them in compiled code, each throw
    deoptimizes and fills in a stack trace.
  - The other 41 are batch and ownership guards in `CellApiOwnership.java` (39) and `CellApiDirectives.java` (2).
    **TP by the rule, off the fast path.**
- **`TruffleStaticContextState`** at `cells-ffm/CellApiFFM.java:2196,2729` (`BatchSentinel.THREAD`): **FP by design,**
  the same never-started sentinel as the runtime's.
- **`ComparisonOutOfRange`** at `cells/CellApiOwnership.java:406,425` and `cells-ffm/CellApiFFM.java:1907,2615`: **TP.**
  The generator emits `(long) count > Long.MAX_VALUE` when the batch count's ABI type is 64-bit, so the check is
  dead. Skip it when the ABI range covers `int`.
- **`StringJoin`** at `example/CellApiDirectives.java:141`: **TP, harmless.** `String.join("", HEADER_0)` with a
  single chunk returns it unchanged. The generator can write the constant directly.
- Warnings:
  - Nullness (101): backlog.
  - `ReferenceEquality` (7) and `ReachabilityFenceUsage` (4): **INV.**
  - `SameNameButDifferent` (2): **FP.** The generated nested type shadows a seam name on purpose.

### Generator, values and Feature Java: 108 errors, 122 warnings

- **`TruffleStackfulException`** (106): **FP for this area.** The generator, CLI and value readers aren't Truffle
  code (43 in `ValueTape.java`, 14 in `Main.java`, 8 in `Dsl.java`, and others). The rule applies everywhere unless
  the policy excludes paths. Add a `stackful.exclude` for `src/main/`, `src/values/` and `src/feature/` to
  `trufflecheck.properties`. The value readers that run in images can opt back in later if needed.
- **`DefaultLocale`** at `src/main/java/dev/elide/myna/backend/NativeImageDirectives.java:216,217`: **TP, no
  behaviour change.** It flags `String.format("\\%03o")` and `("\\u%04x")`. Octal and hex aren't localized, but
  `Locale.ROOT` states that and silences the check.
- Warnings:
  - `MissingOverride` (34), `StringSplitter` (11), `UnusedVariable` (3), `UnusedMethod` (1) and
    `LoopOverCharArray` (1): **TP, hygiene.**
  - Nullness (60): backlog.
  - `ReferenceEquality` (8) and `BadInstanceof` (2): **INV.**
  - `UnusedException` (2): **INV.**
- No `DefaultCharset`, `ClassInitializationDeadlock`, `ThreadLocalUsage` or `TruffleHostValueInImageHeap` findings.

### Rust workspace: 50 errors, 118 warnings

- **`unsafe_code`** (49 in total): **TP under the profile.**
  - In `crates/myna-values`: 17 in `src/text.rs`, 3 in `src/lib.rs` and 1 in `src/builder.rs`.
  - In tests: 16 in `tests/values.rs` and 4 in `tests/support/tape_model.rs`.
  - In `tests/values/src/lib.rs`: 8.
  - The profile expects FFI crates to declare `#![allow(unsafe_code, reason = "…")]`. Add it to `myna-values` and
    the test crate. It changes no code.
- **`clippy::panic`** at `crates/myna-values/tests/values.rs:52`: **FP.** It is a `panic!` in a test helper, in a
  test target, but outside `#[test]`, so `allow-panic-in-tests` doesn't cover it.
- `clippy::unwrap_used` (17) and `clippy::expect_used` (2):
  - **TP.** In library code: `src/lib.rs:101,278` and `src/text.rs:52`. These are invariant `expect`/`unwrap` calls,
    such as "serializer emits UTF-8". Prove them away or comment them.
  - The rest are in `benches/` and test support, where they are acceptable. Scope the allowance.
- Casts (`cast_possible_truncation`, `cast_sign_loss`, `cast_possible_wrap`, `cast_ptr_alignment`), 53: mostly in
  tests. **INV** for the 14 in `src/` (`lib.rs`, `ser.rs`, `text.rs`, `builder.rs`): each is either a checked
  narrowing or needs `try_from`.
- `undocumented_unsafe_blocks` (4) and `multiple_unsafe_ops_per_block` (10): **TP.** Add `// SAFETY:` comments and
  split the blocks.
- `borrow_as_ptr` (22), `wildcard_imports` (2), `unreachable_pub` (5), `clone_on_ref_ptr` (2) and
  `let_underscore_must_use` (1): **TP, mechanical.**

### Generated seam.rs: 119 errors, 213 warnings

The counts cover two files, `ownership/seam.rs` and `cells/seam.rs`.

- **`unsafe_code`** (119): **TP under the profile.** It is inherent in a raw FFI module. Generated code should carry
  a scoped `#[allow(unsafe_code, reason = "raw ABI binding")]` on each module or item. An inner attribute can't
  carry it, because implementers `include!` the file.
- **`clippy::expect_used`** (44): **TP, and important.**
  - Safe wrappers convert buffer lengths with `try_into().expect("buffer length exceeds ABI range")`, for example
    at `ownership/seam.rs:504`.
  - A panic here aborts the process when the Rust side is itself behind `extern "C"`.
  - Return the seam's error status instead. This is the highest-priority Rust item.
- **`undocumented_unsafe_blocks`** (88) and `multiple_unsafe_ops_per_block` (4): **TP.** Emit a `// SAFETY:` line
  naming the contract, such as length, alignment and lifetime, for each call.
- **`non_snake_case`** (43): **FP for generated code.** The wrapper names follow the seam's symbol names, such as
  `surfaceOpen`. Emit `#[allow(non_snake_case, reason = "…")]`.
- `allow_attributes_without_reason` (4): **TP.** Add reasons to the generated `#[allow(unused_macros)]`.
- Casts (10), `borrow_as_ptr` (4), `ptr_as_ptr` (2), `ptr_cast_constness` (3), `len_zero` (5),
  `unnecessary_cast` (2) and `useless_conversion` (4): **TP, mechanical generator cleanups.**

## Order to work through them

1. **Generated cells' compiled path:** `TruffleGenericCatch` and `Finally` in resource-creating cells
   (`TruffleCells.java`). They can swallow guest exceptions and control flow on the fast path. Regenerate the golden
   outputs together afterwards.
2. **Runtime errors needing a change:**
   - Settle `Borrow.Scratches.CURRENT` and `NativeBytes.unowned` (static context state, and the racy lazy static).
   - Mark `NativeCellRootNode.toString` as a boundary.
3. **FFM binding validation reachable from cells** (INV above): confirm whether cells validate first, and make the
   reachable throws stackless.
4. **Policy and suppression pass,** with no code change:
   - Add `stackful.exclude` for generator, values and Feature paths.
   - Add reasoned suppressions for the constructor checks, `Sentinel`/`BatchSentinel`, `MustBeClosedChecker`
     ownership transfers, and the cell-name lookup.
   - Report the `TruffleChildWriteWithoutInsert` shapes and the constructor-as-compiled-code case to trufflecheck,
     and the `MustBeClosedChecker` crash to Error Prone.
5. **Generated `seam.rs`:** replace the length `expect`s with error returns, then emit SAFETY comments and scoped
   `allow`s for `unsafe_code` and `non_snake_case`.
6. **Generated binding hygiene:** dead `ComparisonOutOfRange` checks, the single-chunk `String.join`, the unused
   `adopted`, and the public `CELLS` array.
7. **Rust workspace:** add the crate-level `unsafe_code` allows with reasons, SAFETY comments, and the library
   `expect`/`unwrap` calls; then adopt `[workspace.lints]` and `clippy.toml` from the profile.
8. **Nullness adoption** (about 230 warnings): add `@NullMarked` package by package, starting with
   `dev.elide.myna.truffle`.
9. **Gate the check:** once the error count is zero after policy, drop the report-only mode and add it to `lint`.

## Running the check

```sh
mise run check:trufflecheck   # builds the JVM jars first; never fails on findings
```

The check runs `scripts/trufflecheck.py` and writes its output to `build/trufflecheck/`:

- `summary.md`, with counts per area, rule and severity, and every finding;
- `findings.json`, with the same findings plus metadata: the commit, sha256s, the attestation result and tool
  versions;
- the raw output of each tool: `errorprone-*.log`, `clippy-*.json` and `cargo-deny.json`.

The run fails only on a tool error: a checksum mismatch, a failed attestation, a missing tool, or a check that crashes
twice. Provenance checks fail closed:

- Every download is checked against its sha256 pin before anything runs.
- Artifacts marked `attested` in `tools/trufflecheck.json` (the trufflecheck JAR and profiles) must also pass
  `gh attestation verify`, with the release workflow and SLSA predicate pinned.
- A missing `gh` or a failed verification stops the run. `--skip-attestation`, or
  `TRUFFLECHECK_SKIP_ATTESTATION=1`, opts out. The run then prints a warning, and `summary.md` and `findings.json`
  record that provenance wasn't verified.
- Error Prone, NullAway and JSpecify come from Maven Central without GitHub attestations, so they rely on their
  sha256 pins. Downloads are cached in `$TRUFFLECHECK_CACHE`. Without it, the cache is `/build/caches/trufflecheck` when
`/build/caches` exists, and `$XDG_CACHE_HOME/trufflecheck` otherwise. Attestation results are cached beside each artifact,
per digest and signer. Pass `--skip-rust` for the Java checks alone.

trufflecheck is a pinned download rather than a git submodule. Myna needs only the Error Prone JAR and the profiles
tarball. Both are release assets with build provenance, and the sha256 pins in `tools/trufflecheck.json` make the
inputs exact without a submodule checkout. Nothing is vendored into the repository. The agent skill's marketplace entry is pinned to the same
release (`"ref": "v0.1.1"`); bump that `ref` together with the version and sha256s in `tools/trufflecheck.json`.

### As a CI job

The check isn't in CI yet. A non-blocking job on Linux x64 would run:

```yaml
trufflecheck:
  runs-on: ubuntu-latest
  continue-on-error: true            # report-only until the baseline is cleared
  permissions: { contents: read, attestations: read }
  steps:
    - uses: actions/checkout@<pinned sha>
    - uses: jdx/mise-action@<pinned sha>
    - run: mise run setup
    - run: mise run check:trufflecheck
      env:
        GH_TOKEN: ${{ github.token }}   # gh attestation verify
        TRUFFLECHECK_CACHE: ${{ runner.temp }}/trufflecheck
    - uses: actions/upload-artifact@<pinned sha>
      with: { name: trufflecheck, path: build/trufflecheck/*.{md,json,log} }
```

Cache `TRUFFLECHECK_CACHE`, keyed on `hashFiles('tools/trufflecheck.json', 'tools/truffle.json')`. To make the job
gate merges later, the step would compare `findings.json` against a checked-in allowlist, then fail on new errors.
