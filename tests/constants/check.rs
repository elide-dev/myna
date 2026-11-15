// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
// Compiles the generated constants seam under #![deny(warnings)] and checks the newtypes.
#![deny(warnings)]
#[allow(dead_code, non_snake_case)]
mod seam {
    include!(concat!(env!("SEAM_GENERATED"), "/seam.rs"));
}
use seam::{
    BinaryOp, Kind, Level, Node, NodeTag, Packed, Perms, Record, Session, Status, UnaryOp, Wide,
    WriteFlags,
};

const _: () = assert!(seam::MAGIC == 0x3141514b);
const _: () = assert!(seam::LIMIT == u64::MAX);
const _: () = assert!(seam::FLOOR == i64::MIN);
const _: () = assert!(Status::NULL_RESULT.0 == -1);
const _: () = assert!(Status::OK.0 == 0);
const _: () = assert!(Status::SYNTAX_ERROR.0 == 16);
const _: () = assert!(Status::INTERNAL_ERROR.0 == 3);
const _: () = assert!(Level::LOW.0 == 0);
const _: () = assert!(Level::HIGH.0 == 255);
const _: () = assert!(Perms::NONE.0 == 0);
const _: () = assert!(Perms::READ.0 == 1);
const _: () = assert!(Perms::WRITE.0 == 2);
const _: () = assert!(Perms::STICKY.0 == 0x8000_0000);
const _: () = assert!(Perms::ALL.0 == 0x8000_0003);
const _: () = assert!(Wide::FIRST.0 == 1);
const _: () = assert!(Wide::LAST.0 == 1 << 63);
const _: () = assert!(Wide::ALL.0 == 0x8000_0000_0000_0001);
const _: () = assert!(core::mem::size_of::<Status>() == 4);
const _: () = assert!(core::mem::size_of::<Level>() == 1);
const _: () = assert!(core::mem::size_of::<Perms>() == 4);
const _: () = assert!(core::mem::size_of::<Wide>() == 8);
const _: () = assert!(core::mem::offset_of!(Record, perms) == 4);
const _: () = assert!(Packed::KIND_SHIFT == 8 && Packed::KIND_MASK == 0xff00);
const _: () = assert!(Packed::DEPTH_SHIFT == 16 && Packed::DEPTH_MASK == 0xf_0000);
const _: () = assert!(Packed::ALL.0 == 0xf_ff05);
const _: () = assert!(Packed(0x0005_2a01).kind().0 == 0x2a);
// A whole u8 word as one range: the getter is the word, the setter replaces it.
const _: () = assert!(seam::Octet::CODE_SHIFT == 0 && seam::Octet::CODE_MASK == 0xff);
const _: () = assert!(seam::Octet::ALL.0 == 0xff);
const _: () = assert!(seam::Octet(0xa5).code() == 0xa5);
const _: () = assert!(seam::Octet(0x12).with_code(0xa5).0 == 0xa5);

#[unsafe(no_mangle)]
pub extern "C" fn consts_status(view: *const Record) -> Status {
    unsafe { (*view).status }
}
#[unsafe(no_mangle)]
pub extern "C" fn consts_set_level(_level: Level, _session: *const Session) {}
#[unsafe(no_mangle)]
pub extern "C" fn consts_blocking_status(_session: *const Session) -> Status {
    Status::OK
}
seam::assert_implementations!(seam);

fn classify(status: Status) -> u32 {
    match status {
        Status::NULL_RESULT => 1,
        Status::OK => 2,
        Status::INTERNAL_ERROR => 3,
        Status::SYNTAX_ERROR => 4,
        _ => 0,
    }
}

#[test]
fn enums_match_as_patterns_and_keep_open_values() {
    assert_eq!(classify(Status(-1)), 1);
    assert_eq!(classify(Status::SYNTAX_ERROR), 4);
    assert_eq!(classify(Status(12345)), 0);
    assert_eq!(Status::OK.name(), Some("ok"));
    assert_eq!(Status::NULL_RESULT.name(), Some("null_result"));
    assert_eq!(Status(12345).name(), None);
    assert_eq!(Level(7).name(), None);
    assert_eq!(Level::HIGH.name(), Some("high"));
}

