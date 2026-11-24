# Borrowed TruffleData records from native span callbacks

This optional Native Image comparison uses TruffleData 0.14.0's published JSON parser, compact native tape reader and `TapeExportNode`. Dependencies are downloaded into the ignored build directory and verified by SHA-256. A dedicated weekly and manual benchmark job runs this comparison outside CodSpeed. Myna’s production artifacts have no TruffleData dependency.

The input is JSON containing arrays of records. Each record has a signed 64-bit id, signed delta and an unused boolean. The fixture alternates member order. The guest inspects the field names and folds both integers into an order-sensitive checksum. The independent expected result comes directly from the fixture's integer sequence.

`JsonSession.with` lends the decoded document to the benchmark on the session owner thread. The fixture indexes one borrowed subtree view per span without copying any record data. It closes the callback arena before returning the document. All modes use the same JSON, parser, entered context, thread, tape traversal implementation and projection. Each mode runs in its own process.

| Mode | Measured operation |
|---|---|
| `whole` | Enter the guest once and export the entire borrowed document with a single TruffleData tape walk. |
| `pull` | Enter the guest once. Its compiled loop directly calls the span guest for each borrowed subtree. |
| `bound` | Use the same native producer and span guest as `push`, through a receiver-bound FFM callback that calls the guest target directly. |
| `push` | Make one downcall to the native range producer. It calls the generated receiver-bound Truffle upcall with the first record index and count of each span. The same span guest exports the same borrowed subtree. |

The timer includes dispatch, tape traversal and projection. Parsing, registering the callback, collecting span views and warming the guest are outside the timer. Every timed round checks the ordered projection and verifies that all records reached compiled guest code. Span modes also check exact delivery order, the number of callbacks or direct calls, and the final partial span. The default 32,771 records at width 32 end in a three-record span. File I/O, streaming arrival latency and foreign-thread delivery are outside this comparison. TruffleData confines session borrows to their owner thread.

From the repository root, with the pinned tools installed:

```sh
mise run bench:upcall-records
```

On Linux, add `--cpu N` to use one available logical CPU. For other batch sizes, run `mise exec -- python3 benchmarks/upcall-records/run.py run --forks 5 --rows 32771 --width 128` after the build. Changing the width regroups the same ordered records in the JSON fixture. Compare modes within each width. Run outside CodSpeed. The runner saves raw samples, verification output, binary hashes, build flags, hardware and derived median ns/record to `build/upcall-records/records-<rows>-<width>.json`. The four modes each run three warmup rounds and twelve measured rounds per fresh process.

[TruffleData v0.14.0](https://github.com/elide-dev/truffledata/releases/tag/v0.14.0) · [Tape export implementation](https://github.com/elide-dev/truffledata/blob/55de716347ed347ac6739f9124aea8125b8b10c2/tape/src/main/java/dev/elide/truffledata/tape/TapeExportNode.java)

## Linux x86-64 advisory measurements

On an AMD Ryzen 9 9950X3D2, the median of the fresh-process medians was as follows, in nanoseconds per projected record. All 26,741,136 timed records produced the expected ordered checksum in compiled guest code. These shared-host runs used one logical CPU.

| Records per span | Whole tape | Guest-driven spans | Native receiver-bound callback | Native upcall cell | Fresh processes per mode |
|---|---:|---:|---:|---:|---:|
| [1](baselines/records-32771-1.json) | 30.79 | 37.14 | 68.66 | 64.49 | 5 |
| [32](baselines/records-32771-32.json) | 27.97 | 28.15 | 29.10 | 28.85 | 7 |
| [128](baselines/records-32771-128.json) | 28.76 | 28.17 | 28.27 | 27.68 | 5 |

At width 32 the cell delivered 34.7 million records/s. The whole-tape walk was faster in all seven paired blocks, by a median 0.81 ns/record. At width 128 the cell led the whole-tape walk in three of five blocks. The remaining differences at this width overlapped between runs.

A separate 35-record diagnostic caught the cell root recompiling after its first call. Its warm tier-2 compilation inlined its one direct guest call. The guest root contains TruffleData's tape export node. The native producer issues one indirect call for each range and checks the count returned by the guest. [Truffle compilation trace](baselines/compiled-ast.txt) · [Native loop instructions](baselines/native-range-loop.asm)

To reproduce the diagnostic on Linux after building:

```sh
build/upcall-records/measure-records -Dpolyglot.engine.TraceCompilation=true \
  -Dpolyglot.engine.TraceCompilationAST=true build/upcall-records/libranges.so push 35 32
mise exec -- llvm-objdump --disassemble-symbols=myna_deliver_records build/upcall-records/libranges.so
```
