# Persisted formats

A descriptor can declare versioned binary formats for data that is written to disk and read back later, possibly
by another language. A format is a stream of tagged nodes in which a node refers to earlier nodes by index. Myna
generates, for Rust and Java, a writer that produces the one canonical encoding of a value and a reader that
validates a whole file, including declared limits, before handing out any view. It replaces the pattern of a
hand-written writer in one language, a hand-written reader in the other, and a drift test between them
(myna#167; the design and its measurements are in
[the spec](superpowers/specs/2026-11-18-persisted-node-formats-design.md)).

The [value tape](values.md) is the wrong tool for this: it is an in-memory handoff for one call, with no stable
byte layout, no schema tags and no back-references.

## Example

From [`tests/formats/doc.seam`](../tests/formats/doc.seam), abbreviated:

```text
enum Align type=u8
  value left = 0
  value center = 1
end

family Doc

format DocFile family=Doc version=1 magic=0x31434f44 pinned=e6838f3259d67776
  limit max_bytes=268435456
  limit max_nodes=4194304
  limit max_strings=1048576
  limit max_depth=128
  limit max_work=16777216
  header capability
  root Document
end

kind Block family=Doc nodes=Paragraph,RawText,Section

element Attr family=Doc
  string key
  ref value node=Style optional=true
  field important type=bool
end

element SpanText family=Doc tag=0
  string text
end

element SpanBreak family=Doc tag=1
end

union Span family=Doc elements=SpanText,SpanBreak

node Style family=Doc tag=1 sharing=interned
  string face
  field align type=u8 enum=Align
  field size_bits type=u64
  ref parent node=Style optional=true
end

node Paragraph family=Doc tag=2 sharing=unique
  ref style node=Style
  list spans union=Span max=65535
  list attrs element=Attr max=255
end

node RawText family=Doc tag=3 sharing=unique
  list units type=u16
end

node Section family=Doc tag=4 sharing=unique
  string title
  refs blocks node=Block max=4096
end

node Document family=Doc tag=5 sharing=unique
  refs sections node=Section
  field source type=bytes max=1048576
end
```

## Declarations

Blocks are flat: nodes, elements and formats each name their family.

- `family NAME`: one line. A family owns a namespace shared by its nodes, kinds, elements, unions and formats,
  and a tag space for its nodes. Several formats can share one family and differ in magic, version, limits,
  header word and roots.
- `format NAME family=F version=N magic=M [pinned=HEX16] [draft=true] … end`. `version` is a positive `u32`;
  `magic` is a `u32`, unique among the module's formats. Body lines:
  - `limit KEY=VALUE`, one per line. All five are required, with no defaults: `max_bytes` (44 to 2147483639),
    `max_nodes` (1 to 2147483647), `max_strings` (0 to 2147483647), `max_depth` (1 to 32767) and `max_work`
    (1 to 2147483646).
  - `header NAME`: at most one. Declares the format's header word (below).
  - `root NODE[,NODE…]`: the node types the last node may have; a kind name stands for its nodes.
  - A format accepts exactly the node types reachable from its roots through the declared reference targets.
    Any other node of the family is rejected in that format's files.
- `node NAME family=F tag=N sharing=interned|unique … end`. `tag` is 1 to 65535, unique in the family.
  `sharing` is required:
  - `interned`: equal nodes are one node. The writer merges them; the reader rejects two nodes with equal bytes.
  - `unique`: each node has exactly one reference (none for the root). The reader rejects a second reference,
    including a second one from the same parent.
- `element NAME family=F [tag=N] … end`: an inline list element. With a `tag` (0 to 65535) it is used through a
  union; without one it is used through `element=` and needs at least one member. Elements hold only `field`,
  `string` and `ref` members, so they never nest.
- `kind NAME family=F nodes=A,B,…`: a named set of nodes, usable as a reference target and as a root.
- `union NAME family=F elements=A,B,…`: a closed set of tagged elements with distinct tags.

Members of a node, in wire order (elements take the first three):

| Line | Meaning |
|---|---|
| `field NAME type=T [enum=E \| flags=G]` | A scalar: `u8 i8 u16 i16 u32 i32 u64 i64 f32 f64 bool`. `enum=`/`flags=` name a module group carried by exactly `T`. |
| `string NAME` | A string-table id. There are no inline strings. |
| `ref NAME node=T [optional=true]` | One reference to an earlier node. `T` is a node, a kind, or `any`. |
| `field NAME type=bytes [max=N]` | A byte payload with a `u32` length. |
| `refs NAME node=T [max=N]` | A counted list of references. |
| `list NAME type=T [max=N]` | A counted list of integer scalars (UTF-16 code units with unpaired surrogates fit `u16`). |
| `list NAME element=E [max=N]` | A counted list of untagged elements. |
| `list NAME union=U [max=N]` | A counted list of tagged elements, each laid out by its tag. |

