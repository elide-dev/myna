use allocator_api2::alloc::Global;
use criterion::{BenchmarkId, Criterion, criterion_group, criterion_main};
use myna_values::{Bump, Document};
use serde::Serialize;
use std::hint::black_box;

#[derive(Serialize)]
struct Flat<'a> {
    id: u64,
    active: bool,
    name: &'a str,
    score: f64,
}
#[derive(Serialize)]
struct Nested<'a> {
    items: Vec<Flat<'a>>,
    labels: Vec<&'a str>,
    absent: Option<u64>,
}

fn case<T: Serialize>(c: &mut Criterion, name: &str, value: &T) {
    let mut group = c.benchmark_group(name);
    let arena = Bump::new();
    let mut bump = Document::new(&arena);
    let mut global = Document::new_in(Global);
    bump.encode(value).unwrap();
    global.encode(value).unwrap();
    for (label, use_bump) in [("bumpalo_reused", true), ("global_reused", false)] {
        group.bench_with_input(BenchmarkId::new("tape", label), &use_bump, |b, use_bump| {
            b.iter(|| {
                let view = if *use_bump {
                    bump.encode(black_box(value)).unwrap()
                } else {
                    global.encode(black_box(value)).unwrap()
                };
                black_box(view.nodes());
                black_box(view.bytes());
            });
        });
    }
    group.bench_function("serde_json_value", |b| {
        b.iter(|| black_box(serde_json::to_value(black_box(value)).unwrap()))
    });
    let mut bytes = Vec::new();
    group.bench_function("serde_json_bytes_reused", |b| {
        b.iter(|| {
            bytes.clear();
            serde_json::to_writer(&mut bytes, black_box(value)).unwrap();
            black_box(&bytes);
        })
    });
    group.finish();
}
fn encode(c: &mut Criterion) {
    case(
        c,
        "flat",
        &Flat {
            id: u64::MAX,
            active: true,
            name: "Myna",
            score: 3.5,
        },
    );
    case(
        c,
        "nested",
        &Nested {
            items: (0..32)
                .map(|id| Flat {
                    id,
                    active: id % 2 == 0,
                    name: "Myna 🦀",
                    score: 3.5,
                })
                .collect(),
            labels: vec!["alpha", "beta", "gamma"],
            absent: None,
        },
    );
    case(c, "short_strings_256", &vec!["short 🦀"; 256]);
    case(c, "numbers_4096", &(0..4096_u64).collect::<Vec<_>>());
    case(
        c,
        "strings_256",
        &vec!["A moderately long UTF-8 string 🦀"; 256],
    );
}
fn text(c: &mut Criterion) {
    use myna_values::text::{Utf8, to_utf16};
    for size in [16, 17, 256, 4096] {
        let value = format!("{}🦀", "a".repeat(size - 4));
        let mut group = c.benchmark_group(format!("text_{size}"));
        group.bench_function("validate_selected", |b| {
            b.iter(|| black_box(Utf8::from_bytes(black_box(value.as_bytes())).unwrap()))
        });
        group.bench_function("validate_std", |b| {
            b.iter(|| black_box(std::str::from_utf8(black_box(value.as_bytes())).unwrap()))
        });
        let mut output = vec![0; value.len()];
        group.bench_function("utf16_selected", |b| {
            b.iter(|| {
                let len = to_utf16(black_box(&value), &mut output).unwrap();
                black_box(&output[..len]);
            })
        });
        group.bench_function("utf16_std", |b| {
            b.iter(|| {
                let mut len = 0;
                for unit in black_box(&value).encode_utf16() {
                    output[len] = unit;
                    len += 1;
                }
                black_box(&output[..len]);
            })
        });
        group.finish();
    }
}
criterion_group!(benches, encode, text);
criterion_main!(benches);
