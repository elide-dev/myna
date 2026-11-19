# Design: persisted tagged-node formats in the seam descriptor (myna#167)

Status: design only, against `main` at 0.19.0 (`b44216c`). Nothing in the repository was changed apart from this file.
Second draft, written under a short time box. Only the Java input probe (section 5) was measured. Claims about SVM
code shape, fuzz throughput and the cost of the canonical checks are expectations to verify, not results. The
truffledata slab codec, `docs/values.md`, `Dsl.java` and `Seam.java` were skimmed, not read in full; section 9
lists what that leaves uncertain.

Revision 2026-11-18: the first consumer answered all six questions on myna#167. This draft applies those answers
to the DSL, wire format, reader checks, generated APIs and testing. Section 8 marks what the answers settled.
The maintainer accepted the remaining recommendations (section 8, items 1, 7 to 10 and 18 to 21) the same day,
and the fingerprint answer was posted on myna#167. The design is accepted for implementation; section 9 lists
what implementation must still establish.

**Implemented** in the change that adds `docs/formats.md`, which is the reference wherever details differ from
this design. The differences:

- The fixed part of a node is zero-padded to 4 before the payloads. Payloads start aligned, and the format
  keeps no other padding rule.
- Stage 1 validates the whole index before the first node. Within a node it checks members in wire order: the
  fixed part, then each payload. It does not group checks by category as section 4 lists them. Both readers use
  the same order, so `(kind, offset, node)` agree.
- Java `check` returns `null` or `int[] {kind, offset, node}`, because the three values do not fit one `long`.
  Error offsets are file-relative.
- Enum and flag fields are their raw carriers in both writers and both readers, checked as closed sets. The
  typed wrappers of `seam.rs` and `<Class>Constants` name the values.
- Writers check depth and work at `finish`, per format, rather than at node creation: limits belong to formats
  and a family may have several.
- The Rust fuzz target checks the reader and a bounded recursive walk. Round trips of every golden file through
  both writers, and reader parity over 1600 seeded corruptions, run in `tests/formats_integration.py` instead.

<!-- Decisions recorded 2026-11-18:
     1. The first consumer is an existing set of hand-written codecs in another project, not the
        truffledata slab.
     2. Java writer is in v1 (the first consumer is Java and Rust): generate writer + validating reader in
        both languages.
     3. Take the recommendations for references-only and fixed-width, pending the first consumer's answers
        on elide-dev/myna#167 (inline tagged elements, interned string table, hash-consing, capability word).
        Answered 2026-11-18; none of the answers conflicts with decisions 1 to 4. See section 8.
     4. MemorySegment on Native Image measured (section 5, Java): works with no extra configuration, but
        accessors are ~70x slower than byte[] in an image, so v1 ships the byte[] reader only. -->

## 1. Summary

- New declarations: `family` (a set of node declarations), `format … end` (a versioned file layout over a
  family), `node … end`, `element … end` (an inline list element), and the one-line `kind` and `union` (named
  sets of nodes and of tagged elements). Several formats can share one family and differ in magic, version,
  limits and root.
- A format is a versioned, self-delimiting byte layout: a header, an **interned string table**, and a
  **post-order stream of tagged nodes**. Every child node is written before its parent, a parent names its
  children by node index, and the last node is the root.
- Child nodes are always references. A node can also hold counted lists of **inline elements** (tagged or
  untagged, never nested), counted lists of scalars, and string-table references.
- **The encoding is canonical: a value has exactly one byte encoding.** The order of strings and nodes is
  fixed by the value, not by the order the writer was called in, so the Rust and Java writers produce
  identical bytes and encoded bytes can be compared or used as keys.
- Each node type declares its sharing. `interned` nodes are merged by the writer when equal, and the reader
  rejects two equal ones. `unique` nodes have exactly one reference, and the reader rejects a second.
- Wire format: 40-byte header (magic, version, layout fingerprint, one declared header word, counts), the
  string table, packed fixed-width little-endian nodes, and a trailing `u32` offset index. No varints. A reader
  resolves a reference in place with one load.
- Limits (`max_bytes`, `max_nodes`, `max_strings`, `max_depth`, `max_work`, per-member `max=`) are declared in
  the descriptor, emitted as literal constants, checked by the generated reader and enforced by the generated
  writer, so a writer cannot produce a file its own reader rejects.
- Validation has two stages and is all-or-nothing. The structural stage is one forward pass with no recursion
  and one bounded allocation. The canonical stage checks node order and duplicates with two more bounded
  allocations. A view is handed out only after the whole input has passed. Every error says whether the file
  is unsupported, over a limit, or malformed.
- Generated in v1: Rust writer and reader, Java writer and reader (`byte[]`). Nothing for C or Truffle.
- The hand-written drift test is replaced by three mechanisms: one descriptor for both sides, a layout
  fingerprint in every file, and a `pinned=` fingerprint in the descriptor that fails generation when the layout
  changes.

### Non-goals

- Not a schema language and not protobuf: no optional-field presence bits, no unknown-field preservation, no
  field numbers, no maps, no schema negotiation.
- No forward or backward evolution. A reader accepts exactly one `(version, fingerprint)` pair. A consumer that
  must read two versions declares two formats with the same magic and dispatches on the generated `peek`
  (section 5). This keeps every accepted byte described by the descriptor that generated the reader, which is
  the property the drift test was approximating.
- No cycles. References point strictly backwards, so the stream is a DAG by construction.
- No nested inline children. An element that needs its own list, and a record embedded in a node, are nodes.
- No inline strings. All text goes through the string table, or through a scalar list when it is not UTF-8.
- No streaming decode and no incremental append. The index is a trailer and validation needs the whole input.
- No compression, encryption or checksum (open decision 7).
- No big-endian support. Myna models only little-endian targets (`docs/dsl.md`, targets).
- No byte compatibility with any existing hand-written codec. A consumer moves to the generated layout under
  a new version.

