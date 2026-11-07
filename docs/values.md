# Serde values and Substrate VM

`myna-values` maps Rust `Serialize` implementations directly to a native value tape that SVM reads in place. JSON **types** are the baseline: null, booleans, numbers, UTF-8 strings, arrays, and string-keyed objects. No JSON text, parsing, or intermediate `serde_json::Value` is required. Existing `serde_json::Value` inputs work through their normal Serde implementation.

This implementation provides the generic Rust → SVM path, including derived structs, nested containers, custom serializers, and standard Serde enum representations. Schema-derived typed bindings, SVM → Rust deserialization, retained/asynchronous views, Java JSON facades, and object identity/cycles are not implemented. Ordinary Rust memory layouts are never reinterpreted as Java objects.

## Rust API and allocators

The crate lives at `crates/myna-values`. Add it as a path dependency for this checkout, and enable Serde's `derive` feature in your application:

```toml
[dependencies]
myna-values = { path = "path/to/myna/crates/myna-values" }
serde = { version = "1", features = ["derive"] }
```

```rust
use myna_values::{Bump, Document};
use serde::Serialize;

#[derive(Serialize)]
struct Request<'a> {
    id: u64,
    name: &'a str,
    samples: &'a [f64],
}

let arena = Bump::new();
let mut document = Document::new(&arena);
let request = Request { id: 42, name: "Myna", samples: &[1.5, 2.0] };
let view = document.encode(&request)?;
let descriptor = view.raw();
// Pass descriptor.as_ptr() to your generated synchronous SVM entry point.
// The descriptor and document must remain alive until the call returns.
```

`Document::new(&Bump)` is the default arena path. `Document::new_in(allocator)` accepts any `allocator_api2::alloc::Allocator + Clone`, including references to custom allocators. The crate reexports `allocator_api2`, so downstream code can use the exact trait version:

```rust
use myna_values::{Document, allocator_api2::alloc::Global};
let mut document = Document::new_in(Global);
```

Both the node vector and byte vector use the selected allocator, including growth and deallocation. `Clone` must obey the allocator trait's contract: cloned allocators can access each other's allocations. The allocator's own thread-safety constraints propagate through the document. This API does not replace the process-global allocator.

The stable `bumpalo` integration currently implements `allocator-api2` **0.2**, so Myna deliberately uses 0.2.21 instead of the incompatible 0.4 trait. Serde supplies traversal, bumpalo supplies the default arena, allocator-api2 supplies allocator-aware vectors, and thiserror supplies errors. Criterion and proptest are development dependencies. Versions are resolved in the workspace lockfile.

Retain the document and call `encode` repeatedly to reuse capacity. `clear` also retains capacity. Drop the document before resetting its bump arena; Rust's borrow checker enforces this while safe views exist. Bump growth can leave old allocations in the arena until reset, so avoid repeatedly constructing new documents in a long-lived arena. Error paths clear the logical value but retain capacity; they do not rewind the caller's arena.

Storage allocation failure is reported as `Error::Allocation`, subject to the allocator itself returning an allocation error. A caller allocator that aborts cannot be recovered by Myna. User-supplied `Serialize`/`Display` implementations can allocate independently or panic. Myna's `collect_str` implementation starts in 16 bytes of stack storage and spills into its selected byte buffer only if needed, while custom Serde error messages use an ordinary Rust `String` on the error path.

`Document::with_limits_in` sets maximum container depth, total nodes, and UTF-8 bytes. Defaults are 128 container levels and `u32::MAX` nodes/bytes. The byte limit counts logical UTF-8 bytes, including inline keys and values, not only the external buffer. These bound the representation, not arbitrary work or recursive code performed inside a custom serializer. Failed encodes expose no partial value; the document can be reused afterward.

## ABI v3

The ABI is native-endian and in-process on 64-bit targets. It is not a disk/network format. [The example seam](../examples/values.seam) declares matching layouts and a working callback/export. The layout uses ordinary existing Myna records; no new DSL syntax is necessary.

A descriptor is 56 bytes, aligned to 8:

| Offset | Type | Meaning |
| --- | --- | --- |
| 0 | u32 | ABI version, currently 3 |
| 4 | u32 | Descriptor capabilities: bit 0 permits non-finite F64; other bits must be 0 |
| 8 | pointer | Read-only node storage |
| 16 | u64 | Node count, including the root at index 0 |
| 24 | pointer | Read-only UTF-8 storage |
| 32 | u64 | Region 0 byte count (excludes inline text) |
| 40 | pointer | Array of borrowed region descriptors |
| 48 | u64 | Number of borrowed regions |

