use allocator_api2::alloc::{AllocError, Global};
use myna_values::*;
use proptest::prelude::*;
use serde::{
    Serialize,
    ser::{SerializeMap, SerializeSeq},
};
use serde_json::{Value, json};
use std::{
    alloc::Layout,
    collections::BTreeMap,
    ptr::NonNull,
    sync::{
        Arc,
        atomic::{AtomicUsize, Ordering},
    },
};

fn decode(view: View<'_>, index: usize) -> Value {
    let node = view.nodes()[index];
    match node.kind() {
        NULL => Value::Null,
        BOOL => Value::Bool(node.payload != 0),
        I64 => Value::from(node.payload as i64),
        U64 => Value::from(node.payload),
        F64 => Value::from(f64::from_bits(node.payload)),
        STRING => Value::from(view.string(index).unwrap().as_str()),
        ARRAY | OBJECT => {
            let mut cursor = index + 1;
            if node.tag == ARRAY {
                let mut values = Vec::new();
                for _ in 0..node.len {
                    values.push(decode(view, cursor));
                    cursor = view.nodes()[cursor].subtree_end(cursor) as usize;
                }
                assert_eq!(cursor, node.next as usize);
                Value::Array(values)
            } else {
                let mut values = serde_json::Map::new();
                for _ in 0..node.len {
                    let key = decode(view, cursor).as_str().unwrap().to_owned();
                    cursor += 1;
                    values.insert(key, decode(view, cursor));
                    cursor = view.nodes()[cursor].subtree_end(cursor) as usize;
                }
                assert_eq!(cursor, node.next as usize);
                Value::Object(values)
            }
        }
        _ => panic!("invalid tag"),
    }
}
fn equivalent<T: Serialize>(value: &T) {
    let bump = Bump::new();
    let mut doc = Document::new(&bump);
    assert_eq!(
        decode(doc.encode(value).unwrap(), 0),
        serde_json::to_value(value).unwrap()
    );
}

#[test]
fn structs_attributes_enums_and_dynamic_json() {
    #[derive(Serialize)]
    struct Data<'a> {
        #[serde(rename = "displayName")]
        name: &'a str,
        #[serde(skip_serializing_if = "Option::is_none")]
        omitted: Option<u8>,
        #[serde(flatten)]
        extra: BTreeMap<&'a str, Value>,
        values: Vec<Option<i64>>,
    }
    equivalent(&Data {
        name: "a\0🦀",
        omitted: None,
        extra: BTreeMap::from([("nested", json!({"a": [true, null, {}, []]}))]),
        values: vec![Some(i64::MIN), None],
    });
    #[derive(Serialize)]
    enum Enum {
        Unit,
        New(String),
        Tuple(u32, bool),
        Struct { a: Vec<u8> },
    }
    for value in [
        Enum::Unit,
        Enum::New("text".into()),
        Enum::Tuple(7, true),
        Enum::Struct { a: vec![1, 2] },
    ] {
        equivalent(&value);
    }
    #[derive(Serialize)]
    #[serde(tag = "kind", content = "value")]
    enum Tagged {
        Test(Vec<f64>),
    }
    equivalent(&Tagged::Test(vec![-0.0, 2.5]));
    equivalent(&json!({"number": u64::MAX, "flag": false, "empty": "", "items": [[], {}]}));
    equivalent(&('🦀', (), Some(4), [1_u16, 2]));
}

#[test]
fn layout_and_numeric_boundaries() {
    assert_eq!(std::mem::size_of::<Node>(), 24);
    assert_eq!(std::mem::align_of::<Node>(), 8);
    assert_eq!(std::mem::offset_of!(Node, payload), 8);
    assert_eq!(std::mem::offset_of!(Node, next), 16);
    assert_eq!(std::mem::size_of::<RawView>(), 56);
    assert_eq!(std::mem::offset_of!(RawView, nodes), 8);
    assert_eq!(std::mem::offset_of!(RawView, node_count), 16);
    assert_eq!(std::mem::offset_of!(RawView, bytes), 24);
    assert_eq!(std::mem::offset_of!(RawView, byte_count), 32);
    let bump = Bump::new();
    let mut doc = Document::new(&bump);
    for value in [i64::MIN, -1, 0, i64::MAX] {
        let node = doc.encode(&value).unwrap().nodes()[0];
        assert_eq!(node.tag, I64);
        assert_eq!(node.payload as i64, value);
    }
    assert_eq!(doc.encode(&u64::MAX).unwrap().nodes()[0].payload, u64::MAX);
    assert_eq!(doc.encode(&(u64::MAX as i128)).unwrap().nodes()[0].tag, U64);
    assert_eq!(
        doc.encode(&-0.0).unwrap().nodes()[0].payload,
        (-0.0_f64).to_bits()
    );
    assert!(matches!(doc.encode(&u128::MAX), Err(Error::IntegerRange)));
    assert!(matches!(doc.encode(&i128::MIN), Err(Error::IntegerRange)));
    for value in [f64::NAN, f64::INFINITY, f64::NEG_INFINITY] {
        assert!(matches!(doc.encode(&value), Err(Error::NonFinite)));
        assert!(doc.view().is_none());
    }
}