### Why not the value tape

`myna-values` is the wrong tool for this, for reasons that are structural and not fixable by adding a version
field:

- It is an in-memory handoff for one synchronous call: the descriptor is passed by pointer and is valid until
  the call returns. It has no stable byte layout to commit to.
- Its data model is JSON types. It has no schema-declared tags, so a per-tag layout cannot be checked, and
  `docs/values.md` lists object identity and cycles as not provided, so there are no back-references.
- It has no declared limits; the reader trusts the Rust writer in the same process.

Reusable from it: the UTF-8 validation routine (already fuzzed by `fuzz_targets/utf8_to_utf16.rs`), the
`cargo-fuzz` crate layout and its `asan_canary` target as the template for the new fuzz target, and the Java
conventions for reading little-endian words out of memory without allocating.

## 2. DSL

```text
enum Align type=u8
  value left = 0
  value center = 1
  value right = 2
end

flags StyleFlags type=u32
  bit bold = 0
  bit italic = 1
end

family Doc

format DocFile family=Doc version=1 magic=0x31434f44 pinned=9f3a61c0d2e4b877
  limit max_bytes=268435456
  limit max_nodes=4194304
  limit max_strings=1048576
  limit max_depth=128
  limit max_work=16777216
  header capability
  root Document
end

format DocFragment family=Doc version=1 magic=0x31475246
  limit max_bytes=1048576
  limit max_nodes=65536
  limit max_strings=65536
  limit max_depth=64
  limit max_work=1048576
  root Paragraph,RawText
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

element SpanStyled family=Doc tag=2
  ref style node=Style
  string text
end

union Span family=Doc elements=SpanText,SpanBreak,SpanStyled

node Style family=Doc tag=1 sharing=interned
  string face
  field flags type=u32 flags=StyleFlags
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
  list units type=u16 max=1048576
end

node Section family=Doc tag=4 sharing=unique
  string title
  refs blocks node=Block
end

node Document family=Doc tag=5 sharing=unique
  refs sections node=Section
  field source type=bytes max=1048576
end
```

The example shows each feature the first consumer asked for: two formats over one family; `Style` nodes that
are shared and `Paragraph` nodes that form a tree; a list of tagged elements whose width depends on the tag
(`Span`, with one tag that has no payload); a list of untagged elements (`Attr`); an optional reference in a
node and in an element; a `u64` scalar that carries a floating-point value as raw bits; and text held as raw
UTF-16 code units (`RawText.units`), which may contain unpaired surrogates and so cannot be a string.

Rules:

- `family NAME`: one line. It owns the node, element, `kind` and `union` namespaces and the tag spaces.
- `format NAME family=FAMILY version=N magic=0xXXXXXXXX [pinned=HEX16]`. `version` is a positive `u32`.
  `magic` is a `u32` literal, required, and unique within the module. Body lines:
  - `limit KEY=VALUE`, one per line. All five keys are **required**; there are no defaults, so a limit is
    always a decision somebody made. Generation rejects `max_bytes > 2^31 - 9`, `max_nodes > 2^31 - 1` and
    `max_strings > 2^31 - 1` (Java `int` indexing, the same ceiling `SlabLimits` uses), `max_depth > 32767`,
    and `max_work > 2^31 - 2`.
  - `header NAME`: optional, at most one. Declares the `u32` header word (section 3). The writer takes its
    value from the caller; the reader checks it against a set the caller passes and returns it.
  - `root NODE[,NODE…]`: the tags the last node may have. Kind names are allowed.
  - A format accepts exactly the node tags reachable from its roots through declared `node=` targets. A node
    of the family that no root can reach is `BadTag` in that format.
- `node NAME family=FAMILY tag=N sharing=interned|unique`. `tag` is `1..65535`, unique among the family's
  nodes; 0 is reserved as invalid. `sharing` is **required**:
  - `interned`: structurally equal nodes are one node. The writer merges them; the reader rejects two nodes
    with equal bytes.
  - `unique`: the writer never merges, and the node has exactly one reference (none when it is the root).
    The reader rejects a second.

  There is no third mode: a node that could be either shared or duplicated would give one value two encodings.
  Blocks are flat, not nested inside `family` or `format`, to match the parser's single open block (open
  decision 1). Body lines, whose order is the wire order:
  - `field NAME type=T [enum=E | flags=F]` with `T` in `u8 i8 u16 i16 u32 i32 u64 i64 f32 f64 bool`. `enum=` and
    `flags=` reuse the existing groups and their carrier rule. Both are **closed** here: the reader rejects an
    unlisted enum value and an undeclared flag bit. `offset=` is rejected: Myna assigns offsets. Floats are
    written and compared as raw bits, so NaN payloads and the sign of zero survive.
  - `field NAME type=bytes [max=N]`: a length-prefixed byte payload.
  - `string NAME`: a string-table id. There is no inline string type.
  - `ref NAME node=TARGET [optional=true]`: one back-reference. `TARGET` is a node name, a `kind` name, or `any`.
  - `refs NAME node=TARGET [max=N]`: a counted list of back-references, in order. Duplicates are allowed
    unless the target is `unique`.
  - `list NAME type=T [max=N]`: a counted list of scalars, `T` in `u8 i8 u16 i16 u32 i32 u64 i64`.
  - `list NAME element=ELEMENT [max=N]`: a counted list of untagged inline elements, all with one layout.
  - `list NAME union=UNION [max=N]`: a counted list of tagged inline elements; each element's layout is chosen
    by its tag.