Each node is 24 bytes, aligned to 8:

| Offset | Type | Meaning |
| --- | --- | --- |
| 0 | u32 | Tag: null=0, bool=1, i64=2, u64=3, f64=4, string=5, array=6, object=7, exact number=8; storage flag 0x100 |
| 4 | u32 | Text byte length, array element count, or object entry count; otherwise zero |
| 8 | u64 | Scalar bits, region ID (high 32 bits) + byte offset (low 32 bits), or first 8 inline UTF-8 bytes |
| 16 | u64 | Subtree end, or last 8 inline UTF-8 bytes |

**Copied strings and exact-number tokens of at most 16 UTF-8 bytes are inline**, including empty strings and short object keys. They require no external byte-buffer storage. Bytes 0–7 are packed into the numeric `payload` word in little-endian order; bytes 8–15 are packed into the numeric `next` word the same way. Unused payload bytes are zeroed by the producer. Integer fields themselves remain native-endian. The logical subtree end of an inline string is always `index + 1`. Use Rust `Node::subtree_end(index)` or Java `ValueTape.next(index)`; do not treat the raw `Node.next` field as an index for inline strings. Text longer than 16 bytes uses the region representation. The explicit `REGION_TEXT = 0x100` flag also allows short text to remain borrowed. Mask tags with `KIND_MASK = 0xff` (or call `kind()` / `tag()`) before comparing their logical type.

Region 0 is the descriptor's owned byte buffer. Region IDs 1..regionCount index a 16-byte pair of pointer and u64 byte length in the region table. Each reference's offset and length are u32, even if the underlying allocation is larger. Regions may represent original input, parser unescape storage, or caller-owned text. All must remain live and immutable. Empty regions may have null pointers; they must never be dereferenced.

The v3 Java reader also accepts v2 descriptors (40 bytes, implicit external storage for strings longer than 16). It rejects v1 and unknown versions/flags. V2 descriptors require flags=0. V3 producers always emit v3. Rebuild generated bindings for the 56-byte descriptor. The canonical C ABI is [values.h](../include/myna/values.h); the example seam mirrors its record layouts. Changes to the layout or existing flags=0 semantics require an ABI version change, independently of the generated seam fingerprint. Optional capabilities may use previously reserved flag bits: older readers reject such descriptors rather than silently misinterpreting them.

### Non-finite binary floats for YAML

Set `MYNA_VALUE_ALLOW_NON_FINITE` (descriptor flag bit 0) to permit positive infinity, negative infinity and NaN in F64 nodes. This is an opt-in tape capability, not a different node layout or an instruction to parse YAML. With flags=0 the existing finite-only behavior is unchanged. Consumers using the capability must upgrade their readers; older v3 readers reject nonzero flags even when every value happens to be finite.

Rust `Document::with_options_in(allocator, limits, Options { allow_non_finite: true })` and `Builder::with_options_in` enable it for Serde and direct producers. The C++ equivalent is `myna::value_builder(allocator, limits, MYNA_VALUE_ALLOW_NON_FINITE)`. The choice persists through `clear()` and subsequent encodes. Rust `View::flags()`/`RawView.flags` and Java `ValueTape.flags()` expose the capability. Java subclasses adapting other storage can use the protected `(nodeCount, byteCount, version, flags)` constructor.

F64 payloads carry IEEE-754 binary64 bits. `doubleValue` returns the represented value; `bits` exposes the stored bits directly. Signed zero is preserved. Do not rely on a particular NaN payload surviving arithmetic, f32 widening, or conversions outside the tape. A YAML adapter resolves `.inf`, `-.inf`, and `.nan` itself, then writes binary F64 nodes. The DECIMAL kind still requires an RFC 8259 number token; this flag does not make `.inf`, `.nan`, or `NaN` valid decimal text. A JSON exporter must explicitly reject non-finite values or implement a documented caller-selected conversion; the tape never silently converts them to null or strings.

Containers precede their children. Arrays contain value subtrees; objects alternate a string-key node and a value subtree. The first child is `container + 1`; skipping a child uses its `next` index. Empty containers have `next == container + 1`. Scalars and external strings also have `next == index + 1`; inline strings use the implicit end described above. The root subtree ends at the total node count. Duplicate object keys and serializer iteration order are preserved; the Java `find` helper returns the first matching key.

The descriptor and buffers are borrowed and read-only. No consumer may mutate or retain the storage beyond the owner's borrow. No native ownership, `noalias`, or no-transition optimizer contract follows from merely passing this descriptor. Crossings use Myna's existing isolate/transition rules. A Rust call that invokes Java is not a native leaf.