#[test]
fn flags_compose() {
    let rw = Perms::READ | Perms::WRITE;
    assert!(rw.contains(Perms::READ));
    assert!(!rw.contains(Perms::STICKY));
    assert!(rw.intersects(Perms::WRITE | Perms::STICKY));
    assert!(!Perms::NONE.intersects(Perms::ALL));
    assert_eq!(rw & Perms::WRITE, Perms::WRITE);
    assert_eq!(rw.union(Perms::STICKY), Perms::ALL);
    assert_eq!(Perms::READ.when(true) | Perms::WRITE.when(false), Perms::READ);
    assert_eq!(Wide::LAST.when(true), Wide::LAST);
    assert_eq!(Wide::LAST.when(false), Wide::NONE);
}

#[test]
fn record_fields_use_newtypes() {
    let r = Record { status: Status::OK, perms: Perms::READ, wide: Wide::FIRST, raw: 5 };
    assert_eq!(r.status, Status::OK);
    assert!(r.perms.contains(Perms::READ));
    assert_eq!(consts_status(&r), Status::OK);
}

#[test]
fn ranges_read_and_write_their_bits() {
    let p = Packed::OPEN.with_kind(Kind::BOXED).with_depth(9);
    assert_eq!(p.0, 0x0009_0501);
    assert_eq!(p.kind(), Kind::BOXED);
    assert_eq!(p.depth(), 9);
    assert!(p.contains(Packed::OPEN));
    // The setter keeps the other bits and masks the value to the width.
    let q = p.with_kind(Kind::LAST).with_depth(0x13);
    assert_eq!(q.0, 0x0003_ff01);
    assert_eq!(q.kind(), Kind::LAST);
    assert_eq!(q.with_kind(Kind::PLAIN).kind(), Kind::PLAIN);
    // An undeclared kind is still a Kind; a bit outside every bit and range is unknown.
    assert_eq!(Packed(0x7700).kind(), Kind(0x77));
    assert_eq!(Packed(0x7700).kind().name(), None);
    assert_eq!(q.0 & !Packed::ALL.0, 0);
    assert_ne!(Packed(q.0 | 0x2).0 & !Packed::ALL.0, 0);
    let n = Node { tag: NodeTag::READ, packed: p, operand: 0 };
    assert_eq!(n.packed.kind(), Kind::BOXED);
}

fn describe(n: &Node) -> String {
    match n.tag {
        NodeTag::BINARY => format!("binary {:?}", n.operand_as_binary_op().name()),
        NodeTag::UNARY => format!("unary {:?}", n.operand_as_unary_op().name()),
        NodeTag::WRITE => format!("write {}", n.operand_as_write_flags().contains(WriteFlags::RELEASE)),
        _ => format!("other {}", n.operand),
    }
}

#[test]
fn tagged_fields_read_through_the_case_group() {
    let mut n = Node { tag: NodeTag::BINARY, packed: Packed::NONE, operand: 1 };
    assert_eq!(n.operand_as_binary_op(), BinaryOp::SUB);
    assert_eq!(describe(&n), "binary Some(\"sub\")");
    n.tag = NodeTag::UNARY;
    assert_eq!(n.operand_as_unary_op(), UnaryOp::INVERT);
    assert_eq!(describe(&n), "unary Some(\"invert\")");
    n.tag = NodeTag::WRITE;
    n.operand = (WriteFlags::RELEASE | WriteFlags::VOLATILE_STORE).0;
    assert_eq!(describe(&n), "write true");
    // No check of the tag: an accessor reads the field as its group whatever the tag says.
    assert_eq!(n.operand_as_binary_op(), BinaryOp(3));
    n.tag = NodeTag(9);
    assert_eq!(describe(&n), "other 3");
}