#[test]
fn non_finite_floats_are_explicit_and_survive_reuse() {
    use myna_values::{ALLOW_NON_FINITE, Options};
    let options = Options {
        allow_non_finite: true,
    };
    let bits = [
        f64::INFINITY.to_bits(),
        f64::NEG_INFINITY.to_bits(),
        0x7ff8_0000_0000_0042,
        (-0.0_f64).to_bits(),
        1.5_f64.to_bits(),
    ];
    let mut doc = Document::with_options_in(Global, Limits::default(), options);
    for bits in bits {
        let value = f64::from_bits(bits);
        let view = doc.encode(&value).unwrap();
        assert_eq!(view.flags(), ALLOW_NON_FINITE);
        assert_eq!(view.nodes()[0].payload, bits);
        let raw = view.raw();
        // SAFETY: descriptor and borrowed document remain live for the read.
        assert_eq!(unsafe { (*raw.as_ptr()).flags }, ALLOW_NON_FINITE);
        doc.clear();
    }
    for value in [f32::INFINITY, f32::NEG_INFINITY, f32::NAN] {
        let view = doc.encode(&value).unwrap();
        let actual = f64::from_bits(view.nodes()[0].payload);
        assert!(if value.is_nan() {
            actual.is_nan()
        } else {
            actual == f64::from(value)
        });
    }
    let mut builder = myna_values::Builder::with_options_in(Global, Limits::default(), options);
    for value in [f64::INFINITY, f64::NEG_INFINITY, f64::NAN] {
        builder.clear();
        builder.value(&value).unwrap();
        let view = builder.finish().unwrap();
        assert_eq!(view.flags(), ALLOW_NON_FINITE);
        assert_eq!(view.nodes()[0].payload, value.to_bits());
    }
    builder.clear();
    assert!(matches!(
        builder.exact_number(".inf"),
        Err(Error::InvalidNumber)
    ));
    assert!(builder.finish().is_err());
    builder.clear();
    builder.value(&f64::INFINITY).unwrap();
    assert_eq!(builder.finish().unwrap().flags(), ALLOW_NON_FINITE);
    let mut strict = myna_values::Builder::new_in(Global);
    assert!(matches!(
        strict.value(&f64::INFINITY),
        Err(Error::NonFinite)
    ));
    assert!(strict.finish().is_err());
    strict.clear();
    strict.value(&1.0).unwrap();
    assert_eq!(strict.finish().unwrap().flags(), 0);
}

struct Transient;
impl Serialize for Transient {
    fn serialize<S: serde::Serializer>(&self, s: S) -> Result<S::Ok, S::Error> {
        s.serialize_str(&format!("temporary {}", 42))
    }
}
struct UnknownLength;
impl Serialize for UnknownLength {
    fn serialize<S: serde::Serializer>(&self, s: S) -> Result<S::Ok, S::Error> {
        let mut seq = s.serialize_seq(None)?;
        seq.serialize_element(&Transient)?;
        seq.serialize_element(&vec![1, 2, 3])?;
        seq.end()
    }
}
#[test]
fn transient_strings_unknown_lengths_and_duplicate_keys() {
    equivalent(&UnknownLength);
    struct Duplicate;
    impl Serialize for Duplicate {
        fn serialize<S: serde::Serializer>(&self, s: S) -> Result<S::Ok, S::Error> {
            let mut map = s.serialize_map(None)?;
            map.serialize_entry("x", &1)?;
            map.serialize_entry("x", &2)?;
            map.end()
        }
    }
    let mut doc = Document::new_in(Global);
    let view = doc.encode(&Duplicate).unwrap();
    assert_eq!(view.nodes()[0].len, 2); // Preserve entries; consumers choose lookup policy.
    assert_eq!(view.nodes().len(), 5);
    assert!(view.bytes().is_empty());
}

#[derive(Clone)]
struct Counting(Arc<AtomicUsize>, Arc<AtomicUsize>);
// SAFETY: delegate allocation and deallocation unchanged to Global.
unsafe impl Allocator for Counting {
    fn allocate(&self, layout: Layout) -> Result<NonNull<[u8]>, AllocError> {
        self.0.fetch_add(1, Ordering::Relaxed);
        Global.allocate(layout)
    }
    unsafe fn deallocate(&self, ptr: NonNull<u8>, layout: Layout) {
        self.1.fetch_add(1, Ordering::Relaxed);
        unsafe { Global.deallocate(ptr, layout) }
    }
}
#[test]
fn custom_allocator_routes_both_buffers_and_reuses_capacity() {
    let allocations = Arc::new(AtomicUsize::new(0));
    let deallocations = Arc::new(AtomicUsize::new(0));
    let mut doc = Document::new_in(Counting(allocations.clone(), deallocations.clone()));
    let value = json!({"message": "hello", "nested": [1,2,3]});
    doc.encode(&value).unwrap();
    let before = allocations.load(Ordering::Relaxed);
    assert!(before >= 2);
    let capacities = doc.capacities();
    for _ in 0..100 {
        doc.encode(&value).unwrap();
    }
    assert_eq!(allocations.load(Ordering::Relaxed), before);
    assert_eq!(doc.capacities(), capacities);
    drop(doc);
    assert_eq!(
        allocations.load(Ordering::Relaxed),
        deallocations.load(Ordering::Relaxed)
    );
    // Resetting bumpalo after dropping its document is also supported.
    let mut bump = Bump::new();
    {
        Document::new(&bump).encode(&value).unwrap();
    }
    bump.reset();
    equivalent(&value);
}
#[derive(Clone)]
struct Failing;
// SAFETY: never returns an allocation; deallocation is unreachable.
unsafe impl Allocator for Failing {
    fn allocate(&self, _: Layout) -> Result<NonNull<[u8]>, AllocError> {
        Err(AllocError)
    }
    unsafe fn deallocate(&self, _: NonNull<u8>, _: Layout) {
        unreachable!()
    }
}