- `element NAME family=FAMILY [tag=N]`. Body lines are `field` (scalars only), `string` and `ref`, with the same
  options as in a node. An element holds no lists and no `bytes`, so elements never nest. An element with a
  `tag` (`0..65535`) is used through a `union`; one without is used through `element=`. A tagged element with
  no body lines is legal.
- `kind NAME family=FAMILY nodes=A,B,…` names a set of nodes for `node=`. It shares the node namespace.
- `union NAME family=FAMILY elements=A,B,…` names a set of tagged elements for `union=`. Tags are unique
  within a union. The set is closed.
- A node with no body lines is legal (a unit variant).
- Not in v1: `when tag=` cases inside a node (the node tag already selects the layout), lists inside elements,
  nested structs, fixed arrays of scalars, lists of strings, and inline `str` fields.

Model placement (`Seam.java`): `record Family(name)`, `record Format(name, family, version, magic, pinned,
limits, headerWord, roots)`, `record Node(name, family, tag, sharing, members)`, `record Element(name, family,
tag, members)`, `record NodeKind(name, family, nodes)`, `record ElementUnion(name, family, elements)`, and
`sealed interface NodeMember permits ScalarMember, BytesMember, StringMember, RefMember, RefListMember,
ScalarListMember, ElementListMember`. `Module` gains `families`, `formats`, `nodes`, `elements`, `nodeKinds`,
`elementUnions`. `Dsl.java`: `format`, `node` and `element` join the block-opening case; `limit`, `header`,
`root`, `string`, `ref`, `refs`, `list` are new line cases valid only in their block; `family`, `kind` and
`union` are one-line declarations like `const`. A format needs no `target`: its layout is the same on all four.

## 3. Wire format

All integers are fixed-width little-endian. The file is `header | strings | body | index` and nothing else.

```text
header (40 bytes)
  0   u32  magic
  4   u32  version
  8   u64  layout fingerprint (first 8 bytes of the SHA-256 in section 6, read as LE u64)
  16  u32  header word           the declared `header` value; 0 when the format declares none
  20  u32  string_count          0..max_strings
  24  u32  string_bytes          total UTF-8 bytes, before padding
  28  u32  node_count            1..max_nodes
  32  u32  body_bytes            multiple of 4
  36  u32  reserved = 0
strings (4 * string_count + pad4(string_bytes) bytes)
  u32 end offset of string i, relative to the start of the string bytes, for each i
  the UTF-8 bytes of all strings, concatenated, zero-padded to a 4-byte boundary
body (body_bytes)
  node 0 … node node_count-1, each starting on a 4-byte boundary
index (4 * node_count bytes)
  u32 offset of node i, relative to the start of the body
```

Total length is exactly `40 + 4 * string_count + pad4(string_bytes) + body_bytes + 4 * node_count`. The input
slice is the frame: there is no outer length prefix, and trailing bytes are an error. A consumer that embeds a
format in a larger file frames it itself. String `i` is the bytes from `end[i-1]` (0 for the first) to
`end[i]`; an empty string is legal.

Node layout:

```text
0   u16  tag
2   u16  reserved = 0
4   fixed part: members in declared order, packed, no interior padding
      scalar            its width
      string            u32 string id
      ref               u32 node index, or 0xFFFFFFFF for an absent optional ref
      bytes             u32 byte length
      refs / list       u32 count
    variable part: for each bytes/refs/list member in declared order, its payload, zero-padded to a 4-byte
    boundary
      bytes             the bytes
      refs              count * u32 node indexes
      list type=T       count * width(T)
      list element=E    count elements, each the members of E packed in declared order, zero-padded to 4
      list union=U      count elements, each u16 tag, u16 reserved = 0, then the members of that tag's
                        element packed in declared order, zero-padded to 4
```

Every fixed-part offset is a generation-time constant, and the size of every element is a generation-time
constant per tag. Variable-part offsets are sums of a few lengths; a tagged list is walked element by element,
so its accessor is an iterator and not an index. Reads are unaligned; all four targets and Java's
`byteArrayViewVarHandle` permit that.

### Canonical form

A value is a root node and everything reachable from it, with equal `interned` nodes identified. It has exactly
one encoding, fixed by these rules:

1. Reserved bits and all padding are zero, and a node has no slack bytes.
2. **Strings.** The table holds each distinct string once and holds no string that nothing refers to. Ids are
   assigned in order of first use, reading nodes in index order and each node's members and elements in wire
   order.
3. **Nodes.** Every node except the root is referenced. No two `interned` nodes have equal bytes. Every
   `unique` node other than the root is referenced exactly once, counting two references from one parent as
   two.
4. **Node order.** Nodes appear in depth-first post-order from the root: a node's references are visited in
   wire order, a node that already has an index is not visited again, and a node gets the next index when all
   of its references have one.

Because children are indexes and string ids are canonical, two `interned` nodes are structurally equal exactly
when their bytes are equal, so rule 3 needs no deep comparison. Rules 2 to 4 make the order of strings and
nodes follow from the value alone. Neither writer's call order nor its hash function can change the output.

Decisions and the reasons:

- **Fixed-width, not varint.** A varint reader needs a branch per byte, a canonical-form check to stay
  deterministic, and cannot index into a node. Fixed width gives accessors that are one bounds-checked load at a
  literal offset, which is the shape SVM compiles well, and makes "this node occupies exactly these bytes" a
  simple equality. The cost is size; a consumer that cares can compress the frame.
- **Absolute index, not backward delta.** Deltas only pay off with varints. An absolute index needs no
  arithmetic against the referring node, and the validity rule is one compare: `ref < self`.