## Direct producers, exact numbers, and C++ simdjson

`Builder::new_in(&arena)` combines Serde values with explicit borrowed text and exact numbers. Its node, byte, region, and container-stack vectors all use the selected allocator. `json-number` validates number grammar without rounding through floating point. The Rust borrow checker keeps registered immutable slices live; an error poisons the builder until `clear()`, and `finish()` only exposes one complete root.

```rust
use myna_values::{Bump, Builder};
let arena = Bump::new();
let input = b"1e-999999999999";
let mut builder = Builder::new_in(&arena);
let region = builder.region(input)?;
builder.begin_object()?;
builder.value("number")?;
builder.number_ref(region, 0, input.len() as u32)?;
builder.value("name")?;
builder.value("Myna")?;
builder.end()?;
let view = builder.finish()?;
let descriptor = view.raw();
```

`string_ref` validates UTF-8 and borrows at any length. `number_ref` validates and borrows a numeric token. `exact_number` validates and copies a token, with short-token inlining. Huge integers and exponents are preserved as text. Java's `numberText` returns the exact spelling; `decimalValue` and `decimalLongExact` are explicit conversions and may fail when the destination cannot represent it. A valid JSON exponent can exceed BigDecimal's range.

C++ producers include [values.hpp](../include/myna/values.hpp), a C++17 header-only builder without Rust, Serde, or simdjson dependencies:

```cpp
std::pmr::monotonic_buffer_resource arena;
myna::value_builder builder(&arena);
builder.begin_object();
builder.string("name");
auto region = builder.region(parsed_utf8); // caller retains this storage
builder.string_ref(region, 0, uint32_t(parsed_utf8.size()));
builder.end();
auto descriptor = builder.view();
// consume(&descriptor) synchronously while builder and parsed_utf8 remain live
```

All C++ vectors use the supplied `std::pmr::memory_resource`. Errors throw and poison the current document; catch them before crossing a C ABI. String methods require valid UTF-8. `validated_number` and `validated_number_ref` explicitly require an already lexically validated JSON number. These contracts let a parser avoid redundant scans. Check size before narrowing a parser's size_t length to u32. Mutating the C++ builder invalidates previously returned views.

The pinned C++ simdjson conformance fixture demonstrates OnDemand → Myna → SVM with no intermediate DOM. It borrows string views, including parser-owned unescaped strings, and preserves exact numeric tokens. Its independent regex number validation is test code, not a production parser adapter. TruffleData owns full-document validation, duplicate-key policy, pooling, known-schema bindings, and durable Java facades. Raw token access alone is not validation. OnDemand's parser and padded input must remain alive and unreused until the consumer returns; “zero-copy” here means no additional Myna text copy, not that simdjson never unescapes or allocates.

Neither `NativeValueTape` nor a raw descriptor owns its regions. Retained/asynchronous use requires an external owner/lease covering all storage. Scoped views must not escape into an immutable, thread-safe JEP 540 value without a durable owner or snapshot. JEP 540, Jackson, and JSON-P facade implementations belong upstream; this release provides their native representation foundation.

## SVM consumption

`dev.elide.myna.values.NativeValueTape` is included in the separate `myna-values.jar`. Compile that JAR onto the consumer's classpath, alongside the generated bindings. Construct one reader per view; all node operations are direct memory reads, with no per-node wrapper or native callback. Graal may eliminate the reader allocation where it can prove escape behavior; the API does not promise that optimization.

```java
private static final ValueTape.Utf8Key NAME = ValueTape.Utf8Key.of("name");

public static long consume(PointerBase descriptor) {
    var values = new NativeValueTape((Pointer) descriptor);
    long name = values.find(0, NAME);
    if (name < 0) return -1;
    return values.length(name); // UTF-8 byte length, without materializing a String
}
```

`find` is a linear field lookup using a reusable UTF-8 key. `Utf8Key.of(String)` prepares immutable keys once, rejecting unpaired UTF-16 surrogates; keys up to 16 bytes compare packed words. The byte-array overload remains available. Repeated access can retain a node index for that view's lifetime; indexes must not be reused after re-encoding a different value. `first` and `next` support allocation-free container traversal. `stringEquals` compares UTF-8 directly; `stringByte` exposes an individual byte. `copyStringUtf8` writes into caller-provided reusable byte storage. `stringValue` materializes a Java String only on request, returns the shared empty String for zero length, and rejects malformed UTF-8 rather than replacing it. The local reader has an ASCII decoding fast path; non-ASCII local decoding uses the JDK charset decoder. `unsignedBits` preserves the full unsigned 64-bit value in a Java `long`; use Java's unsigned operations.