#[derive(Default)]
struct AllocationState {
    attempts: AtomicUsize,
    allocated: AtomicUsize,
    released: AtomicUsize,
    live_bytes: AtomicUsize,
    fail_after: AtomicUsize,
}
#[derive(Clone)]
struct FailAfter(Arc<AllocationState>);
impl FailAfter {
    fn new(limit: usize) -> Self {
        let state = Arc::new(AllocationState::default());
        state.fail_after.store(limit, Ordering::Relaxed);
        Self(state)
    }
    fn allow_allocations(&self) {
        self.0.fail_after.store(usize::MAX, Ordering::Relaxed);
    }
    fn attempts(&self) -> usize {
        self.0.attempts.load(Ordering::Relaxed)
    }
    fn assert_released(&self) {
        assert_eq!(self.0.live_bytes.load(Ordering::Relaxed), 0);
        assert_eq!(
            self.0.allocated.load(Ordering::Relaxed),
            self.0.released.load(Ordering::Relaxed)
        );
    }
}
// SAFETY: successful allocations and their matching deallocations delegate to
// Global with unchanged layouts; failures never expose partially allocated memory.
unsafe impl Allocator for FailAfter {
    fn allocate(&self, layout: Layout) -> Result<NonNull<[u8]>, AllocError> {
        let attempt = self.0.attempts.fetch_add(1, Ordering::Relaxed);
        if attempt >= self.0.fail_after.load(Ordering::Relaxed) {
            return Err(AllocError);
        }
        let allocation = Global.allocate(layout)?;
        self.0.allocated.fetch_add(1, Ordering::Relaxed);
        self.0
            .live_bytes
            .fetch_add(layout.size(), Ordering::Relaxed);
        Ok(allocation)
    }
    unsafe fn deallocate(&self, ptr: NonNull<u8>, layout: Layout) {
        let previous = self
            .0
            .live_bytes
            .fetch_sub(layout.size(), Ordering::Relaxed);
        assert!(previous >= layout.size());
        self.0.released.fetch_add(1, Ordering::Relaxed);
        // SAFETY: this pointer and layout came unchanged from Global above.
        unsafe { Global.deallocate(ptr, layout) }
    }
}

#[test]
fn allocation_fail_after_each_partial_construction_recovers_and_releases() {
    let value = json!({
        "message": "owned text longer than the inline representation",
        "nested": (0..32).map(|i| json!({"index": i, "text": "another owned text allocation"})).collect::<Vec<_>>()
    });
    let baseline = FailAfter::new(usize::MAX);
    {
        let mut doc = Document::new_in(baseline.clone());
        assert_eq!(decode(doc.encode(&value).unwrap(), 0), value);
    }
    baseline.assert_released();
    let count = baseline.attempts();
    assert!(count > 2, "exercise growth after both buffers exist");
    for fail_after in 0..=count {
        let allocator = FailAfter::new(fail_after);
        {
            let mut doc = Document::new_in(allocator.clone());
            let result = doc.encode(&value);
            if fail_after < count {
                assert!(
                    matches!(result, Err(Error::Allocation)),
                    "failure point {fail_after}"
                );
                assert!(doc.view().is_none(), "partial document escaped");
            } else {
                assert_eq!(decode(result.unwrap(), 0), value);
            }
            allocator.allow_allocations();
            assert_eq!(decode(doc.encode(&value).unwrap(), 0), value);
            let attempts = allocator.attempts();
            assert_eq!(decode(doc.encode(&value).unwrap(), 0), value);
            assert_eq!(
                allocator.attempts(),
                attempts,
                "capacity reuse after recovery"
            );
        }
        allocator.assert_released();
    }

    fn populate<'a>(
        builder: &mut Builder<'a, FailAfter>,
        value: &Value,
        backing: &'a [u8],
    ) -> Result<(), Error> {
        // Grow node, owned-text, frame and borrowed-region storage independently.
        for _ in 0..8 {
            builder.begin_array()?;
            let region = builder.region(backing)?;
            builder.string_ref(region, 0, backing.len() as u32)?;
        }
        builder.value(value)?;
        for _ in 0..8 {
            builder.end()?;
        }
        Ok(())
    }
    let backing = "borrowed region that must remain alive through recovery";
    let mut expected = value.clone();
    for _ in 0..8 {
        expected = json!([backing, expected]);
    }
    let baseline = FailAfter::new(usize::MAX);
    {
        let mut builder = Builder::new_in(baseline.clone());
        populate(&mut builder, &value, backing.as_bytes()).unwrap();
        assert_eq!(decode(builder.finish().unwrap(), 0), expected);
    }
    baseline.assert_released();
    let count = baseline.attempts();
    assert!(count > 4, "exercise all four buffers and their growth");
    for fail_after in 0..=count {
        let allocator = FailAfter::new(fail_after);
        {
            let mut builder = Builder::new_in(allocator.clone());
            let result = populate(&mut builder, &value, backing.as_bytes());
            if fail_after < count {
                assert!(
                    matches!(result, Err(Error::Allocation)),
                    "failure point {fail_after}"
                );
                assert!(matches!(builder.finish(), Err(Error::Protocol(_))));
                let attempts = allocator.attempts();
                assert!(matches!(builder.value(&42), Err(Error::Protocol(_))));
                assert_eq!(allocator.attempts(), attempts, "poisoned builder allocated");
            } else {
                result.unwrap();
                assert_eq!(decode(builder.finish().unwrap(), 0), expected);
            }
            allocator.allow_allocations();
            builder.clear();
            populate(&mut builder, &value, backing.as_bytes()).unwrap();
            assert_eq!(decode(builder.finish().unwrap(), 0), expected);
            let attempts = allocator.attempts();
            builder.clear();
            populate(&mut builder, &value, backing.as_bytes()).unwrap();
            assert_eq!(decode(builder.finish().unwrap(), 0), expected);
            assert_eq!(
                allocator.attempts(),
                attempts,
                "capacity reuse after recovery"
            );
        }
        allocator.assert_released();
    }
}