- **First-class string table.** Strings repeat across nodes, and the first consumer refers to them by id. A
  table with end offsets gives O(1) access by id, one UTF-8 check per distinct string, and a first-use ordering
  rule the reader can check with a counter.
- **Trailing offset index.** It makes `node(i)` O(1) with no reader-side table, at 4 bytes per node. The reader
  does not trust it: section 4 checks it against the walked node sizes.
- **Tag `u16` plus 16 reserved zero bits**, for nodes and tagged elements. Keeps everything 4-aligned and
  leaves room without a format change to Myna itself.
- **One header word, not general header fields.** The first consumer needs a single word a reader can test
  before it reads any node. More fields can use the reserved word later.

## 4. Reader validation

Two entry points. `validate(bytes, accept)` runs both stages and is what a consumer that compares or keys on
encoded bytes must call. `validate_structural(bytes, accept)` runs stage 1 only. Each runs its checks in this
order and stops at the first failure. Sizes are computed in 64-bit arithmetic so nothing wraps. `accept` is the
set of header-word values the caller is willing to read.

**Stage 1: structure and limits.** One forward pass.

1. `len >= 40`, then `len <= max_bytes`. (`Truncated`, `TooLarge`)
2. `magic`, then `version`, then `fingerprint`; then the header word is in `accept` (or is 0 when the format
   declares no header word); then the reserved word is zero. (`BadMagic`, `BadVersion`, `BadFingerprint`,
   `HeaderRejected`, `BadHeader`). All of this happens before any string or node is read. The order is
   deliberate: a version mismatch is the expected, explainable failure; a fingerprint mismatch at a matching
   version means the layout changed without a bump.
3. `string_count <= max_strings`; `1 <= node_count <= max_nodes`; `body_bytes % 4 == 0`; the total-length
   equation in section 3 holds exactly. (`TooManyStrings`, `TooManyNodes`, `BadLength`). Every count is now
   known to fit in the input, before anything is allocated for it.
4. String table: end offsets are non-decreasing; the last equals `string_bytes` (which is 0 when
   `string_count` is 0); padding is zero; each string is valid UTF-8 on its own. (`BadStringTable`, `BadUtf8`)
5. Allocate the side table: one `u32` (work) and one `u16` (depth, top bit = "referenced") per node, at most
   `6 * max_nodes` bytes, allocated once and fallibly. This is the only allocation in stage 1.
   (`OutOfMemory` in Rust.)
6. For each node `i` in index order:
   1. `offset[i] % 4 == 0`; `offset[0] == 0`; `offset[i] > offset[i-1]`; `offset[i] < body_bytes`. The extent is
      `[offset[i], offset[i+1])`, or to `body_bytes` for the last node. (`BadIndex`)
   2. extent `>= 4`; the tag is one this format accepts; reserved half-word is zero; extent `>=` the tag's
      fixed size. (`BadTag`, `BadNode`)
   3. Each length and count: `<=` its `max=`, and `count * minimum element size` fits in what is left of the
      extent. (`FieldTooLong`, `BadNode`)
   4. Scalars, in the node and in every element: `bool` is 0 or 1; an `enum=` value is declared; a `flags=`
      value has no undeclared bit. Floats and scalar-list items are not inspected. (`BadValue`)
   5. Each tagged element: its tag is in the union and its reserved half-word is zero. (`BadElement`)
   6. The node ends exactly at its extent and all padding bytes, in elements and after payloads, are zero.
      (`BadNode`)
   7. Each string id `s`, in wire order: `s < string_count`; `s <= next_string`, and if `s == next_string`
      then `next_string` is incremented. (`BadString`, `StringOrder`). `next_string` is one counter for the
      whole file, so this checks first-use order with no table.
   8. Each reference `r`, in a `ref`, a `refs` list or an element: `r < i` (so in range, backward, and not
      self); the target's tag is in the member's `node=` set; `0xFFFFFFFF` is accepted only for
      `optional=true`; a `unique` target is not already marked referenced. (`BadRef`, `BadRefKind`,
      `SharedUnique`). The target is then marked referenced, before the next reference is checked, so two
      references from one node to the same `unique` target are caught. The target was validated earlier in
      this same pass, so reading its tag and side-table entry is safe.
   9. `depth[i] = 1 + max(depth[children])`, 1 for a leaf; reject if `> max_depth`. (`TooDeep`)
   10. `work[i] = 1 + sum(work[children])`, saturating; reject if `> max_work`. (`TooMuchWork`)
7. Every node except the last is marked referenced. Since all references point backwards, that is equivalent
   to "reachable from the root". `next_string == string_count`, so no string is unused. The last node's tag is
   in `root`. (`Unreferenced`, `UnusedString`, `BadRoot`)

**Stage 2: canonical form.** Runs only after stage 1 has passed, so every reference is already known to be
safe.

8. Node order. Walk from the root with an explicit stack and a counter that starts at 0. A reference below the
   counter is already placed and is skipped; any other is entered. When a node's references are exhausted its
   index must equal the counter, which is then incremented. (`NodeOrder`). The stack never exceeds `max_depth`
   frames, each node is entered once, and each reference is read once, so the walk is linear in the body. It
   needs no visited set: in a canonical stream the placed nodes are exactly the indexes below the counter.
9. Duplicates. No two `interned` nodes have equal bytes, and no two strings are equal. (`DuplicateNode`,
   `DuplicateString`). Recommended mechanism: sort `u32` indexes by length and then bytes with a merge sort and
   compare neighbours. That is `O(len * log n)` in the worst case and has no hash function for an input to
   attack (open decision 18).