Enums and flag sets are **closed** in a persisted format, unlike at the call boundary: the reader rejects an
unlisted enum value and an undeclared flag bit, and the writer refuses to write them. `bool` must be 0 or 1.
Floats are stored and compared as raw bits, so NaN payloads and the sign of zero survive. Member names are
`[a-z][a-z0-9_]*`; names a generated accessor already uses (`index`, `tag`, `bytes`, …, or another member's name
with an accessor suffix such as `_id` or `_count`) are rejected.

## Wire format

All integers are little-endian and fixed-width. A file is `header | strings | body | index`, and nothing else:
the input slice is the frame, and trailing bytes are an error.

| Offset | Field |
|---|---|
| 0 | `u32` magic |
| 4 | `u32` version |
| 8 | `u64` layout fingerprint (the first 8 bytes of the SHA-256 below) |
| 16 | `u32` header word, 0 when the format declares none |
| 20 | `u32` string count |
| 24 | `u32` string bytes (total UTF-8 bytes) |
| 28 | `u32` node count, at least 1 |
| 32 | `u32` body bytes, a multiple of 4 |
| 36 | `u32` reserved, 0 |

- **Strings**: one `u32` end offset per string (relative to the string bytes), then the UTF-8 bytes of all
  strings, zero-padded to 4. Each string is valid UTF-8 on its own; an empty string is allowed.
- **Body**: the nodes, each starting on a 4-byte boundary. A node is a `u16` tag and a zero `u16`, then its
  fixed part, which holds its members packed in declared order with no interior padding. A scalar takes its
  width (`bool` one byte), and a string id, reference, length or count four bytes. The fixed part is
  zero-padded to 4. Then, for each `bytes`, `refs` and `list` member in declared order, comes its payload,
  zero-padded to 4. A tagged element is a `u16` tag, a zero `u16` and its members; an untagged element is its
  members; each element is zero-padded to 4. An absent optional reference is `0xFFFFFFFF`.
- **Index**: one `u32` per node, its offset in the body. Node `i`'s extent ends where node `i + 1` starts.

**Canonical form.** A value has exactly one encoding:

1. Padding and reserved fields are zero, and every node ends exactly at its extent.
2. Strings are stored once each, in order of first use: nodes in index order, and each node's members and
   elements in wire order. No string is unused.
3. Every node but the root is referenced. No two `interned` nodes have equal bytes, and a `unique` node is
   referenced exactly once.
4. Nodes are in depth-first post-order from the root, which is last: a node's references are visited in wire
   order, a node already placed is not visited again, and a node is placed once its references are.

Because a child is an index and string ids are canonical, two `interned` nodes are structurally equal exactly
when their bytes are, so deduplication needs no deep comparison. Both writers lay files out by these rules,
whatever order the nodes were created in, so they produce identical bytes and encoded files can be compared or
used as keys.

## Validation

`validate` runs two stages; `validate_structural` (Java: `checkStructural`) runs only the first. Each stops at
the first failure and reports the error kind, the byte offset of the failed check within the file, and the node
index (`u32::MAX` in Rust and -1 in Java outside a node). No view is handed out for a file that failed, and no
input can make a reader panic, read out of bounds, recurse, or allocate more than its limits allow.

Stage 1, structure and limits, in one forward pass:

1. Length at least 40 (`Truncated`), then at most `max_bytes` (`TooLarge`).
2. Magic, version and fingerprint (`BadMagic`, `BadVersion`, `BadFingerprint`). Then the header word: it must be
   in the caller's accepted set (`HeaderRejected`), or 0 when the format declares none (`BadHeader`). Then the
   reserved word (`BadHeader`). All of this is checked before any string or node is read.
3. Counts: strings (`TooManyStrings`), nodes (`TooManyNodes`), at least one node, body bytes a multiple of 4,
   and the exact total length (`BadLength`).
4. The string table: non-decreasing end offsets, the last equal to the string bytes, zero padding
   (`BadStringTable`), and each string's UTF-8 (`BadUtf8`).
5. The index: each offset 4-aligned, the first 0, increasing, and inside the body (`BadIndex`).
6. The side tables: a depth and a work counter per node, allocated once (Rust reports `OutOfMemory` if that
   fails).
