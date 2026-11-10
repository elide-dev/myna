# Truffle cell call costs

Run `mise run build:jvm`, `mise run bench:cells-build`, then `mise run bench:cells`. Results, the
Native Image build log, per-process samples with each process's Truffle compilation trace, input
and binary hashes, commands and toolchain identities are retained under `build/cells/`. The
`Truffle cell costs` workflow runs the same experiment on the hosted ARM machine. Use the same
machine and build for comparisons.

Every case is one fresh process running one language-less Truffle root (a `NativeCell` driven
through its `NativeFunction` wrapper from the host) that contains the whole measured loop. The
polyglot context compiles immediately, in the foreground, single tier, and fails on compilation
failure; every measured sample must observe `CompilerDirectives.inCompiledCode()` and the process
must print an `opt done` trace, otherwise the run fails. Ten warmup and ten retained samples per
process, five processes per case, seeded rotated case order, a current-thread Java heap allocation
counter with its own canary, and an independent Python oracle for every checksum match the other
harnesses. Controls are not subtracted.

Scalar cases run the dependent recurrence `x = x * 1664525 + 1013904223` through
`myna_bench_leaf` (binding ABI 2 leaf) or `myna_bench_step` (`native_leaf=false` transition) from
the shared comparison kernel, each call depending on the previous result:

- `control`: the recurrence in Java inside the compiled root.
- `direct-leaf`, `direct-step`: plain Java calls to the generated Native Image bindings, the floor
  a cell should match.
- `direct-ffm-leaf`, `direct-ffm-step`: plain Java calls to the generated AOT FFM bindings.
- `glue-*`: language-specific glue, a consumer node that adopts the generated cell as a child and
  calls `execute` directly, for the Native Image and FFM backends.
- `typed-*`: the same glue holding the concrete generated cell type and calling its primitive
  `executeLong`, so the result is never boxed and no `UnexpectedResultException` handler is needed.
- `interop-*`: the generic path, the cell's executable wrapper called through a dispatched
  `InteropLibrary`.
- `nfi-leaf`: Truffle NFI (libffi backend) bound to the same leaf symbol, the dynamic-FFI
  baseline. NFI is an internal language, so a two-method benchmark language (`BenchLanguage`)
  binds the symbol and hands the function to the host; it is the only DSL-processed source here.

- `hand-step`: `Cells.step` behind a hand-written `@TruffleBoundary`, the shape cells are compared
  with for transitioning imports; `glue-boundary-step` is the generated cell with
  `truffle_transitions=boundary`.

Slab cases call a ten-argument entry (two byte buffers, two opaque pointers, five integers),
exported as a leaf (`slab`) and as a transition (`slab-step`): `direct-*` passes raw addresses,
`invoke-*` calls the cell's static `invoke` with the same raw addresses,
`hand-slab-step` does so behind a hand-written boundary, `glue-*-native` passes `NativeBytes` and
`NativePointer` values through the cell, `glue-slab-heap` passes `HeapBytes` (both copied, since a
byte pointer without a `borrow` declaration is NUL-terminated), `glue-boundary-*` uses the
`boundary` cell and `interop-*` the executable wrapper.

Guest values (buffers, pointers, arrays) live in non-final fields of the measuring node, so partial
evaluation sees them as values, not constants, as it would for arguments arriving from guest code.

Borrow cases call `myna_bench_sum(bytes, count, salt)`, a leaf that adds the bytes of a pattern
buffer to the previous result, lending the same buffer on every call. Sizes are 16, 256, 1024,
4096 and 65536 bytes; call counts scale down with size so each sample sums a similar volume.

- `direct-sum-N`: a raw pointer from Java, the floor including the summing work.
- `glue-sum-native-N`: `NativeBytes` lent in place.
- `glue-sum-pin-N`: `HeapBytes` with `-Dmyna.truffle.pinThreshold=1`, so the array is pinned.
- `glue-sum-copy-N`: `HeapBytes` with the threshold above any size, so the array is copied.

At 256 bytes the same sum also runs with one array pinned by hand, behind a boundary (`hand-sum-pin`)
and visible to partial evaluation (`inline-sum-pin`), against the cell; `*-sum-step-*` repeats that
across a transitioning import, and `glue-sum-mixed` alternates native memory and a pinned heap
array at one call site.

`--cases REGEX` runs a subset; a partial run is marked as a smoke run.

String cases call `myna_bench_text(text, salt)` with an 11-byte string: `direct-text` passes a
native C string, `glue-text` lends a Java string (UTF-8, NUL-terminated copy per call), `nfi-text`
passes the string through NFI's `STRING` conversion.

Runtime-compiled code is not part of the image, so the image disassembly cannot show it; the
retained evidence for inlining is the compilation trace plus the measured parity with the direct
call and the zero allocation count. On Linux the benchmark's C file carries a weak `memfd_create`
syscall shim because the pinned toolchain's sysroot predates glibc 2.27 while the distribution's
static libffi (used by NFI) references it; a newer libc wins at link time.