Stage 2 allocates at most `16 * max_depth` bytes for the stack and `8` bytes per `interned` node and per
string for the sort. Both are sized from counts stage 1 already checked.

**How work is counted.** Stage 1 is linear in `len`, so `max_bytes` already bounds it. `max_work` bounds
something different: the number of node visits a naive recursive walk from the root makes when it does not
memoise shared children. With back-references that count can be exponential in the node count (node `i`
referencing node `i-1` twice gives `2^i`), which is the decompression-bomb shape. `work[i]` is exactly that
visit count for the subtree at `i`, so after validation a consumer may walk the tree recursively without its
own budget: at most `max_work` visits and `max_depth` frames. References held in inline elements count as
children. Strings and payload bytes are not counted in work; they are bounded by `max_bytes` and are never
duplicated by sharing unless the consumer copies them.

**Errors.** One value type per family: a kind (the names above, with stable integer codes emitted as
literals), the byte offset where the check failed, and the node index (or `0xFFFFFFFF` outside step 6). Every
kind belongs to exactly one class, and the class is readable from the code without a table:

| Class | Meaning | Kinds |
|---|---|---|
| `Unsupported` | Not a file this reader is for. | `BadMagic`, `BadVersion`, `BadFingerprint`, `HeaderRejected` |
| `Limit` | Well-formed so far, but over a declared limit, or the bounded side tables could not be allocated. | `TooLarge`, `TooManyStrings`, `TooManyNodes`, `FieldTooLong`, `TooDeep`, `TooMuchWork`, `OutOfMemory` |
| `Malformed` | Not a valid encoding. | every other kind, including the canonical-form kinds |

No strings are formatted on the reject path. Rust and Java return the same `(kind, offset, node)` for the same
input; the golden rejects in section 7 test exactly that.

**Guarantees on reject.** No view, cursor or partially decoded value is ever exposed. No panic and no
out-of-bounds read: every load is preceded by a bounds check against the extent. No recursion, so no stack
growth with input. Every allocation is sized from a count that was already checked against its limit and
against `len`. Stage 1 is `O(len)`.

**Guarantees on accept.** Every accessor on a validated view is total. After `validate`, the input is the
canonical encoding of its value: decoding it and encoding the result again returns the same bytes. The
generated accessors still use bounds-checked reads in v1 (no `unsafe` in Rust, plain array access in Java);
removing those checks is a measured follow-up, not a v1 goal.

## 5. Generated API

Node views, element types, ids and tags are generated once per family. Constants, `validate`, `peek` and
`finish` are generated once per format.

### Writers (both languages)

A writer is a builder for one family. The caller creates strings and nodes in any order that puts children
before parents; `finish` then lays the file out in canonical form. The bytes depend only on the value that
was built.

- Strings are interned by content when created. Ids handed to the caller are provisional.
- Creating an `interned` node looks its encoded bytes up in a map and returns the existing id when there is
  one. Creating a `unique` node always returns a new id, and using that id a second time is an error.
- The writer keeps the same depth and work tables as the reader and fails at node creation when `max_depth`,
  `max_work` or a member `max=` would be exceeded.
- `finish(root, header_word)` walks from the root in the canonical node order, renumbers nodes and strings,
  drops anything the root does not reach (open decision 19), checks `max_nodes`, `max_strings` and
  `max_bytes`, and writes the file. Floats are written as raw bits.
- The two writers share no code and need not share a hash function: the canonical form leaves no choice.

### Rust

`#![no_std]` with `alloc`. No dependencies beyond `core`/`alloc` (so the interning maps are `BTreeMap`).

- Constants per format: `MAGIC`, `VERSION`, `FINGERPRINT`, `MAX_BYTES`, `MAX_NODES`, `MAX_STRINGS`, `MAX_DEPTH`,
  `MAX_WORK`. Per family: a `#[repr(u16)] enum Tag` and the error codes.
- Writer: `DocWriter::new()`; `string(&str) -> StrId`; one method per node, for example
  `style(face: StrId, flags: StyleFlags, align: Align, size_bits: u64, parent: Option<StyleId>) ->
  Result<StyleId, WriteError>` and `paragraph(style: StyleId, spans: impl IntoIterator<Item = Span>, attrs:
  impl IntoIterator<Item = Attr>) -> Result<ParagraphId, WriteError>`; and one `finish` per format, for example
  `finish_doc_file(root: DocumentId, capability: u32) -> Result<Vec<u8>, WriteError>`.
  - Ids are `#[repr(transparent)]` `u32` newtypes per node and per `kind`, with `From` conversions into the
    kinds a node belongs to, so a wrong-kind or forward reference is a compile error within one writer.
  - Ids of `interned` nodes and `StrId` are `Copy`. Ids of `unique` nodes are not `Copy` or `Clone`, so a
    second reference to a `unique` node is a compile error.
  - Elements are plain generated types: a struct for an untagged element (`Attr { key, value, important }`)
    and an enum for a union (`Span::Text { text }`, `Span::Break`, `Span::Styled { style, text }`).
  - Ids from a different writer are a logic error caught by the range check, not by the type.
- Reader: `DocFile::validate(&[u8], accept: &[u32]) -> Result<DocFile<'_>, FormatError>` and
  `validate_structural` with the same shape; `doc.capability()`; `doc.root()`; typed `Copy` views
  (`Paragraph<'a>` with `style() -> Style<'a>`, `spans()`, `attrs()`); `enum Block<'a>` for a kind and
  `enum Span<'a>` for a union, both `match`-able; `refs` and `list` members are iterators (`ExactSizeIterator`
  except over a union); `string` members return `&'a str` borrowed from the input; a scalar list also offers
  its raw `&'a [u8]`. `FormatError::class()` returns `Unsupported`, `Limit` or `Malformed`.