#[test]
fn allocation_failure_limits_and_recovery() {
    let mut fail = Document::new_in(Failing);
    assert!(matches!(fail.encode(&42), Err(Error::Allocation)));
    assert!(matches!(fail.encode("hello"), Err(Error::Allocation)));
    assert!(fail.view().is_none());
    let mut doc = Document::with_limits_in(
        Global,
        Limits {
            max_depth: 2,
            max_nodes: 8,
            max_bytes: 4,
        },
    );
    assert!(matches!(
        doc.encode(&vec![vec![vec![1]]]),
        Err(Error::Limit)
    ));
    assert!(matches!(doc.encode("abcde"), Err(Error::Limit)));
    assert!(matches!(doc.encode(&vec![1; 8]), Err(Error::Limit)));
    assert!(doc.view().is_none());
    assert_eq!(decode(doc.encode(&vec![vec![1]]).unwrap(), 0), json!([[1]]));
    assert!(doc.encode("abcd").is_ok());
    assert!(matches!(
        doc.encode(&BTreeMap::from([(1, 2)])),
        Err(Error::KeyType)
    ));
    assert!(doc.view().is_none());
    assert!(doc.encode(&42).is_ok());
}

#[test]
fn malformed_map_protocol_is_rejected() {
    struct Bad(u8);
    impl Serialize for Bad {
        fn serialize<S: serde::Serializer>(&self, s: S) -> Result<S::Ok, S::Error> {
            let mut map = s.serialize_map(None)?;
            match self.0 {
                0 => {
                    map.serialize_key("a")?;
                }
                1 => {
                    map.serialize_value(&1)?;
                }
                _ => {
                    map.serialize_key("a")?;
                    map.serialize_key("b")?;
                }
            }
            map.end()
        }
    }
    let mut doc = Document::new_in(Global);
    for mode in 0..3 {
        assert!(matches!(doc.encode(&Bad(mode)), Err(Error::Protocol(_))));
        assert!(doc.view().is_none());
    }
}

fn values() -> impl Strategy<Value = Value> {
    let leaf = prop_oneof![
        Just(Value::Null),
        any::<bool>().prop_map(Value::from),
        any::<i64>().prop_map(Value::from),
        any::<u64>().prop_map(Value::from),
        ".{0,40}".prop_map(Value::from)
    ];
    leaf.prop_recursive(6, 128, 8, |inner| {
        prop_oneof![
            proptest::collection::vec(inner.clone(), 0..8).prop_map(Value::Array),
            proptest::collection::btree_map(".{0,10}", inner, 0..8)
                .prop_map(|map| Value::Object(map.into_iter().collect()))
        ]
    })
}
proptest! {
    #[test]
    fn arbitrary_nested_json_shapes(value in values()) { equivalent(&value); }
}

#[test]
fn display_formats_directly_into_selected_allocator() {
    struct DisplayValue;
    impl std::fmt::Display for DisplayValue {
        fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
            write!(f, "value={}🦀", 42)
        }
    }
    impl Serialize for DisplayValue {
        fn serialize<S: serde::Serializer>(&self, s: S) -> Result<S::Ok, S::Error> {
            s.collect_str(self)
        }
    }
    equivalent(&DisplayValue);
    let mut doc = Document::with_limits_in(
        Global,
        Limits {
            max_bytes: 5,
            ..Limits::default()
        },
    );
    assert!(matches!(doc.encode(&DisplayValue), Err(Error::Limit)));
    assert!(doc.view().is_none());
    let mut fail = Document::new_in(Failing);
    assert!(matches!(fail.encode(&DisplayValue), Err(Error::Allocation)));
}