The reader bounds-checks indexes and byte ranges. `validate()` optionally checks tags, subtree boundaries, entry counts, and scalar constraints in O(nodes + exact-number text bytes) time. It assumes the descriptor's pointers and allocation extents are trustworthy and the backing data remains immutable. Neither Java nor Myna can establish the validity of arbitrary raw pointers from their numeric values. UTF-8 is guaranteed by the Rust serializer; structural validation does not independently validate UTF-8 from other producers.

## SIMD validation and transcoding

The Rust [simdutf binding](https://docs.rs/simdutf/0.7.0/) supplies CPU-dispatched validation and UTF-8 → UTF-16 conversion. It bundles the C++ implementation; building the crate requires a C++11-compatible compiler and linking its C++ runtime. No JSON parser is added: this path handles typed values, not JSON text.

For byte-sourced text, use `myna_values::text::Utf8::from_bytes(&bytes)`. It borrows without copying, validates short inputs locally with Rust's UTF-8 validator, and uses simdutf at 64 bytes and above. The validated wrapper implements Serde `Serialize` as a string. Normal Rust `str` inputs skip validation; ordinary byte arrays continue to serialize as numeric arrays.

`text::to_utf16(&str, &mut [u16])` converts into caller-owned storage without allocating. It checks capacity before writing and reports `OutputTooSmall` without changing the destination. Short text uses Rust's local UTF-16 iterator; bulk conversion uses simdutf. Rust `View::string(index)` handles both string representations without heap allocation and avoids revalidating external strings already produced by the serializer.

For SVM bulk materialization, use `SimdNativeValueTape` in place of `NativeValueTape`. It transcodes external strings of **256 bytes or more** directly into a pinned Java `char[]`, validates during conversion, and constructs the Java String from the written UTF-16 range. Shorter strings use the same local path as `NativeValueTape`. It uses a normal native transition because string lengths are unbounded. The 256-byte threshold is a conservative initial policy measured on the local fixture, not a universal crossover.

The SIMD reader requires the `myna_utf8_to_utf16_v2` symbol from a linked Rust static library containing `myna-values`, plus `-lc++` on macOS or `-lstdc++` on Linux. The integration fixture demonstrates the link. Its output buffer uses Java's allocator and GC; Rust document buffers still use the selected caller allocator. `NativeValueTape` itself has no native helper dependency, so consumers can opt out of the transcoding bridge. Both readers require live, immutable native storage for the duration of access.

```java
var values = new SimdNativeValueTape((Pointer) descriptor);
String text = values.stringValue(node); // Local or SIMD decoding by byte length.
```

No normalization, case folding, or Unicode-equivalence matching is performed. UTF-8 comparisons are byte-exact. An inline string threshold is measured in bytes, not characters; four supplementary-plane characters occupy all 16 bytes. Validation covers malformed sequences, overlong encodings, UTF-8 surrogate encodings, truncated sequences, and out-of-range code points.

## Serde mapping policy

| Serde input | Value representation |
| --- | --- |
| Unit, unit struct, `None` | Null |
| `Some`, newtype struct | Wrapped value |
| Signed / unsigned integers through 64 bits | Separate i64/u64 tags; no floating conversion |
| i128 / u128 | Accepted only if representable by i64 or u64; otherwise error |
| f32 / f64 | f64; f32 widens, negative zero preserved; NaN/infinity require `ALLOW_NON_FINITE` |
| String, char, `collect_str` | Copied UTF-8 string, including embedded NUL |
| Sequence, tuple, tuple struct, bytes | Array (bytes become unsigned numeric elements) |
| Struct, map | Object; keys must serialize as strings |
| Unit variant | Variant-name string |
| Newtype, tuple, struct variant | Externally tagged object |

Serde attributes such as rename, skip, flatten, tagged/untagged variants, and custom serialization are applied by their `Serialize` implementations before/while writing the tape. The serializer reports `is_human_readable() == true`. Non-string map keys are rejected, rather than stringified. `serde_json`'s private `raw_value`/`arbitrary_precision` sentinel encodings are not specially interpreted; do not enable those extensions expecting transparent numeric/raw JSON semantics.

Strings must be copied: Serde permits a temporary string that dies as soon as `serialize_str` returns. This path avoids JSON encoding/decoding, but is not zero-copy from arbitrary Rust inputs. It also writes structural metadata for every value and key. Large homogeneous numeric arrays currently use one 24-byte node per element; packed typed arrays are future work.

## Verification and performance

```sh
mise run test:rust          # formatting, Clippy, tests, property tests, lifetime doctest
mise run test:cpp           # C/C++ ABI, PMR, sanitizers, pinned simdjson
mise run test:jvm           # reader validation and existing generator tests/coverage
mise run test:native-image  # includes real Rust and C++ simdjson -> SVM consumption
mise run bench:values      # Criterion encoding benchmarks
build/values-test/values-app 1000000  # optional local crossing smoke measurement
build/values-test/values-app strings 30000  # Java vs SIMD string materialization
```

Criterion compares reused bumpalo/global documents against `serde_json::to_value` and JSON bytes written into a reused vector. Cases cover a flat struct, nested structs, a 4096-number array, and 256 strings. These compare representation construction costs; the JSON byte benchmark excludes parsing and the `Value` benchmark allocates a fresh tree. They are not interchangeable end-to-end operations.

The Native Image fixture builds a Rust static library and C++ producer, uses generated direct bindings and a normal Java callback entry, and checks numeric boundaries, Unicode/NUL strings, null, nested arrays, and repeated encoding. Its optional timer includes encoding, isolate entry, field lookup, and nested numeric consumption. It excludes image startup and uses a small fixed input. Hardware, compiler, allocator, input shape, GC, and allocation strategy all affect results. Benchmarks are evidence for tuning, not a cross-platform latency guarantee.

Historical ABI-v1 baseline before inline strings, 2026-11-04: Apple M5 Max, macOS arm64, Rust 1.99.1, release ThinLTO. Criterion used 30 samples, 0.5 s warmup, and 1 s measurement per case. Approximate central estimates from this run:

| Input | Reused bumpalo tape | Reused Global tape | Fresh serde_json::Value | Reused JSON bytes |
| --- | --- | --- | --- | --- |
| Flat, four fields | 42 ns | 35 ns | 136 ns | 46 ns |
| Nested, 32 structs | 1.53 µs | 0.97 µs | 3.53 µs | 1.20 µs |
| 4096 integers | 7.85 µs | 5.80 µs | 6.45 µs | 11.77 µs |
| 256 strings | 1.28 µs | 1.05 µs | 3.38 µs | 3.33 µs |

The native fixture's separate one-million-iteration smoke run measured approximately 143 ns/object for encoding its seven-field struct, Rust → SVM entry, lookup, and nested summation. This is a different input and measurement method from the Criterion cases. There is no handwritten typed-seam baseline yet, and Java allocation/GC costs have not been profiled. The results do not establish that bumpalo is always fastest: Global was faster in these warmed-up encoding cases, and the numeric-array case also illustrates why packed typed arrays remain useful future work.


ABI-v2 string materialization smoke run on the same M5 Max (30,000 iterations per case):

| UTF-8 input | Local Java reader | SIMD reader |
| --- | --- | --- |
| 256-byte ASCII | 260 ns | 171 ns |
| 4096-byte ASCII | 3478 ns | 642 ns |
| 256-byte supplementary Unicode | 410 ns | 240 ns |
| 4096-byte supplementary Unicode | 4937 ns | 2127 ns |

These figures include a Rust → SVM callback and a volatile String sink to force materialization, but exclude encoding (the tape is built once). They include allocations and possible GC noise, use a single local smoke run, and do not establish cross-platform guarantees. Both readers use local decoding for 16-byte strings. Criterion also measures validation and conversion into reused Rust buffers at 16, 17, 256, and 4096 bytes. Inline-only documents allocate no external byte buffer; tests verify allocator routing, this allocation reduction, logical byte limits, and formatted-string spill behavior.

Rust-only Unicode microbenchmarks (20 samples, 0.1 s warmup, 0.3 s measurement; mostly ASCII plus a supplementary character) measured approximately 109 ns for converting 4096 UTF-8 bytes into reused UTF-16 storage via the selected SIMD path, versus 1931 ns for the Rust iterator baseline. UTF-8 validation was approximately 35 ns versus 82 ns. These exclude SVM crossings and Java allocation. The short-input paths stay local and retain output-capacity checks; they are not expected to outperform the bare iterator baseline.


## Packaging

`mise run build` produces the generator, hosted feature, and separate value-reader JARs. Generator-only Bazel consumers may continue using `src/main/java/**/*.java`; readers are in `src/values/java/**/*.java` and require the Graal Native Image SDK. `mise run package:values` builds the value JAR, verifies the Rust crate package, and emits a versioned SDK archive with the crate, C/C++ headers, ABI example, documentation, and SHA-256 manifest. Use the Git tag/release archive or a Git/path Rust dependency; this does not imply publication to crates.io or Maven Central.