7. Each node in index order:
   - the tag is one this format accepts (`BadTag`), the reserved half-word is zero, and the fixed part fits
     (`BadNode`);
   - the fixed part's members in wire order: scalar values (`BadValue`), string ids, which must be in range and
     in first-use order (`BadString`, `StringOrder`), references, which must point to an earlier node of an
     allowed type, with a `unique` target referenced only once (`BadRef`, `BadRefKind`, `SharedUnique`), and
     each length or count against its `max=` (`FieldTooLong`);
   - the fixed part's padding (`BadNode`);
   - each payload in order: it must fit in the node (`BadNode`), and its references and elements are checked
     as above, tagged elements against their union (`BadElement`);
   - the node ends exactly at its extent (`BadNode`);
   - its depth, 1 plus its deepest child's, against `max_depth` (`TooDeep`), and its work, 1 plus its children's
     work, saturating, against `max_work` (`TooMuchWork`).
8. Every node but the last is referenced (`Unreferenced`), every string is used (`UnusedString`), and the last
   node is a root (`BadRoot`).

Stage 2, canonical form: node order by an explicit-stack walk from the root (`NodeOrder`), then duplicate
`interned` nodes (`DuplicateNode`) and duplicate strings (`DuplicateString`), found with a merge sort. Both
stages are linear in the input, except the sort.

`max_work` bounds what a consumer's naive recursive walk costs: the number of node visits from the root when
shared children are visited once per reference. Sharing can make that count exponential in the node count, as
a decompression bomb does; after validation, a recursive walk makes at most `max_work` visits and `max_depth`
nested calls. References held in elements count as children.

| Code | Kind | Class |
|---|---|---|
| 1-4 | `BadMagic`, `BadVersion`, `BadFingerprint`, `HeaderRejected` | `Unsupported` |
| 16-22 | `TooLarge`, `TooManyStrings`, `TooManyNodes`, `FieldTooLong`, `TooDeep`, `TooMuchWork`, `OutOfMemory` | `Limit` |
| 32-52 | `Truncated`, `BadHeader`, `BadLength`, `BadStringTable`, `BadUtf8`, `BadIndex`, `BadTag`, `BadNode`, `BadValue`, `BadElement`, `BadString`, `StringOrder`, `BadRef`, `BadRefKind`, `SharedUnique`, `Unreferenced`, `UnusedString`, `BadRoot`, `NodeOrder`, `DuplicateNode`, `DuplicateString` | `Malformed` |

Codes are stable and numbered in that order; the class follows from the code (below 16, below 32, the rest).
The Rust and Java readers return the same kind, offset and node for every input: the integration suite checks
one input per kind and 1600 random corruptions.

## Versioning and the layout fingerprint

Every file carries a layout fingerprint: the first 8 bytes of a SHA-256 over a canonical JSON of numbers and
fixed keys only. It changes only when the set of byte sequences a reader accepts changes.

- **Hashed**:
  - the wire-rules revision, which changes only if Myna changes how a descriptor maps to bytes;
  - magic and version;
  - whether a header word is declared;
  - the root tags;
  - each node tag the format accepts, sorted, with its sharing mode and its members in wire order (kind,
    scalar type, `optional`, resolved target tags and `max=`; an element list contributes each element's
    layout);
  - the value set of each referenced enum and the mask of each referenced flag set.
- **Not hashed**:
  - every name: format, family, node, element, member, kind, union, enum value, flag bit and header word;
  - the five format limits;
  - the generator version, the target ABI, the module's `abi=`, Java naming, comments and declaration order;
  - nodes that only another format of the family reaches.

So a rename, a limit change or a Myna upgrade leaves files already on disk valid. `pinned=` holds the expected
value. Generation fails when it differs, with a message giving the computed value and saying to bump `version=`
and update the pin, so a layout change cannot ship under an old version by accident. A format without `pinned=`
must say `draft=true`; generation reports the value to pin. A reader accepts exactly one `(version, fingerprint)`
pair; a consumer that reads two versions declares two formats with the same family and dispatches on `peek`.

The module's `seam.json` records families and formats (with their names) only when the module declares a family,
so every other module keeps its `seam.abi`.

## Generated Rust

`formats.rs` holds one `pub mod` per family (`doc` for `Doc`), needing only `core` and `alloc`, so it builds in
`no_std` crates. Include it as a module file: `#[path = "generated/formats.rs"] mod formats;`.

- Per format, a `Copy` type (`doc::DocFile<'a>`) with `MAGIC`, `VERSION`, `FINGERPRINT` and the five limits as
  constants. It has:
  - `validate(bytes, accept)` and `validate_structural(bytes, accept)`, returning `Result<Self, FormatError>`
    (without `accept` when the format declares no header word);
  - `peek(bytes) -> Option<Peek>`, the version and header word when the magic matches;
  - the header word's accessor, `root()`, `node(i)`, `node_count()` and `bytes()`.