#[test]
fn swallowed_child_errors_do_not_publish_partial_values() {
    struct IgnoreError;
    impl Serialize for IgnoreError {
        fn serialize<S: serde::Serializer>(&self, s: S) -> Result<S::Ok, S::Error> {
            let mut seq = s.serialize_seq(None)?;
            let _ = seq.serialize_element(&vec![1.0, f64::NAN]);
            seq.end()
        }
    }
    let mut document = Document::new_in(Global);
    assert!(document.encode(&IgnoreError).is_err());
    assert!(document.view().is_none());
    assert!(document.encode(&vec![1, 2]).is_ok());
}

#[test]
fn short_strings_stay_inline_and_limits_count_all_text() {
    let mut doc = Document::new_in(Global);
    for text in [
        "",
        "1234567",
        "12345678",
        "123456789",
        "123456789abcdef",
        "123456789abcdef0",
        "123456789abcdef01",
        "🦀🦀🦀🦀",
        "🦀🦀🦀🦀x",
        "a\0b",
    ] {
        let view = doc.encode(text).unwrap();
        assert_eq!(view.string(0).unwrap().as_str(), text);
        assert_eq!(view.nodes()[0].subtree_end(0), 1);
        assert_eq!(view.nodes()[0].is_inline_string(), text.len() <= 16);
        assert_eq!(
            view.bytes().len(),
            if text.len() <= 16 { 0 } else { text.len() }
        );
    }
    let allocations = Arc::new(AtomicUsize::new(0));
    let deallocations = Arc::new(AtomicUsize::new(0));
    let mut inline = Document::new_in(Counting(allocations.clone(), deallocations));
    inline.encode("123456789abcdef0").unwrap();
    assert_eq!(allocations.load(Ordering::Relaxed), 1); // Only node storage.
    let mut limited = Document::with_limits_in(
        Global,
        Limits {
            max_bytes: 16,
            ..Limits::default()
        },
    );
    assert!(limited.encode(&["12345678", "12345678"]).is_ok());
    assert!(matches!(
        limited.encode(&["12345678", "123456789"]),
        Err(Error::Limit)
    ));
}

#[test]
fn formatted_strings_spill_once_and_preserve_utf8() {
    struct Chunks<'a>(&'a [&'a str]);
    impl std::fmt::Display for Chunks<'_> {
        fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
            for chunk in self.0 {
                f.write_str(chunk)?;
            }
            Ok(())
        }
    }
    impl Serialize for Chunks<'_> {
        fn serialize<S: serde::Serializer>(&self, serializer: S) -> Result<S::Ok, S::Error> {
            serializer.collect_str(self)
        }
    }
    let mut doc = Document::new_in(Global);
    for parts in [
        &["", "a", "🦀"][..],
        &["12345678", "12345678", "", "🦀"],
        &["a long initial chunk of text", "", "more"],
    ] {
        let view = doc.encode(&Chunks(parts)).unwrap();
        let expected = parts.concat();
        assert_eq!(view.string(0).unwrap().as_str(), expected);
        assert_eq!(
            view.bytes().len(),
            if expected.len() <= 16 {
                0
            } else {
                expected.len()
            }
        );
    }
}

#[test]
fn unicode_validation_and_transcoding() {
    use myna_values::text::{Utf8, myna_utf8_to_utf16_v2, to_utf16};
    let long = "Myna\0🦀 é 東京".repeat(128);
    for value in ["", "ASCII\0", "é", "🦀", long.as_str()] {
        assert_eq!(Utf8::from_bytes(value.as_bytes()).unwrap().as_str(), value);
        equivalent(&Utf8::from_bytes(value.as_bytes()).unwrap());
        let expected: Vec<u16> = value.encode_utf16().collect();
        let mut output = vec![0xbeef; expected.len() + 2];
        assert_eq!(
            to_utf16(value, &mut output[..expected.len()]).unwrap(),
            expected.len()
        );
        assert_eq!(&output[..expected.len()], &expected);
        assert_eq!(output[expected.len()], 0xbeef);
        if !expected.is_empty() {
            let mut small = vec![0xbeef; expected.len() - 1];
            assert!(matches!(
                to_utf16(value, &mut small),
                Err(Error::OutputTooSmall)
            ));
            assert!(small.iter().all(|unit| *unit == 0xbeef));
        }
    }
    for invalid in [
        vec![0xc0, 0x80],
        vec![0xed, 0xa0, 0x80],
        vec![0xf4, 0x90, 0x80, 0x80],
        vec![0xff; 300],
        vec![0xf0, 0x9f, 0xa6],
    ] {
        assert!(Utf8::from_bytes(&invalid).is_err());
        let mut output = vec![0; invalid.len()];
        // SAFETY: valid, disjoint input/output buffers with stated extents.
        assert_eq!(
            unsafe {
                myna_utf8_to_utf16_v2(
                    invalid.as_ptr(),
                    invalid.len() as u64,
                    output.as_mut_ptr(),
                    output.len() as u64,
                )
            },
            -1
        );
    }
    let mut output = [0; 1];
    // SAFETY: short capacities are rejected before either pointer is accessed.
    assert_eq!(
        unsafe { myna_utf8_to_utf16_v2(b"ab".as_ptr(), 2, output.as_mut_ptr(), 1) },
        -2
    );
}

