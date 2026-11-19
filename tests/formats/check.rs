// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
//! Drives the generated Rust persisted-format code for `tests/formats/doc.seam`.
//!
//! - `golden DIR`: builds every golden value in two creation orders, checks the bytes match and
//!   round-trip through the reader and writer, and writes `DIR/<name>.bin`.
//! - `verdict FILE...`: prints both validation verdicts and, when valid, a dump of every node.
//! - `writer-errors`: prints the writer's error for each unencodable value.
//!
//! `tests/formats/FormatsMain.java` prints the same output from the generated Java.
mod formats;

use formats::doc::write::{Attr, DocWriter, Span as WSpan};
use formats::doc::{self, AnyNode, ErrorKind, FormatError};
use std::fmt::Write as _;

fn hex(bytes: &[u8]) -> String {
    let mut out = String::from("x");
    for b in bytes {
        write!(out, "{b:02x}").unwrap();
    }
    out
}

fn opt(index: Option<u32>) -> String {
    index.map_or(String::from("-"), |i| i.to_string())
}

fn dump(node: AnyNode<'_>) -> String {
    match node {
        AnyNode::Style(s) => format!(
            "Style face={} flags={} align={} size={:x} parent={}",
            hex(s.face().as_bytes()),
            s.flags(),
            s.align(),
            s.size_bits(),
            opt(s.parent().map(|p| p.index()))
        ),
        AnyNode::Paragraph(p) => {
            let spans: Vec<String> = p
                .spans()
                .map(|span| match span {
                    doc::Span::SpanText(t) => format!("T:{}", hex(t.text().as_bytes())),
                    doc::Span::SpanBreak(_) => String::from("B"),
                    doc::Span::SpanStyled(x) => {
                        format!("S:{}:{}:{:x}", x.style().index(), hex(x.text().as_bytes()), x.scale().to_bits())
                    }
                })
                .collect();
            let attrs: Vec<String> = p
                .attrs()
                .map(|a| format!("{}:{}:{}", hex(a.key().as_bytes()), opt(a.value().map(|v| v.index())), a.important()))
                .collect();
            assert_eq!(spans.len(), p.spans_count());
            assert_eq!(attrs.len(), p.attrs_count());
            format!("Paragraph style={} spans=[{}] attrs=[{}]", p.style().index(), spans.join(","), attrs.join(","))
        }
        AnyNode::RawText(r) => {
            let units: Vec<String> = r.units().map(|u| format!("{u:x}")).collect();
            assert_eq!(r.units_raw().len(), 2 * r.units_count());
            if r.units_count() > 0 {
                assert_eq!(r.units_at(0), r.units().next().unwrap());
            }
            format!("RawText units=[{}]", units.join(","))
        }
        AnyNode::Section(s) => {
            let blocks: Vec<String> = s.blocks().map(|b| b.index().to_string()).collect();
            for i in 0..s.blocks_count() {
                assert_eq!(s.blocks_at(i).index().to_string(), blocks[i]);
            }
            format!("Section title={} id={} blocks=[{}]", hex(s.title().as_bytes()), s.title_id(), blocks.join(","))
        }
        AnyNode::Document(d) => {
            let sections: Vec<String> = d.sections().map(|s| s.index().to_string()).collect();
            format!(
                "Document sections=[{}] source={} meta={}",
                sections.join(","),
                hex(d.source()),
                opt(d.meta().map(|m| m.index()))
            )
        }
        AnyNode::Meta(m) => {
            let samples: Vec<String> = m.samples().map(|x| x.to_string()).collect();
            format!(
                "Meta a={} b={} c={} d={} e={} f={:x} g={} samples=[{}] blob={} marker={}",
                m.a(),
                m.b(),
                m.c(),
                m.d(),
                m.e(),
                m.f().to_bits(),
                m.g(),
                samples.join(","),
                hex(m.blob_raw()),
                opt(m.marker().map(|x| x.index()))
            )
        }
        AnyNode::Marker(_) => String::from("Marker"),
    }
}

// ------------------------------------------------------------------- golden values