- `DocFile::peek(&[u8]) -> Option<Peek>` checks the magic only and returns the version and the header word,
  so a caller can choose a reader or an `accept` set before validating.

### Java

Pure Java, no FFM downcalls, no reflection, no per-node objects.

- `<Family>Nodes`: the tags, the error codes and classes as literal `public static final` constants, and the
  accessors. `<Format>Format`: the per-format constants. This follows the `<Class>Constants` rule: immediates,
  zero image-heap objects.
- `<Format>Reader` over `byte[]` (with a base offset and length): `static long check(byte[] b, int off, int
  len, int[] accept)` returns 0 or the packed `(kind, offset, node)`; `checkStructural` has the same shape;
  `static void require(...)` throws a stackless `<Family>Exception` carrying the same three values and the
  class; `static int headerWord(byte[] b, int off)` and `static long peek(...)` read the header.
- Accessors are static methods on `int` node indexes: `int tag(byte[] b, int base, int node)`,
  `int Paragraph_style(...)`, `int Section_blocks_count(...)`, `int Section_blocks_at(..., int i)`,
  `long Style_size_bits(...)`. A string member has `int Section_title_id(...)` and `String Section_title(...)`,
  with `string_offset(id)` and `string_length(id)` for callers that do not want the `String`. A scalar list
  has `_count`, `_at` and `_offset`. Element lists are read through a cursor that is an `int` byte offset:
  `Paragraph_spans_first(...)`, `Span_next(..., int at)`, `Span_tag(..., int at)`, `SpanStyled_style(..., int
  at)`. Kinds and unions are dispatched with a `switch` on the tag, which compiles to immediate compares.
- `<Family>Writer`: the builder described above, over growable `byte[]` storage, with `int` ids. A second
  reference to a `unique` node is a run-time `WriteError`, since Java cannot express it in the type. Floats use
  `doubleToRawLongBits` and `floatToRawIntBits`, never the canonicalising variants.
