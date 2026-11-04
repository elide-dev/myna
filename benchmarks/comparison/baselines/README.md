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


`hosted-arm64-0.7.2` is the maintained five-process CI baseline for release source `7401c446ef9ad80d7bd14a9bf92f360a04956b80`. It includes full ARM CPU identity and points to the originating workflow. It drives the checked-in SVM charts; it is still a shared-host measurement, not a controlled-hardware timing gate.
