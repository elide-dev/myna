// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
//! Arbitrary bytes through the generated persisted-format readers of `tests/formats/doc.seam`
//! (myna#167), seeded with `tests/formats/golden/*.bin`. A reader never panics; an accepted file
//! also passes the structural stage, and a naive recursive walk from its root (no memoising of
//! shared nodes) stays within the format's `MAX_DEPTH` frames and `MAX_WORK` visits.
#![no_main]
#[path = "../../../../tests/golden/formats/formats.rs"]
mod formats;

use formats::doc::{self, AnyNode, Block};

struct Walk {
    visits: u64,
    deepest: u32,
}

impl Walk {
    fn node(&mut self, node: AnyNode<'_>, depth: u32) {
        self.visits += 1;
        self.deepest = self.deepest.max(depth);
        match node {
            AnyNode::Style(s) => {
                let _ = s.face();
                if let Some(p) = s.parent() {
                    self.node(AnyNode::Style(p), depth + 1);
                }
            }
            AnyNode::Paragraph(p) => {
                self.node(AnyNode::Style(p.style()), depth + 1);
                for span in p.spans() {
                    match span {
                        doc::Span::SpanText(t) => {
                            let _ = t.text();
                        }
                        doc::Span::SpanBreak(_) => {}
                        doc::Span::SpanStyled(x) => {
                            let _ = (x.text(), x.scale());
                            self.node(AnyNode::Style(x.style()), depth + 1);
                        }
                    }
                }
                for a in p.attrs() {
                    let _ = (a.key(), a.important());
                    if let Some(v) = a.value() {
                        self.node(AnyNode::Style(v), depth + 1);
                    }
                }
            }
            AnyNode::RawText(r) => {
                let _ = r.units().count();
            }
            AnyNode::Section(s) => {
                let _ = s.title();
                for b in s.blocks() {
                    self.node(block(b), depth + 1);
                }
            }
            AnyNode::Document(d) => {
                let _ = d.source();
                for s in d.sections() {
                    self.node(AnyNode::Section(s), depth + 1);
                }
                if let Some(m) = d.meta() {
                    self.node(AnyNode::Meta(m), depth + 1);
                }
            }
            AnyNode::Meta(m) => {
                let _ = (m.samples().count(), m.blob_raw(), m.f(), m.g());
                if let Some(x) = m.marker() {
                    self.node(x, depth + 1);
                }
            }
            AnyNode::Marker(_) => {}
        }
    }
}

fn block(b: Block<'_>) -> AnyNode<'_> {
    match b {
        Block::Paragraph(p) => AnyNode::Paragraph(p),
        Block::RawText(r) => AnyNode::RawText(r),
        Block::Section(s) => AnyNode::Section(s),
    }
}

fn walk(root: AnyNode<'_>, max_depth: u32, max_work: u32) {
    let mut w = Walk { visits: 0, deepest: 0 };
    w.node(root, 1);
    assert!(w.deepest <= max_depth, "depth {} over {max_depth}", w.deepest);
    assert!(w.visits <= max_work as u64, "work {} over {max_work}", w.visits);
}

libfuzzer_sys::fuzz_target!(|data: &[u8]| {
    if let Ok(f) = doc::DocFile::validate(data, &[7]) {
        assert!(doc::DocFile::validate_structural(data, &[7]).is_ok());
        walk(AnyNode::Document(f.root()), doc::DocFile::MAX_DEPTH, doc::DocFile::MAX_WORK);
    }
    if let Ok(f) = doc::DocFragment::validate(data) {
        assert!(doc::DocFragment::validate_structural(data).is_ok());
        walk(f.root(), doc::DocFragment::MAX_DEPTH, doc::DocFragment::MAX_WORK);
    }
    if let Ok(f) = doc::DocTiny::validate(data, &[1]) {
        assert!(doc::DocTiny::validate_structural(data, &[1]).is_ok());
        walk(f.root(), doc::DocTiny::MAX_DEPTH, doc::DocTiny::MAX_WORK);
    }
    let _ = (doc::DocFile::peek(data), doc::DocTiny::peek(data));
});
