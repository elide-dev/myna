# Local scalar baselines

These five-process runs preserve raw per-process samples, commands, binary hashes,
compiler evidence and runtime metadata. They were measured on a shared Apple M5
Max workstation with substantial concurrent activity. Treat them as diagnostic
baselines; the wide process ranges prohibit small percentage performance claims.
Use the separate `Boundary comparisons` CI workflow for dedicated-hardware data.

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