fn full(order_b: bool) -> Vec<u8> {
    let mut w = DocWriter::new();
    if order_b {
        // Strings and nodes the root never reaches are dropped by finish.
        w.string("unused");
        for s in ["title-2", "title-1", "ac", "ab", "k", "naïve", "café", "", "Body", "Serif"] {
            w.string(s);
        }
        let unused = w.string("dropped");
        w.style(unused, 0, 0, 9, None).unwrap();
    }
    let serif = w.string("Serif");
    let body = w.string("Body");
    let marker = w.marker().unwrap();
    let (s_a, s_b) = if order_b {
        let s_b = w.style(serif, 0, 0, 2, None).unwrap();
        (w.style(serif, 0, 0, 1, None).unwrap(), s_b)
    } else {
        let s_a = w.style(serif, 0, 0, 1, None).unwrap();
        (s_a, w.style(serif, 0, 0, 2, None).unwrap())
    };
    let s_base = w.style(serif, 1 | (5 << 4), 1, 0x4028_0000_0000_0000, None).unwrap();
    let s_child = w.style(body, 2, 2, 3, Some(s_base)).unwrap();
    let r1 = w.raw_text(&[0x48, 0xD800, 0x69]).unwrap();
    let cafe = w.string("café");
    let naive = w.string("naïve");
    let k = w.string("k");
    let empty = w.string("");
    let p1 = w
        .paragraph(
            s_child,
            [WSpan::SpanText { text: cafe }, WSpan::SpanBreak, WSpan::SpanStyled { style: s_a, text: naive, scale: 1.5 }],
            [Attr { key: k, value: Some(s_b), important: true }, Attr { key: empty, value: None, important: false }],
        )
        .unwrap();
    let base_again = if order_b { w.style(serif, 1 | (5 << 4), 1, 0x4028_0000_0000_0000, None).unwrap() } else { s_base };
    let ab = w.string("ab");
    let ac = w.string("ac");
    let p2 = w.paragraph(base_again, [WSpan::SpanText { text: ab }], []).unwrap();
    let p3 = w.paragraph(s_base, [WSpan::SpanText { text: ac }], []).unwrap();
    let t1 = w.string("title-1");
    let t2 = w.string("title-2");
    let sec1 = w.section(t1, [p1.into(), r1.into()]).unwrap();
    let sec2 = w.section(t2, [p2.into(), p3.into()]).unwrap();
    let meta = w
        .meta(-5, 65535, -2, i32::MIN, i64::MAX, f64::from_bits(0x7ff8_0000_0000_0001), true, &[-1, 0, 1], &[0xde, 0xad], Some(marker.into()))
        .unwrap();
    let doc = w.document([sec1, sec2], b"source\x00\xff", Some(meta)).unwrap();
    w.finish_doc_file(doc, 7).unwrap()
}

fn fragment_paragraph(order_b: bool) -> Vec<u8> {
    let mut w = DocWriter::new();
    let face = w.string("Serif");
    let style = w.style(face, 0, 0, 0, None).unwrap();
    let style = if order_b { w.style(face, 0, 0, 0, None).unwrap() } else { style };
    let p = w.paragraph(style, [WSpan::SpanText { text: face }, WSpan::SpanBreak], []).unwrap();
    w.finish_doc_fragment(p).unwrap()
}

fn fragment_raw_text(_order_b: bool) -> Vec<u8> {
    let mut w = DocWriter::new();
    let r = w.raw_text(&[]).unwrap();
    w.finish_doc_fragment(r).unwrap()
}

fn tiny(order_b: bool) -> Vec<u8> {
    // At every DocTiny limit: 4 nodes, depth 3, work 6, 3 strings.
    let mut w = DocWriter::new();
    let (f, k, t) = if order_b {
        let t = w.string("t");
        let k = w.string("k");
        (w.string("f"), k, t)
    } else {
        (w.string("f"), w.string("k"), w.string("t"))
    };
    let (s1, s2) = if order_b {
        let s2 = w.style(f, 0, 0, 2, None).unwrap();
        (w.style(f, 0, 0, 1, None).unwrap(), s2)
    } else {
        let s1 = w.style(f, 0, 0, 1, None).unwrap();
        (s1, w.style(f, 0, 0, 2, None).unwrap())
    };
    let attr = |value| Attr { key: k, value, important: false };
    let p = w.paragraph(s2, [], [attr(Some(s1)), attr(Some(s1)), attr(Some(s1)), attr(None)]).unwrap();
    let section = w.section(t, [p.into()]).unwrap();
    w.finish_doc_tiny(section, 1).unwrap()
}

