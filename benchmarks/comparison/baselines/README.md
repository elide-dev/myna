# Scalar baselines

These five-process runs preserve raw per-process samples, commands, binary hashes,
compiler evidence and runtime metadata. They were measured on a shared Apple M5
Max workstation with substantial concurrent activity. Treat them as diagnostic
baselines; the wide process ranges prohibit small percentage performance claims.
Use the separate `Boundary comparisons` CI workflow for rerunnable hosted ARM data;
configure a compatible controlled runner before treating timing changes as gates.

`before-unbound-arm64` records the runtime-bound FFM adapter. `unbound-arm64`
records the generated build-time adapter, an equivalently optimized handwritten
adapter, and an explicitly named runtime-bound diagnostic case. Workload profile
versions differ because the handwritten comparison was expanded; `compare.py`
intentionally rejects comparing these two profiles automatically.

The original build revision and dirty state remain in each record.
`verified_source_commit` identifies the later commit whose recorded source hashes
were verified byte-for-byte. Reports can be regenerated with `run.py report`.

The generated AOT FFM median changed from about 2056 ns/call to 4.67 ns/call.
The optimized handwritten median was 6.08 ns/call, with overlapping process ranges.
Generated direct leaf calls were about 1.06 ns/call. These establish the large AOT
adapter improvement and absence of a demonstrated generator penalty at this noise
level; they do not establish a universal minimum or a portable percentage advantage.


`default-leaf-2026-11-10-linux-x86_64` and `scalar-2026-11-10-linux-x86_64` are the November 10, 2026 unclemax (Threadripper PRO 9965WX) runs at `4f88a77` that drive the README's floor and boundary charts; the Truffle cell run from the same session is in `../../cells/baselines/`. `default-leaf-0.16.0-linux-x86_64` and `scalar-0.16.0-linux-x86_64` are the same host's runs of release 0.16.0 from earlier that day, kept as the record of the AOT FFM gaps that change fixed. See [their measurement record](../evidence/readme-2026-11-10/README.md). They are local measurements, without a CI provenance claim.

`hosted-arm64-0.7.2` is the earlier five-process CI baseline for release source `7401c446ef9ad80d7bd14a9bf92f360a04956b80`. It includes full ARM CPU identity and points to the originating workflow. It drove the SVM charts before the November 10 rerun; it is still a shared-host measurement, not a controlled-hardware timing gate.

The `leaf-floor-linux-arm64` and `leaf-floor-macos-arm64` JSON/Markdown pairs add matched native direct/indirect calls, identity functions, inlineable typed pointers, explicit unrolling and bounded batches. They use the same linked kernel objects within each build, retain all measured variants, and include separate CPU/build provenance. These experiments are not comparable as absolute timings across hosts or with the earlier scalar-only baselines. See [the floor harness](../../floor/README.md).

## Scalar adapter matrix

[`adapters-macos-arm64.json`](adapters-macos-arm64.json) retains the local
`scalar-recurrence-v3` run on the pinned toolchain and Apple M5 Max. It contains
28 runtime/case combinations, five fresh processes each, ten warmup rounds and
ten measured rounds of 1,000,000 dependent scalar calls per process. The recorded
base revision, dirty worktree, source hashes, executable hashes, commands and CPU
identity are preserved. This is local build evidence, without a CI provenance
claim. The [table](adapters-macos-arm64.md) includes every measured case.

Generated SVM normal calls measured 3.879 ns/op versus 3.938 for handwritten
calls; their [retained instructions](../evidence/adapters-macos-arm64.json) match
after target-address normalization. Generated leaves measured 1.032 ns/op versus
1.009 for handwritten leaves. Static JNI measured 14.036 versus 14.293 ns/op;
the generated and handwritten C wrappers have matching instructions. The broad
process ranges do not establish a timing advantage for any of these pairs.

The generated typed critical FFM binding measured 2.507 ns/op in SVM versus
2.118 for the handwritten unbound handle. Additional binding/address checks
remain in the generated instructions; this result does not establish exact FFM
parity. The runtime-bound critical factory is retained as a diagnostic: it
measured 1923.313 ns/op and approximately 3416 Java heap bytes/op in SVM. Its
handwritten bound counterpart has the same generic MethodHandle path. On the
JVM, both typed and bound critical calls measured approximately 1 ns/op in this
workload. These observations distinguish runtime adapter construction from the
critical transition contract; they do not imply a portable speedup ratio.

Every measured sample outside the three SVM runtime-bound FFM diagnostic cases
reported zero bytes allocated on the measured Java thread. An allocation canary
runs before measurement and exact zero-byte budgets gate generated/handwritten
direct normal and leaf SVM calls. These counters exclude native allocations and
other threads. Buffer, record, callback, ownership/lifecycle and no-argument
timings remain outside this scalar baseline.

Rerun the complete matrix and extract ARM64 call sites with:

```sh
mise run bench:comparison-build
mise run bench:comparison
mise exec -- python3 benchmarks/comparison/inspect-adapters.py build/comparison build/comparison/adapter-evidence
```

Regenerate the saved table without rebuilding or measuring:

```sh
mise exec -- python3 benchmarks/comparison/run.py report --output benchmarks/comparison/baselines/adapters-macos-arm64.json
```

The v3 workload and allocation measurement are a new profile. Do not compare its
absolute timings to older profiles or overwrite the historical baselines/charts.