proptest! {
    #[test]
    fn simd_validation_matches_rust(bytes in proptest::collection::vec(any::<u8>(), 0..4096)) {
        prop_assert_eq!(myna_values::text::Utf8::from_bytes(&bytes).is_ok(), std::str::from_utf8(&bytes).is_ok());
    }
    #[test]
    fn simd_transcoding_matches_rust(text in ".{0,512}") {
        let expected: Vec<u16> = text.encode_utf16().collect();
        let mut output = vec![0; expected.len()];
        let count = myna_values::text::to_utf16(&text, &mut output).unwrap();
        prop_assert_eq!(count, expected.len());
        prop_assert_eq!(output, expected);
    }
}

#[test]
fn direct_builder_regions_exact_numbers_and_serde() {
    let arena = Bump::new();
    let input = b"xxborrowed1e-999999999999";
    let mut builder = Builder::new_in(&arena);
    let region = builder.region(input).unwrap();
    builder.begin_object().unwrap();
    builder.value("name").unwrap();
    builder.string_ref(region, 2, 8).unwrap();
    builder.value("numbers").unwrap();
    builder.begin_array().unwrap();
    builder
        .number_ref(region, 10, (input.len() - 10) as u32)
        .unwrap();
    builder.exact_number("-0").unwrap();
    builder
        .exact_number("1.234567890123456789012345678901")
        .unwrap();
    builder.value(&vec![1, 2, 3]).unwrap();
    builder.end().unwrap();
    builder.end().unwrap();
    let view = builder.finish().unwrap();
    assert_eq!(view.string(2).unwrap().as_str(), "borrowed");
    assert_eq!(
        view.string(2).unwrap().as_str().as_ptr(),
        input[2..].as_ptr()
    );
    assert_eq!(view.nodes()[2].tag, STRING | REGION_TEXT);
    assert_eq!(view.number_text(5).unwrap().as_str(), "1e-999999999999");
    assert_eq!(view.number_text(6).unwrap().as_str(), "-0");
    assert!(view.nodes()[6].is_inline_text());
    assert_eq!(view.nodes()[6].subtree_end(6), 7);
    assert_eq!(
        view.number_text(7).unwrap().as_str(),
        "1.234567890123456789012345678901"
    );
    assert_eq!(view.nodes()[0].len, 2);
    assert_eq!(std::mem::size_of::<Region>(), 16);
    assert_eq!(std::mem::offset_of!(RawView, regions), 40);
    builder.clear();
    assert!(builder.string_ref(region, 0, 1).is_err());
    assert!(builder.finish().is_err());
}

#[test]
fn direct_builder_rejects_invalid_and_incomplete_values() {
    let mut b = Builder::new_in(Global);
    for number in ["", "+1", "01", "NaN", "1.", ".2", "1e", "--0", "1 2"] {
        b.clear();
        assert!(matches!(b.exact_number(number), Err(Error::InvalidNumber)));
        assert!(b.value(&0).is_err()); // Poisoning must prevent partial publication.
        assert!(b.finish().is_err());
    }
    for number in ["0", "-0", "1e+999999999999", "-1.25E-04"] {
        b.clear();
        b.exact_number(number).unwrap();
        assert_eq!(b.finish().unwrap().number_text(0).unwrap().as_str(), number);
    }
    b.clear();
    b.begin_object().unwrap();
    assert!(matches!(b.value(&1), Err(Error::KeyType)));
    b.clear();
    b.begin_object().unwrap();
    b.value("key").unwrap();
    assert!(b.end().is_err());
    assert!(b.finish().is_err());
    b.clear();
    assert!(b.end().is_err());
    b.clear();
    b.value(&1).unwrap();
    assert!(b.value(&2).is_err());
    b.clear();
    let region = b.region(&[0xff]).unwrap();
    assert!(matches!(
        b.string_ref(region, 0, 1),
        Err(Error::InvalidUtf8)
    ));
    b.clear();
    let region = b.region(b"01").unwrap();
    assert!(matches!(
        b.number_ref(region, 0, 2),
        Err(Error::InvalidNumber)
    ));
    b.clear();
    let region = b.region(b"x").unwrap();
    assert!(matches!(
        b.string_ref(region, 1, 1),
        Err(Error::RegionBounds)
    ));
    let mut limited = Builder::with_limits_in(
        Global,
        Limits {
            max_depth: 1,
            max_nodes: 8,
            max_bytes: 2,
        },
    );
    limited.begin_array().unwrap();
    assert!(matches!(limited.begin_array(), Err(Error::Limit)));
    limited.clear();
    let r = limited.region(b"abc").unwrap();
    assert!(matches!(limited.string_ref(r, 0, 3), Err(Error::Limit)));
}