// ------------------------------------------------------------------- round trip

enum Id {
    Style(doc::write::StyleId),
    Paragraph(Option<doc::write::ParagraphId>),
    RawText(Option<doc::write::RawTextId>),
    Section(Option<doc::write::SectionId>),
    Document(Option<doc::write::DocumentId>),
    Meta(doc::write::MetaId),
    Marker(doc::write::MarkerId),
}

fn style_id(ids: &[Id], i: u32) -> doc::write::StyleId {
    match &ids[i as usize] {
        Id::Style(s) => *s,
        _ => panic!("not a style"),
    }
}

fn any_id(ids: &mut [Id], i: u32) -> doc::write::AnyId {
    match &mut ids[i as usize] {
        Id::Style(s) => (*s).into(),
        Id::Paragraph(p) => p.take().unwrap().into(),
        Id::RawText(r) => r.take().unwrap().into(),
        Id::Section(s) => s.take().unwrap().into(),
        Id::Document(d) => d.take().unwrap().into(),
        Id::Meta(m) => (*m).into(),
        Id::Marker(m) => (*m).into(),
    }
}

fn block_id(ids: &mut [Id], i: u32) -> doc::write::BlockId {
    match &mut ids[i as usize] {
        Id::Paragraph(p) => p.take().unwrap().into(),
        Id::RawText(r) => r.take().unwrap().into(),
        Id::Section(s) => s.take().unwrap().into(),
        _ => panic!("not a block"),
    }
}

/// Rebuilds every node of a file through a fresh writer, in index order.
fn copy(count: u32, node: impl Fn(u32) -> AnyNode<'static>) -> (DocWriter, Vec<Id>) {
    let mut w = DocWriter::new();
    let mut ids: Vec<Id> = Vec::new();
    for i in 0..count {
        let id = match node(i) {
            AnyNode::Style(s) => {
                let face = w.string(s.face());
                let parent = s.parent().map(|p| style_id(&ids, p.index()));
                Id::Style(w.style(face, s.flags(), s.align(), s.size_bits(), parent).unwrap())
            }
            AnyNode::Paragraph(p) => {
                let mut spans = Vec::new();
                for span in p.spans() {
                    spans.push(match span {
                        doc::Span::SpanText(t) => WSpan::SpanText { text: w.string(t.text()) },
                        doc::Span::SpanBreak(_) => WSpan::SpanBreak,
                        doc::Span::SpanStyled(x) => {
                            WSpan::SpanStyled { style: style_id(&ids, x.style().index()), text: w.string(x.text()), scale: x.scale() }
                        }
                    });
                }
                let mut attrs = Vec::new();
                for a in p.attrs() {
                    attrs.push(Attr { key: w.string(a.key()), value: a.value().map(|v| style_id(&ids, v.index())), important: a.important() });
                }
                Id::Paragraph(Some(w.paragraph(style_id(&ids, p.style().index()), spans, attrs).unwrap()))
            }
            AnyNode::RawText(r) => {
                let units: Vec<u16> = r.units().collect();
                Id::RawText(Some(w.raw_text(&units).unwrap()))
            }
            AnyNode::Section(s) => {
                let title = w.string(s.title());
                let blocks: Vec<_> = s.blocks().map(|b| block_id(&mut ids, b.index())).collect();
                Id::Section(Some(w.section(title, blocks).unwrap()))
            }
            AnyNode::Document(d) => {
                let sections: Vec<_> = d
                    .sections()
                    .map(|s| match &mut ids[s.index() as usize] {
                        Id::Section(x) => x.take().unwrap(),
                        _ => panic!("not a section"),
                    })
                    .collect();
                let meta = d.meta().map(|m| match &ids[m.index() as usize] {
                    Id::Meta(x) => *x,
                    _ => panic!("not a meta"),
                });
                Id::Document(Some(w.document(sections, d.source(), meta).unwrap()))
            }
            AnyNode::Meta(m) => {
                let samples: Vec<i64> = m.samples().collect();
                let marker = m.marker().map(|x| any_id(&mut ids, x.index()));
                Id::Meta(w.meta(m.a(), m.b(), m.c(), m.d(), m.e(), m.f(), m.g(), &samples, m.blob_raw(), marker).unwrap())
            }
            AnyNode::Marker(_) => Id::Marker(w.marker().unwrap()),
        };
        ids.push(id);
    }
    (w, ids)
}