- Per node, a `Copy` view (`Style<'a>`) with `index()` and one accessor per member:
  - a scalar returns its value, enum and flag fields as their raw carriers;
  - a string returns `&'a str` borrowed from the input, and `<name>_id()` its id;
  - a reference returns the target's view (`Option<…>` when optional), a kind's enum (`Block<'a>`) or `AnyNode<'a>`;
  - `bytes` returns `&'a [u8]`;
  - `refs` and scalar lists return `ExactSizeIterator`s, with `<name>_count()` and `<name>_at(i)`, and scalar
    lists also `<name>_raw()`;
  - element lists iterate element views or the union's enum (`Span<'a>`).
- `tag::*` constants, `ErrorKind` (with `code()` and `class()`), `Class`, `FormatError { kind, offset, node }`.
- `write::DocWriter`:
  - `string(&str) -> StrId` interns a string;
  - one method per node (`style(face, align, size_bits, parent)`) returns its id or a `WriteError`;
  - `finish_doc_file(&self, root, capability) -> Result<Vec<u8>, WriteError>` writes the canonical bytes.

  Ids are typed per node (`StyleId`), per kind (`BlockId`, with `From` from each member's id) and for `any`
  (`AnyId`). Ids of `interned` nodes are `Copy`; ids of `unique` nodes are not, so referencing one twice does
  not compile. Elements are plain structs (`Attr { key, value, important }`) and unions enums
  (`Span::SpanText { text }`). The writer enforces the format's limits and closed sets, and `finish` drops
  nodes and strings the root does not reach.

## Generated Java

Plain Java over `byte[]`, with no FFM and no reflection; validation allocates only its side tables. For family
`Doc` and format `DocFile`:

- `DocNodes`: `TAG_*`, the error codes, `errorClass` and `errorName`, and static accessors on a validated file
  at array offset `base`:
  - `tag(b, base, node)`, `nodeCount`, `string`, `stringOffset` and `stringLength`;
  - per member, `Style_face(b, base, node)`, with `Style_face_id` for strings; a reference returns the target's
    node index, or `NO_REF` when absent;
  - `refs` and scalar lists have `_count` and `_at`, and scalar lists also `_offset`; `bytes` returns a copy;
  - element lists have `_count` and `_first`, which returns a cursor (an array offset). Element accessors take
    the cursor (`Attr_key(b, base, at)`); `Attr_next(at)` and `Span_next(b, at)` step it, and `Span_tag(b, at)`
    reads a tagged element's tag.
- `DocFileReader`: the constants, and:
  - `check(b, off, len, accept)` returns `null` or `{kind, offset, node}`; `checkStructural` has the same shape;
  - `require` throws `DocException`;
  - `headerWord`, `peek` (−1, or the version in the high 32 bits and the header word in the low) and `root`.
- `DocWriter`: `string(String)`, which rejects unpaired surrogates, then one method per node returning an
  `int` id, and `finishDocFile(root, capability)`. Members map as follows:
  - unsigned scalars take the same-width signed primitive, as elsewhere in Myna;
  - `refs` take `int[]`, scalar lists primitive arrays, and `bytes` a `byte[]`;
  - element lists take `List`s of generated records, and unions a sealed interface of records
    (`DocWriter.Span.SpanText`);
  - an absent optional reference is `NO_REF`.

  Errors are `DocException` with offset and node −1. Java cannot type-check `unique` reuse or reference
  targets, so the writer checks both when a node is added (`SharedUnique`, `BadRefKind`).

There is no `MemorySegment` or `ByteBuffer` reader. On the pinned Native Image, segment accessors measured about
70 times slower than `byte[]` view handles, and `ByteBuffer` 2.5 times slower (elide-dev/graal#250).

## Tests

`mise run test:formats` runs [`tests/formats_integration.py`](../tests/formats_integration.py) over the
fixture. [`check.rs`](../tests/formats/check.rs) and [`FormatsMain.java`](../tests/formats/FormatsMain.java)
drive each language. The test checks that:

- each writer builds every golden value in two creation orders, both giving the checked-in
  `tests/formats/golden/*.bin`;
- every golden survives decoding and encoding again unchanged;
- both readers print the same verdicts, node dumps and writer errors;
- one crafted input per error kind gets the same `(kind, offset, node)` from both readers;
- seeded random corruptions get identical verdicts from both readers;
- [`shapes.seam`](../tests/formats/shapes.seam) compiles in both languages.

`MYNA_UPDATE_GOLDEN=1` rewrites the golden files. The Native Image suite runs the Java writer and reader in an
image against the same golden files, and `mise run test:rust-fuzz` fuzzes the Rust reader (`formats_reader`).