#[test]
fn serde_forms_and_raw_view_contract() {
    #[derive(Serialize)]
    struct Unit;
    #[derive(Serialize)]
    struct Newtype(i32);
    #[derive(Serialize)]
    struct Tuple(u8, bool);
    equivalent(&Unit);
    equivalent(&Newtype(-7));
    equivalent(&Tuple(255, true));
    equivalent(&1.25_f32);
    equivalent(&-42_i128);
    struct Bytes;
    impl Serialize for Bytes {
        fn serialize<S: serde::Serializer>(&self, s: S) -> Result<S::Ok, S::Error> {
            assert!(s.is_human_readable());
            s.serialize_bytes(&[0, 128, 255])
        }
    }
    equivalent(&Bytes);
    let mut doc = Document::new_in(Global);
    let view = doc
        .encode(&vec!["short", "a longer string in owned storage"])
        .unwrap();
    assert!(view.string(0).is_none());
    assert!(view.string(99).is_none());
    assert!(view.number_text(1).is_none());
    assert!(view.number_text(99).is_none());
    let raw = view.raw();
    // SAFETY: descriptor and storage remain live under the view borrow.
    let raw = unsafe { &*raw.as_ptr() };
    assert_eq!(raw.version, ABI_VERSION);
    assert_eq!(raw.node_count, 3);
    assert_eq!(raw.nodes, view.nodes().as_ptr());
    assert_eq!(raw.bytes, view.bytes().as_ptr());
    assert_eq!(raw.byte_count, 32);
    assert_eq!(raw.region_count, 0);
    struct Custom;
    impl Serialize for Custom {
        fn serialize<S: serde::Serializer>(&self, _: S) -> Result<S::Ok, S::Error> {
            Err(serde::ser::Error::custom("intentional failure"))
        }
    }
    assert!(matches!(doc.encode(&Custom), Err(Error::Custom(s)) if s == "intentional failure"));
    assert!(doc.view().is_none());
    assert_eq!(decode(doc.encode(&42).unwrap(), 0), json!(42));
}

#[test]
fn native_unicode_bridge_checks_extents_and_unicode() {
    use myna_values::text::myna_utf8_to_utf16_v2 as convert;
    let mut output = [0_u16; 256];
    // SAFETY: valid buffers for successful conversions; invalid extents below
    // must be rejected before any pointer is dereferenced.
    unsafe {
        assert_eq!(convert(std::ptr::null(), 0, std::ptr::null_mut(), 0), 0);
        for value in ["hello", "🦀\0é", &"🦀é".repeat(30)] {
            let written = convert(value.as_ptr(), value.len() as u64, output.as_mut_ptr(), 256);
            assert_eq!(
                &output[..written as usize],
                value.encode_utf16().collect::<Vec<_>>()
            );
        }
        assert_eq!(convert([255_u8].as_ptr(), 1, output.as_mut_ptr(), 256), -1);
        assert_eq!(convert(std::ptr::null(), 1, output.as_mut_ptr(), 256), -2);
        assert_eq!(convert(b"a".as_ptr(), 1, std::ptr::null_mut(), 256), -2);
        let unaligned = output.as_mut_ptr().cast::<u8>().add(1).cast::<u16>();
        assert_eq!(convert(b"a".as_ptr(), 1, unaligned, 256), -2);
        assert_eq!(
            convert(b"a".as_ptr(), u64::MAX, output.as_mut_ptr(), 256),
            -2
        );
        assert_eq!(convert(b"a".as_ptr(), 1, output.as_mut_ptr(), u64::MAX), -2);
        assert_eq!(convert(b"ab".as_ptr(), 2, output.as_mut_ptr(), 1), -2);
    }
}