- **No `MemorySegment` reader in v1.** Measured on the pinned fork, 25.5.5-elide.31, linux-amd64, `-O2`.
  The probe walked 4M 8-byte nodes (`u16` tag, `u32` backward reference, reference check per node), best of 15:

  | Input | JVM | Native Image |
  |---|---|---|
  | `byte[]` + `byteArrayViewVarHandle` (LE) | 1.5 ms | 3.7 ms |
  | `ByteBuffer` (LE) | 1.5 ms | 9.4 ms |
  | heap `MemorySegment` | 2.5 ms | 262 ms |
  | native `MemorySegment` (`Arena.ofConfined`) | 1.6 ms | 257 ms |

  Profiling (elide-dev/graal#250) shows two causes. With the layout constants initialized at run time, every
  access goes through Native Image's generic reflective method-handle path. Initializing the holder at build time
  brings segments to 28 ms, still 7.5 times slower than `byte[]`, because the segment internals
  (`unsafeGetOffset`, `unsafeGetBase`, `maxAlignMask`) stay virtual calls. `.28` behaves the same. A lint for
  both is proposed in elide-dev/trufflecheck#12. Revisit a segment reader if graal#250 is fixed.

  Segment access builds and runs on Native Image with no extra configuration. Out-of-bounds reads throw
  `IndexOutOfBoundsException` there as on the JVM. But the accessors are about 70 times slower than `byte[]` in
  an image, which matches the AGENTS.md rule to use word operations, never `MemorySegment` accessors, on
  Substrate. This was a single-host micro-probe; the optimized instructions were not inspected. A convenience
  `fromSegment` can take a heap segment's backing `byte[]` (`heapBase()`), and otherwise copy into one, since
  validation reads every byte anyway. A zero-copy reader over mapped native memory needs SVM word operations
  and is deferred.
- No `ByteBuffer` variant: it is 2.5 times slower than `byte[]` in an image.
- Per AGENTS.md the readers get JVM unit tests plus an SVM integration test.

### C and Truffle

Nothing in v1. `seam.h` is unchanged. A Truffle guest-visible view over a validated stream is plausible later
and would sit on `<Format>Reader`; it needs its own PE measurements.

## 6. Canonical JSON, fingerprint and versioning

- `seam.json` gains `families` (sorted by name), each with its `nodes` (sorted by tag, members in declared
  order), `elements`, `kinds` and `unions`, and `formats` (sorted by name), each with `family`, `version`,
  `magic`, `limits`, `headerWord` and `roots`. The keys are written only when the module declares a family, so
  every existing module keeps its `seam.abi`.
- The **layout fingerprint** in the file header is a second, narrower hash. It is separate from the
  call-boundary fingerprint and shares no inputs with it. It changes only when the set of byte sequences the
  reader accepts changes. It is SHA-256 over a canonical JSON that holds numbers and fixed keys only.
  - **Hashed:**
    - a wire-rules revision number, which changes only if Myna changes how a descriptor maps to bytes (for
      example the header size or the padding rule);
    - `magic` and `version`;
    - whether the format declares a header word;
    - the root tag set, sorted;
    - for each node tag the format accepts (section 2), sorted by tag: the tag, its `sharing`, and its members
      in wire order. A member contributes its kind, scalar type, `optional`, resolved target tag set (sorted)
      and `max`. An element list contributes the full layout of its element, or of every element in its union
      sorted by element tag;
    - the value set of every referenced enum (sorted) and the bit mask of every referenced flag set.
  - **Not hashed:**
    - every name: format, family, node, element, member, kind, union, enum, enum value, flag bit and header
      word. A rename does not orphan files on disk;
    - the five format limits. Raising a limit lets a new reader open old files; lowering one can reject old
      files with a `Limit` error, which is the honest error (open decision 9);
    - the generator version, `targetAbi`, the module `abi=`, Java naming (`java_class`, `java_package`),
      comments, and the order of declarations in the file. A Myna upgrade does not change it unless the
      wire-rules revision changes;
    - nodes of the family the format's roots cannot reach, and the other formats of the family. Adding a node
      that only another format uses does not change this format's fingerprint.
  - Per-member `max=` is hashed: it is part of what a node may contain.
- `pinned=HEX16` is the first 8 bytes of that hash, the same value the file header carries. When present,
  generation fails if the computed value differs, with a message naming the two values and saying to bump
  `version` and update the pin. Consequences:
  - a layout edit cannot ship under an old version number by accident;
  - a Myna upgrade that would change the fingerprint fails at generation time, before any reader that
    rejects existing files is built;
  - a rename, a limit change or a regenerated build with a newer Myna leaves the pin valid.

  Without `pinned=`, generation prints the value so it can be pasted in (open decision 20).
- `version` is the author's number and is what a human reasons about; the fingerprint is the proof. The module's
  `abi=` is unrelated and is not written to the file.

## 7. Testing

- **Fixture.** `tests/formats/doc.seam` (the example above, widened to cover every member kind, an empty node,
  an empty tagged element, an optional ref both ways in a node and in an element, a shared `interned` subtree,
  an empty string, and a `u16` list holding an unpaired surrogate).
- **Golden accepts.** A Rust test and a Java test each build the same fixed set of values with the generated
  writer and compare the bytes to checked-in `tests/formats/golden/*.bin`. Each value is built at least twice
  in different creation orders, and with equal `interned` nodes created separately, and must give the same
  bytes every time. The Rust and Java readers each dump every golden to a canonical text form, compared to a
  checked-in `*.txt`. Any writer/writer or writer/reader disagreement shows up as a byte or text diff.
- **Golden rejects.** At least one checked-in file per error kind, including the boundary cases:
  - references: a forward ref, a self ref, `ref == node_count`, a wrong-kind ref, an absent non-optional ref;
  - limits: depth exactly `max_depth` (accept) and `max_depth + 1` (reject), a doubling chain that crosses
    `max_work`, a count that the remaining bytes cannot hold, a size computation that would wrap in 32 bits;
  - closed sets: an unknown node tag, a tag of the family that this format's roots cannot reach, an unknown
    element tag, an unlisted enum value, a `bool` of 2, an undeclared flag bit;
  - framing: a one-byte truncation, one trailing byte, an index entry off by 4, a non-zero padding byte;
  - strings: an id out of range, ids used out of first-use order, an unused string, two equal strings, a
    string split inside a UTF-8 sequence;
  - sharing: an unreferenced node, two equal `interned` nodes, a `unique` node referenced twice by two parents
    and twice by one;
  - order: two sibling subtrees swapped;
  - header: a header word outside `accept`, and a non-zero word in a format that declares none.

  Both readers must return the same `(kind, offset, node)`, and each file's class is asserted.
  `validate_structural` must accept exactly the rejects whose kind belongs to stage 2.
- **Fuzzing.** A new target in `crates/myna-values/fuzz/fuzz_targets/` (or a sibling fuzz crate if the format
  runtime does not live in `myna-values`; open decision 8) over the fixture's generated reader: arbitrary bytes
  must never panic; on accept, a full recursive walk must make at most `MAX_WORK` visits and reach at most
  `MAX_DEPTH`; and rebuilding the value through the writer, in an order chosen by the fuzzer, must reproduce
  the input byte-for-byte. A structure-aware second target builds valid values, mutates one field, and checks
  the reader's verdict against a slow reference checker.
- **Java differential.** The fuzz corpus (accepts and rejects, minimised) is replayed through the Java reader
  in a unit test, comparing verdicts with Rust, and every accepted input is rebuilt through the Java writer
  and compared with the input. Jazzer is not added in v1.
- **Generator tests.** Canonical JSON and fingerprint stability: a rename of anything does not change the
  layout fingerprint, nor does a limit change, nor does adding a node only another format reaches; reordering
  members, changing `sharing`, a `max=` or an enum value set does. Also the `pinned=` mismatch message and each
  DSL rejection.
- **Drift test.** A consumer deletes its hand-written drift test once both sides are generated. What replaces
  it is stronger at each stage: same descriptor at build time, `pinned=` at generation time, header fingerprint
  at run time.

## 8. Decisions

Numbers are kept from the first draft so earlier references still hold.

### Settled

2. **References only, or inline children too?** *(resolved per #167 discussion)* Child nodes stay
   references-only, as recorded. The first consumer chose option (b): counted lists of inline elements with no
   nesting, tagged or untagged, holding scalars, string ids and optional or required refs. It also needs
   counted scalar lists and `u64` scalars, and stores floats as raw bits. It does not need general inline
   children: an element that needs its own list, and a record embedded in a node, become nodes.
3. **Fixed-width or varint integers?** *(resolved per #167 discussion)* Fixed-width, as recorded. The first
   consumer does not need its current bytes preserved and raised no size concern. If size turns out to matter,
   measure a compressed frame before adding varints.
4. **Java inputs in v1.** *(resolved per #167 discussion)* `byte[]` only, as recorded and measured (section 5).
   The first consumer needs nothing else for v1.
5. **Java writer.** *(resolved per #167 discussion)* In v1, as recorded. The first consumer needs a writer and
   a reader in each language, and the two writers must produce identical bytes (item 13).
6. **Reject unreferenced nodes?** *(resolved per #167 discussion)* Yes. The first consumer requires that every
   node except the root has a parent.
11. **Interned strings.** *(resolved per #167 discussion)* A first-class string table referenced by `u32` id,
    range-checked and UTF-8-checked by the reader, interned in order of first use with no unused entry. No
    inline `str`. This replaces the earlier recommendation to model strings as leaf nodes.
12. **Writer deduplication.** *(resolved per #167 discussion)* Declared per node type and done by the
    generated writer: `sharing=interned` or `sharing=unique`. The reader rejects two equal `interned` nodes and
    a second reference to a `unique` node. This replaces the earlier recommendation to leave uniqueness out of
    the reader.
13. **Byte-identical writers.** *(resolved per #167 discussion)* Required, and stronger than first proposed:
    the bytes follow from the value alone, not from the sequence of writer calls (section 3, canonical form).
14. **Declared header word.** *(resolved per #167 discussion)* One declared `u32` word. The writer takes it
    from the caller. The reader checks it against a value or set the caller passes, before reading any node,
    and returns it. Rules tying the word to the content stay with the consumer. Related and also settled:
    several formats share node declarations through a `family` and differ in magic and root.
15. **Magic width.** *(resolved per #167 discussion)* `u32`. The first consumer moves to the new layout under
    a new version and does not need its current bytes matched.
16. **What `max_work` counts.** *(resolved per #167 discussion)* As defined in section 4, computed in the
    forward pass. The generated writer enforces the same limits.
17. **Structural or semantic.** *(resolved per #167 discussion)* The split stands; semantic checks stay with
    the consumer. Structural, per the answer: closed sets for tags, enums, bools and flags; counts checked
    against the remaining bytes before any allocation; trailing bytes rejected; every non-root node referenced
    and a `unique` node exactly once; errors that separate "over a limit" from "malformed". The earlier `min=N`
    proposal is dropped; nobody asked for it.

### Accepted 2026-11-18

The maintainer accepted each recommendation below as written.

1. **Flat blocks, or nodes nested inside `family … end`?** Recommend flat. The parser has one open block
   today and every other declaration is flat; nesting is nicer to read but is a parser change with no semantic
   gain. The spellings `family`, `element`, `union`, `sharing=` and `header` are this draft's choice; the first
   consumer left the spelling to us.
7. **Checksum.** Recommend none. Validation already makes any byte string safe to read; integrity against bit
   rot belongs to whatever stores the frame. An opt-in `checksum=crc32c` using the reserved header word could
   be added later without changing node layout.
8. **Where the Rust runtime pieces live.** Recommend fully generated code with no runtime crate, except the
   UTF-8 check, which should be `core::str::from_utf8` unless a measurement says the `myna-values` routine is
   needed. The canonical stage adds a merge sort and a stack walk per family; if two families in one build make
   that duplication matter, move those two routines to a small runtime crate. That decides whether the fuzz
   target sits in the existing fuzz crate or a new one.
9. **Limits in the layout fingerprint?** Recommend excluding the five format limits (section 6). Including them
   is stricter but makes raising a limit a breaking format change, which is the opposite of what the first
   consumer asked of the fingerprint.
10. **Stored depth/work per node.** An alternative puts `depth:u16` and `work:u32` in each node header so the
    reader verifies them locally and allocates nothing. Recommend not doing this in v1: it adds 4 to 8 bytes
    per node on disk to save one bounded allocation.
18. **Canonical stage: on by default, and how duplicates are found.** The first consumer said a canonical
    check in the reader is welcome but not required, since it can decode, encode and compare. Its sharing
    example does ask the reader to reject duplicates. Recommend: `validate` runs both stages and
    `validate_structural` is the cheaper opt-out; duplicates are found by a merge sort, not a hash table, so
    worst-case time is `O(len * log n)` and no input can force collisions. The cost is not measured. If it is
    too high on real files, the fallback is to drop step 9 from the reader and rely on re-encoding.
19. **Unreached nodes and strings at `finish`.** Recommend dropping them silently. Hash-consing code builds
    shared nodes it may not end up using, and dropping keeps the output a function of the value. The
    alternative is a `WriteError`, which catches a forgotten subtree but makes speculative construction an
    error.
20. **Make `pinned=` mandatory?** Recommend yes once a format has files on disk: required for generation
    unless a `draft=true` attribute is set on the format. Without it, an unintended layout change is caught
    only at run time, by `BadFingerprint`.
21. **Per-string length limit.** This draft bounds strings only by `max_strings` and `max_bytes`. Recommend
    leaving it there unless the first consumer wants a `max_string_bytes` limit; ask before adding a sixth
    required limit.

## 9. What this draft does not establish

- **Fit against the first consumer.** Its answers on myna#167 are applied above, and an earlier pass skimmed
  its current codecs. The example in section 2 is still generic: it has not been rewritten as either of the
  first consumer's two formats, and that rewrite is the real test of the DSL. The first consumer has a third
  codec that does not wait on this issue; v1 is not sized for it.
- **Cost of the canonical stage.** Nothing is measured: not the stack walk, not the duplicate sort, not the
  writer's interning maps or its renumbering pass at `finish`.
- **The truffledata slab probably does not fit this design as-is**, and is not a target of v1. From a skim,
  its record layouts come from a runtime `Layout` (protobuf descriptors), not a static descriptor, and it has
  mode flags (owning, borrowing, lax, certified, projected), lazy children and a handle table. This spec
  covers formats whose node layouts are known when Myna runs.
- Output file names, and how families and formats interact with `java_class`/`java_package`, are not settled.
- No measurement backs the SVM code-shape claims, the fixed-width size cost, or the cost of the validation
  side table.