fn round_trip(name: &str, bytes: &'static [u8]) {
    let rebuilt = if name.starts_with("docfile") {
        let file = doc::DocFile::validate(bytes, &[7]).unwrap();
        let (w, mut ids) = copy(file.node_count(), |i| file.node(i));
        let root = match ids.pop().unwrap() {
            Id::Document(d) => d.unwrap(),
            _ => panic!("root is a document"),
        };
        w.finish_doc_file(root, file.capability()).unwrap()
    } else if name.starts_with("docfragment") {
        let file = doc::DocFragment::validate(bytes).unwrap();
        let (w, mut ids) = copy(file.node_count(), |i| file.node(i));
        let root = any_id(&mut ids, file.node_count() - 1);
        w.finish_doc_fragment(root).unwrap()
    } else {
        let file = doc::DocTiny::validate(bytes, &[1]).unwrap();
        let (w, mut ids) = copy(file.node_count(), |i| file.node(i));
        let root = any_id(&mut ids, file.node_count() - 1);
        w.finish_doc_tiny(root, file.mode()).unwrap()
    };
    assert_eq!(rebuilt, bytes, "{name}: decoding and encoding again changed the bytes");
}

fn golden(dir: &str) {
    let values: [(&str, fn(bool) -> Vec<u8>); 4] = [
        ("docfile-full", full),
        ("docfragment-paragraph", fragment_paragraph),
        ("docfragment-rawtext", fragment_raw_text),
        ("doctiny-section", tiny),
    ];
    for (name, build) in values {
        let a = build(false);
        let b = build(true);
        assert_eq!(a, b, "{name}: creation order changed the bytes");
        let leaked: &'static [u8] = Box::leak(a.clone().into_boxed_slice());
        round_trip(name, leaked);
        std::fs::write(format!("{dir}/{name}.bin"), &a).unwrap();
    }
}

// ------------------------------------------------------------------- verdicts

fn error(e: FormatError) -> String {
    format!("ERR {:?} {} {} {:?}", e.kind, e.offset, e.node as i32, e.class())
}