#[test]
fn abi4_directories_metadata_references_and_recovery() {
    assert_eq!(std::mem::size_of::<RawViewV4>(), 88);
    assert_eq!(std::mem::size_of::<DirectoryEntry>(), 8);
    assert_eq!(hash_utf8(b"hello"), 0x4f9f2cab);
    let arena = Bump::new();
    let mut b = Builder::new_in(&arena);
    b.enable_v4(2, true).unwrap();
    b.begin_array().unwrap();
    b.begin_object().unwrap();
    b.value("z").unwrap();
    b.value(&1).unwrap();
    b.value("a").unwrap();
    b.value(&2).unwrap();
    b.value("z").unwrap();
    b.value(&3).unwrap();
    b.end().unwrap();
    b.metadata(1, 42).unwrap();
    b.reference_node(1).unwrap();
    b.reference_node(8).unwrap();
    b.end().unwrap();
    let v = b.finish().unwrap();
    assert_eq!(v.version(), 4);
    assert_eq!(v.metadata().len(), 10);
    assert_eq!(v.metadata()[1], 42);
    assert_eq!(v.reference_target(8), Some(1));
    assert_eq!(v.reference_target(9), Some(8));
    assert_eq!(v.reference_target(0), None);
    assert_eq!(v.reference_target(99), None);
    assert_eq!(v.nodes()[1].payload, 1);
    assert_eq!(v.directory().len(), 3);
    assert!(v.directory().windows(2).all(|pair| pair[0] < pair[1]));
    let raw = v.raw();
    // SAFETY: raw is borrowed from the live builder.
    let descriptor = unsafe { &*raw.as_v4_ptr().unwrap() };
    assert_eq!(descriptor.base.version, 4);
    assert_eq!(descriptor.metadata_count, 10);
    b.clear();
    b.begin_array().unwrap();
    assert!(b.reference_node(0).is_err()); // Open ancestor would form a cycle.
    assert!(b.end().is_err());
    b.clear();
    assert!(b.reference_node(100).is_err());
    b.clear();
    b.value(&0).unwrap();
    assert!(b.enable_v4(8, false).is_err());
    b.clear();
    b.enable_v4(0, false).unwrap();
    b.begin_object().unwrap();
    b.value("key").unwrap();
    b.value(&1).unwrap();
    b.end().unwrap();
    let v = b.finish().unwrap();
    assert!(v.directory().is_empty() && v.metadata().is_empty());
    assert_eq!(v.nodes()[0].payload, 0);
    assert!(b.metadata(0, 1).is_err());
    let mut legacy = Builder::new_in(&arena);
    legacy.value(&7).unwrap();
    assert!(legacy.finish().unwrap().raw().as_v4_ptr().is_none());
    assert!(legacy.reference_node(0).is_err());
}

#[test]
fn batch_text_mixed_storage_sizing_and_transactional_errors() {
    use myna_values::text::{TextEntry, myna_tape_text_v1};
    let arena = Bump::new();
    let mut b = Builder::new_in(&arena);
    b.enable_v4(0, false).unwrap();
    let borrowed = "borrowed é 🦀";
    let r = b.region(borrowed.as_bytes()).unwrap();
    b.begin_array().unwrap();
    b.value("ASCII").unwrap();
    b.value("café").unwrap();
    b.value("🦀").unwrap();
    b.value(&42).unwrap();
    b.value("").unwrap();
    b.value("long owned text with Ελληνικά").unwrap();
    b.string_ref(r, 0, borrowed.len() as u32).unwrap();
    b.exact_number("123.4500e+7").unwrap();
    b.reference_node(1).unwrap();
    b.end().unwrap();
    let v = b.finish().unwrap();
    let raw = v.raw();
    // SAFETY: live borrowed descriptor, disjoint owned output buffers.
    unsafe {
        let size = myna_tape_text_v1(
            raw.as_ptr(),
            std::ptr::null_mut(),
            0,
            std::ptr::null_mut(),
            0,
        );
        assert!(size > 0);
        let mut out = vec![0xcc; size as usize];
        let mut table = vec![TextEntry::default(); v.nodes().len()];
        assert_eq!(
            myna_tape_text_v1(
                raw.as_ptr(),
                out.as_mut_ptr(),
                size as u64 - 1,
                table.as_mut_ptr(),
                table.len() as u64
            ),
            -3
        );
        assert!(out.iter().all(|b| *b == 0xcc));
        assert_eq!(
            myna_tape_text_v1(
                raw.as_ptr(),
                out.as_mut_ptr(),
                out.len() as u64,
                table.as_mut_ptr(),
                table.len() as u64
            ),
            size
        );
        for (i, entry) in table.iter().enumerate() {
            let text = v.string(i).or_else(|| v.number_text(i));
            if let Some(text) = text {
                let start = entry.offset as usize;
                let decoded = if entry.encoding == 1 {
                    out[start..start + entry.length as usize]
                        .iter()
                        .map(|b| char::from(*b))
                        .collect::<String>()
                } else {
                    let units = out[start..start + entry.length as usize * 2]
                        .as_chunks::<2>()
                        .0
                        .iter()
                        .map(|b| u16::from_le_bytes([b[0], b[1]]))
                        .collect::<Vec<_>>();
                    String::from_utf16(&units).unwrap()
                };
                assert_eq!(decoded, text.as_str());
            } else {
                assert_eq!(entry.encoding, 0);
            }
        }
        let mut descriptor = *raw.as_ptr();
        descriptor.version = 2;
        assert_eq!(
            myna_tape_text_v1(
                &descriptor,
                std::ptr::null_mut(),
                0,
                std::ptr::null_mut(),
                0
            ),
            -2
        );
        descriptor.version = 3;
        let malformed = [Node {
            tag: STRING,
            len: 1,
            payload: 255,
            next: 0,
        }];
        descriptor.nodes = malformed.as_ptr();
        descriptor.node_count = 1;
        assert_eq!(
            myna_tape_text_v1(
                &descriptor,
                out.as_mut_ptr(),
                out.len() as u64,
                table.as_mut_ptr(),
                table.len() as u64
            ),
            -1
        );
        assert_eq!(
            myna_tape_text_v1(
                std::ptr::null(),
                std::ptr::null_mut(),
                0,
                std::ptr::null_mut(),
                0
            ),
            -2
        );
    }
}
