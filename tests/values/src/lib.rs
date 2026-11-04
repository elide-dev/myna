use myna_values::{Bump, Document, RawView};
use serde::Serialize;
use std::ffi::c_void;

#[derive(Serialize)]
struct Data<'a> {
    id: u64,
    name: &'a str,
    active: bool,
    signed: i64,
    ratio: f64,
    absent: Option<u8>,
    nested: Vec<Vec<i32>>,
    text: Vec<String>,
}

/// Synchronous fixture: descriptor and buffers stay live until every callback
/// returns. No Rust panic is permitted to unwind through a foreign frame.
///
/// # Safety
/// The consumer and isolate must be valid and obey the call-scoped ABI.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn myna_value_drive(
    consumer: unsafe extern "C" fn(*mut c_void, *const RawView, i32) -> i64,
    isolate: *mut c_void,
    iterations: u32,
    mode: i32,
) -> i64 {
    std::panic::catch_unwind(|| {
        if (2..=4).contains(&mode) {
            let nodes = [myna_values::Node {
                tag: myna_values::STRING | myna_values::REGION_TEXT,
                len: 300,
                payload: 0,
                next: 1,
            }];
            let bytes = [0xff_u8; 300];
            let raw = RawView {
                version: if mode == 3 {
                    1
                } else {
                    myna_values::ABI_VERSION
                },
                flags: if mode == 4 { 1 } else { 0 },
                nodes: nodes.as_ptr(),
                node_count: 1,
                bytes: bytes.as_ptr(),
                byte_count: 300,
                regions: std::ptr::null(),
                region_count: 0,
            };
            // SAFETY: caller guarantees callback; this deliberately invalid
            // UTF-8 producer still supplies live allocations and valid layout.
            return unsafe { consumer(isolate, &raw, mode) };
        }
        if mode >= 10 {
            let (size, pattern) = match (mode - 10) / 2 {
                0 => (16, "a"),
                1 => (256, "a"),
                2 => (4096, "a"),
                3 => (16, "🦀"),
                4 => (256, "🦀"),
                _ => (4096, "🦀"),
            };
            let value = pattern.repeat(size / pattern.len());
            let arena = Bump::new();
            let mut document = Document::new(&arena);
            let view = document.encode(&value).unwrap();
            let raw = view.raw();
            let mut total = 0;
            for _ in 0..iterations {
                // SAFETY: view/descriptor remain live across synchronous calls.
                let result = unsafe { consumer(isolate, raw.as_ptr(), mode) };
                if result < 0 {
                    return result;
                }
                total += result;
            }
            return total;
        }
        let arena = Bump::new();
        let mut document = Document::new(&arena);
        let data = Data {
            id: u64::MAX,
            name: "Myna\0🦀",
            active: true,
            signed: i64::MIN,
            ratio: -0.0,
            absent: None,
            nested: vec![vec![1, 2], vec![], vec![3]],
            text: [
                "",
                "1234567",
                "12345678",
                "123456789",
                "123456789abcdef",
                "123456789abcdef0",
                "123456789abcdef01",
                "🦀🦀🦀🦀",
            ]
            .into_iter()
            .map(str::to_owned)
            .chain(["Myna🦀\0".repeat(128)])
            .collect(),
        };
        let mut total = 0_i64;
        for _ in 0..iterations {
            let Ok(view) = document.encode(&data) else {
                return -1;
            };
            let raw = view.raw();
            // SAFETY: caller guarantees the callback; both buffers and the
            // descriptor remain live and immutably borrowed until it returns.
            let result = unsafe { consumer(isolate, raw.as_ptr(), mode) };
            if result < 0 {
                return result;
            }
            total = match total.checked_add(result) {
                Some(total) => total,
                None => return -2,
            };
        }
        total
    })
    .unwrap_or(-3)
}