fn verdict(path: &str) {
    let bytes: &'static [u8] = Box::leak(std::fs::read(path).unwrap().into_boxed_slice());
    let name = std::path::Path::new(path).file_name().unwrap().to_str().unwrap();
    let (full, structural, count, node): (Result<(), FormatError>, Result<(), FormatError>, u32, Box<dyn Fn(u32) -> AnyNode<'static>>) =
        if name.starts_with("docfile") {
            let s = doc::DocFile::validate_structural(bytes, &[7]).map(|_| ());
            match doc::DocFile::validate(bytes, &[7]) {
                Ok(f) => (Ok(()), s, f.node_count(), Box::new(move |i| f.node(i))),
                Err(e) => (Err(e), s, 0, Box::new(|_| unreachable!())),
            }
        } else if name.starts_with("docfragment") {
            let s = doc::DocFragment::validate_structural(bytes).map(|_| ());
            match doc::DocFragment::validate(bytes) {
                Ok(f) => (Ok(()), s, f.node_count(), Box::new(move |i| f.node(i))),
                Err(e) => (Err(e), s, 0, Box::new(|_| unreachable!())),
            }
        } else {
            let s = doc::DocTiny::validate_structural(bytes, &[1]).map(|_| ());
            match doc::DocTiny::validate(bytes, &[1]) {
                Ok(f) => (Ok(()), s, f.node_count(), Box::new(move |i| f.node(i))),
                Err(e) => (Err(e), s, 0, Box::new(|_| unreachable!())),
            }
        };
    let show = |r: &Result<(), FormatError>| match r {
        Ok(()) => String::from("OK"),
        Err(e) => error(*e),
    };
    println!("{name} full={} structural={}", show(&full), show(&structural));
    for i in 0..count {
        println!("  #{i} {}", dump(node(i)));
    }
}

// ------------------------------------------------------------------- writer errors

fn kind<T>(r: Result<T, doc::WriteError>) -> String {
    match r {
        Ok(_) => String::from("OK"),
        Err(e) => format!("{:?}", e.kind),
    }
}

fn writer_errors() {
    let mut w = DocWriter::new();
    let f = w.string("f");
    println!("bad-enum {}", kind(w.style(f, 0, 3, 0, None)));
    println!("bad-flags {}", kind(w.style(f, 0x100, 0, 0, None)));
    let s = w.style(f, 0, 0, 0, None).unwrap();
    let attrs: Vec<Attr> = (0..256).map(|_| Attr { key: f, value: None, important: false }).collect();
    println!("field-too-long {}", kind(w.paragraph(s, [], attrs)));

    // DocTiny: max_depth 3, max_work 6, max_nodes 4, max_strings 3, max_bytes 512.
    let mut w = DocWriter::new();
    let f = w.string("f");
    let s1 = w.style(f, 0, 0, 1, None).unwrap();
    let s2 = w.style(f, 0, 0, 2, Some(s1)).unwrap();
    let p = w.paragraph(s2, [], []).unwrap();
    let t = w.string("t");
    let section = w.section(t, [p.into()]).unwrap();
    println!("too-deep {}", kind(w.finish_doc_tiny(section, 1)));

    let mut w = DocWriter::new();
    let f = w.string("f");
    let s = w.style(f, 0, 0, 0, None).unwrap();
    let attrs: Vec<Attr> = (0..5).map(|_| Attr { key: f, value: Some(s), important: false }).collect();
    let p = w.paragraph(s, [], attrs).unwrap();
    println!("too-much-work {}", kind(w.finish_doc_tiny(p, 1)));

    let mut w = DocWriter::new();
    let names: Vec<_> = ["a", "b", "c", "d"].iter().map(|x| w.string(x)).collect();
    let s = w.style(names[0], 0, 0, 0, None).unwrap();
    let spans: Vec<WSpan> = names.iter().map(|&text| WSpan::SpanText { text }).collect();
    let p = w.paragraph(s, spans, []).unwrap();
    println!("too-many-strings {}", kind(w.finish_doc_tiny(p, 1)));

    let mut w = DocWriter::new();
    let f = w.string("f");
    let t = w.string("t");
    let mut blocks = Vec::new();
    for _ in 0..4 {
        blocks.push(w.raw_text(&[]).unwrap().into());
    }
    let _ = f;
    let section = w.section(t, blocks).unwrap();
    println!("too-many-nodes {}", kind(w.finish_doc_tiny(section, 1)));

    let mut w = DocWriter::new();
    let r = w.raw_text(&[0u16; 300]).unwrap();
    println!("too-large {}", kind(w.finish_doc_tiny(r, 1)));

    let mut w = DocWriter::new();
    let t = w.string("t");
    let section = w.section(t, []).unwrap();
    println!("bad-root {}", kind(w.finish_doc_fragment(section)));
}

fn main() {
    let args: Vec<String> = std::env::args().collect();
    match args.get(1).map(String::as_str) {
        Some("golden") => golden(&args[2]),
        Some("verdict") => {
            for path in &args[2..] {
                verdict(path);
            }
        }
        Some("writer-errors") => writer_errors(),
        _ => {
            eprintln!("usage: check golden DIR | verdict FILE... | writer-errors");
            std::process::exit(2);
        }
    }
    let _ = ErrorKind::BadMagic.code();
}
