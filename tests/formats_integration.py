# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Persisted formats (myna#167): the generated Rust and Java writers produce the same canonical
bytes, and the two validating readers return the same verdict, (kind, offset, node), for every
input. Run against the built native CLI. Set MYNA_UPDATE_GOLDEN=1 to rewrite tests/formats/golden."""
import os
from pathlib import Path
import random
import shutil
import struct
import sys
import unittest
from support import BINARY, ROOT, main, run

FIXTURE = ROOT / "tests/formats"
GOLDEN = FIXTURE / "golden"
NONE = 0xFFFFFFFF


def pad4(n):
    return (n + 3) & ~3


class File:
    """A mutable view of one encoded file of the fixture's family."""

    def __init__(self, data):
        self.b = bytearray(data)
        sc, sb, nc, bb = struct.unpack_from("<IIII", self.b, 20)
        self.sc, self.sb, self.nc, self.bb = sc, sb, nc, bb
        self.sbytes = 40 + 4 * sc
        self.body = self.sbytes + pad4(sb)
        self.index = self.body + bb
        self.nodes = [self.body + struct.unpack_from("<I", self.b, self.index + 4 * i)[0] for i in range(nc)]

    def u32(self, at, value=None):
        if value is None:
            return struct.unpack_from("<I", self.b, at)[0]
        struct.pack_into("<I", self.b, at, value)

    def u16(self, at, value):
        struct.pack_into("<H", self.b, at, value)

    def u64(self, at, value):
        struct.pack_into("<Q", self.b, at, value)

    def tag(self, i):
        return struct.unpack_from("<H", self.b, self.nodes[i])[0]

    def node(self, tag, nth=0):
        """The index of the nth node with this tag."""
        return [i for i in range(self.nc) if self.tag(i) == tag][nth]

    def string_start(self, i):
        return self.sbytes + (0 if i == 0 else self.u32(40 + 4 * (i - 1)))


# Fixed offsets of the fixture's layouts (tests/formats/doc.seam), from each node's start.
STYLE, PARAGRAPH, RAW_TEXT, SECTION, DOCUMENT, META, MARKER = range(1, 8)
STYLE_FACE, STYLE_FLAGS, STYLE_ALIGN, STYLE_SIZE, STYLE_PARENT = 4, 8, 12, 13, 21
PARAGRAPH_STYLE, PARAGRAPH_ATTRS, PARAGRAPH_PAYLOAD = 4, 12, 16
SECTION_TITLE, SECTION_BLOCKS = 4, 12
# The full golden's first paragraph: spans Text(8) Break(4) Styled(16), then attrs of 12 bytes.
P1_ATTRS = PARAGRAPH_PAYLOAD + 8 + 4 + 16


def style_by_size(f, size):
    for i in range(f.nc):
        if f.tag(i) == STYLE and struct.unpack_from("<Q", f.b, f.nodes[i] + STYLE_SIZE)[0] == size:
            return i
    raise AssertionError(size)


def full_cases(g):
    """Mutations of docfile-full: name -> (bytes, expected full kind, expected structural kind)."""
    cases = {}

    def case(name, kind, structural=None, edit=None, data=None):
        f = File(g)
        if edit:
            edit(f)
        cases[name] = (bytes(f.b) if data is None else data, kind, structural or kind)

    f = File(g)
    p1, p2 = f.node(PARAGRAPH, 0), f.node(PARAGRAPH, 1)
    sec1, sec2 = f.node(SECTION, 0), f.node(SECTION, 1)
    raw = f.node(RAW_TEXT)
    base = f.node(STYLE, 0)
    case("truncated", "Truncated", data=bytes(g[:39]))
    case("truncated-1", "BadLength", data=bytes(g[:-1]))
    case("trailing", "BadLength", data=bytes(g) + b"\0")
    case("magic", "BadMagic", edit=lambda f: f.u32(0, f.u32(0) ^ 1))
    case("version", "BadVersion", edit=lambda f: f.u32(4, 2))
    case("fingerprint", "BadFingerprint", edit=lambda f: f.u32(8, f.u32(8) ^ 1))
    case("header-rejected", "HeaderRejected", edit=lambda f: f.u32(16, 8))
    case("reserved", "BadHeader", edit=lambda f: f.u32(36, 1))
    case("body-unaligned", "BadLength", edit=lambda f: f.u32(32, f.bb + 2))
    case("no-nodes", "BadLength", edit=lambda f: f.u32(28, 0))
    case("index-unaligned", "BadIndex", edit=lambda f: f.u32(f.index + 12, f.u32(f.index + 12) + 2))
    case("index-off-by-4", "BadNode", edit=lambda f: f.u32(f.index + 12, f.u32(f.index + 12) + 4))
    case("string-ends", "BadStringTable", edit=lambda f: f.u32(44, 0))
    assert f.sb % 4, "the full golden's strings end unpadded, so a padding byte exists"
    case("string-padding", "BadStringTable", edit=lambda f: f.b.__setitem__(f.sbytes + f.sb, 1))
    case("utf8", "BadUtf8", edit=lambda f: f.b.__setitem__(f.sbytes, 0xff))
    # "café" (string 2) then "naïve": end the first inside its two-byte "é".
    case("utf8-split", "BadUtf8", edit=lambda f: f.u32(40 + 4 * 2, f.u32(40 + 4 * 2) - 1))
    case("unknown-tag", "BadTag", edit=lambda f: f.u16(f.nodes[0], 99))
    case("tag-reserved", "BadNode", edit=lambda f: f.u16(f.nodes[0] + 2, 1))
    case("enum", "BadValue", edit=lambda f: f.b.__setitem__(f.nodes[base] + STYLE_ALIGN, 3))
    case("flags", "BadValue", edit=lambda f: f.u32(f.nodes[base] + STYLE_FLAGS, 0x100))
    case("bool", "BadValue", edit=lambda f: f.b.__setitem__(f.nodes[p1] + P1_ATTRS + 8, 2))
    case("element-tag", "BadElement", edit=lambda f: f.u16(f.nodes[p1] + PARAGRAPH_PAYLOAD, 9))
    case("element-reserved", "BadElement", edit=lambda f: f.u16(f.nodes[p1] + PARAGRAPH_PAYLOAD + 2, 1))
    case("string-range", "BadString", edit=lambda f: f.u32(f.nodes[base] + STYLE_FACE, 1000))
    case("string-order", "StringOrder", edit=lambda f: f.u32(f.nodes[base] + STYLE_FACE, 1))
    case("self-ref", "BadRef", edit=lambda f: f.u32(f.nodes[p1] + PARAGRAPH_STYLE, p1))
    case("ref-count", "BadRef", edit=lambda f: f.u32(f.nodes[p1] + PARAGRAPH_STYLE, f.nc))
    case("wrong-kind", "BadRefKind", edit=lambda f: f.u32(f.nodes[p1] + PARAGRAPH_STYLE, f.node(MARKER)))
    case("absent-required", "BadRef", edit=lambda f: f.u32(f.nodes[p1] + PARAGRAPH_STYLE, NONE))
    case("unique-one-parent", "SharedUnique", edit=lambda f: f.u32(f.nodes[sec1] + SECTION_BLOCKS + 4, p1))
    case("unique-two-parents", "SharedUnique", edit=lambda f: f.u32(f.nodes[sec2] + SECTION_BLOCKS, p1))
    case("field-too-long", "FieldTooLong", edit=lambda f: f.u32(f.nodes[p1] + PARAGRAPH_ATTRS, 256))
    case("count-overflow", "BadNode", edit=lambda f: f.u32(f.nodes[raw] + 4, 1 << 20))
    # Style 5 (size 2) is referenced only by the first attribute; point it at style 4 instead.
    case("unreferenced", "Unreferenced",
         edit=lambda f: f.u32(f.nodes[p1] + P1_ATTRS + 4, style_by_size(f, 1)))
    # "title-2", the last string, is used only by the second section's title.
    case("unused-string", "UnusedString", edit=lambda f: f.u32(f.nodes[sec2] + SECTION_TITLE, 6))
    # Canonical form only: structural validation accepts these.
    case("order", "NodeOrder", "OK", edit=lambda f: (f.u32(f.nodes[sec2] + SECTION_BLOCKS, p2 + 1),
                                                      f.u32(f.nodes[sec2] + SECTION_BLOCKS + 4, p2)))
    case("duplicate-node", "DuplicateNode", "OK",
         edit=lambda f: f.u64(f.nodes[style_by_size(f, 2)] + STYLE_SIZE, 1))
    # "ab" (string 7) and "ac": make the second equal to the first.
    case("duplicate-string", "DuplicateString", "OK",
         edit=lambda f: f.b.__setitem__(f.string_start(8) + 1, ord("b")))
    return cases


def other_cases(fragment, tiny):
    cases = {}
    f = File(fragment)
    f.u16(f.nodes[0], MARKER)  # a tag of the family that DocFragment's roots cannot reach
    cases["docfragment-unreachable-tag"] = (bytes(f.b), "BadTag", "BadTag")
    f = File(fragment)
    f.u32(16, 1)
    cases["docfragment-header"] = (bytes(f.b), "BadHeader", "BadHeader")
    # Keep only the style: a valid stream whose last node is not a root.
    f = File(fragment)
    style = bytes(f.b[f.nodes[0]:f.nodes[1]])
    head = bytearray(f.b[:f.body])
    struct.pack_into("<II", head, 28, 1, len(style))
    cases["docfragment-bad-root"] = (bytes(head) + style + struct.pack("<I", 0), "BadRoot", "BadRoot")

    def tiny_case(name, kind, edit):
        f = File(tiny)
        edit(f)
        cases["doctiny-" + name] = (bytes(f.b), kind, kind)

    s1 = style_by_size(File(tiny), 1)
    p = File(tiny).node(PARAGRAPH)
    tiny_case("too-many-strings", "TooManyStrings", lambda f: f.u32(20, 4))
    tiny_case("too-many-nodes", "TooManyNodes", lambda f: f.u32(28, 5))
    cases["doctiny-too-large"] = (bytes(tiny) + bytes(400), "TooLarge", "TooLarge")
    # The golden is at depth 3: give the paragraph's style a parent.
    def deeper(f):
        # Chain the styles and hang the paragraph from the lower one; drop the attributes'
        # references so work stays within its limit.
        s1, s2 = style_by_size(f, 1), style_by_size(f, 2)
        f.u32(f.nodes[s1] + STYLE_PARENT, s2)
        f.u32(f.nodes[p] + PARAGRAPH_STYLE, s1)
        for k in range(3):
            f.u32(f.nodes[p] + PARAGRAPH_PAYLOAD + 12 * k + 4, NONE)
    tiny_case("too-deep", "TooDeep", deeper)
    # ... and at work 6: point the fourth attribute at a style too.
    tiny_case("too-much-work", "TooMuchWork", lambda f: f.u32(f.nodes[p] + PARAGRAPH_PAYLOAD + 3 * 12 + 4, s1))
    return cases


class FormatsTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.work = ROOT / "build/formats-test"
        shutil.rmtree(cls.work, ignore_errors=True)
        generated = cls.work / "generated"
        run(BINARY, "generate", FIXTURE / "doc.seam", "--out", generated)
        rust = cls.work / "rust"
        rust.mkdir(parents=True)
        shutil.copy(generated / "formats.rs", rust / "formats.rs")
        shutil.copy(FIXTURE / "check.rs", rust / "check.rs")
        cls.rust = cls.work / "check"
        run("rustc", "--edition=2024", "-Dwarnings", "-C", "opt-level=1", "-C", "debug-assertions",
            "-C", "overflow-checks", rust / "check.rs", "-o", cls.rust)
        classes = cls.work / "classes"
        run("javac", "-Xlint:all", "-Werror", "-d", classes, *sorted(generated.glob("Doc*.java")),
            FIXTURE / "FormatsMain.java")
        cls.java = ("java", "-cp", classes, "fixture.formats.FormatsMain")

    def both(self, *args):
        rust = run(self.rust, *args).stdout
        java = run(*self.java, *args).stdout
        self.assertEqual(rust, java)
        return rust

    def test_writers_agree_with_each_other_and_the_golden_files(self):
        """Both writers build every value in two creation orders, round-trip it through their
        reader and writer, and produce the checked-in bytes."""
        out_rust, out_java = self.work / "golden-rust", self.work / "golden-java"
        out_rust.mkdir()
        out_java.mkdir()
        run(self.rust, "golden", out_rust)
        run(*self.java, "golden", out_java)
        names = sorted(p.name for p in out_rust.glob("*.bin"))
        self.assertEqual(names, sorted(p.name for p in out_java.glob("*.bin")))
        verdicts = self.both("verdict", *sorted(out_rust.glob("*.bin")))
        if os.environ.get("MYNA_UPDATE_GOLDEN") == "1":
            for name in names:
                shutil.copy(out_rust / name, GOLDEN / name)
            (GOLDEN / "verdicts.txt").write_text(verdicts)
        for name in names:
            self.assertEqual((out_rust / name).read_bytes(), (out_java / name).read_bytes(), name)
            self.assertEqual((out_rust / name).read_bytes(), (GOLDEN / name).read_bytes(), name)
        self.assertEqual(verdicts, (GOLDEN / "verdicts.txt").read_text())
        self.assertNotIn("ERR", verdicts)

    def test_writer_errors(self):
        rust = run(self.rust, "writer-errors").stdout
        java = run(*self.java, "writer-errors").stdout
        self.assertEqual(rust, "".join(l for l in java.splitlines(keepends=True) if not l.startswith("java ")))
        if os.environ.get("MYNA_UPDATE_GOLDEN") == "1":
            (GOLDEN / "writer-errors.txt").write_text(java)
        self.assertEqual(java, (GOLDEN / "writer-errors.txt").read_text())
        # Every case is an error but the java accepts-* ones: valid adds after a rejection, and
        # 100,000 distinct unique children, which the writer checks in linear time.
        for line in java.splitlines():
            words = line.split()
            self.assertEqual(words[-1] == "OK", words[1].startswith("accepts-"), line)

    def test_rejects(self):
        """One input per error kind; both readers return the same kind, offset and node."""
        golden = {p.stem: p.read_bytes() for p in GOLDEN.glob("*.bin")}
        cases = {"docfile-" + k: v for k, v in full_cases(golden["docfile-full"]).items()}
        cases.update(other_cases(golden["docfragment-paragraph"], golden["doctiny-section"]))
        rejects = self.work / "rejects"
        rejects.mkdir()
        for name, (data, _, _) in cases.items():
            (rejects / f"{name}.bin").write_bytes(data)
        out = self.both("verdict", *sorted(rejects.glob("*.bin")))
        seen = {}
        for line in out.splitlines():
            if line.startswith("  "):
                continue
            name, full, structural = line.split(" ", 1)[0], line.split(" full=")[1].split(" structural=")[0], line.split(" structural=")[1]
            seen[name.removesuffix(".bin")] = (full.split()[1] if full != "OK" else "OK",
                                               structural.split()[1] if structural != "OK" else "OK")
        for name, (_, kind, structural) in cases.items():
            self.assertEqual(seen[name], (kind, structural), name)
        kinds = {kind for _, kind, _ in cases.values()}
        # Every error kind but OutOfMemory has a case.
        self.assertEqual(len(kinds), 31, sorted(kinds))

    def test_other_shapes_compile(self):
        """tests/formats/shapes.seam: non-Copy ids, kind references and other shapes the fixture
        does not reach compile without warnings in both languages."""
        out = self.work / "shapes"
        run(BINARY, "generate", FIXTURE / "shapes.seam", "--out", out)
        run("rustc", "--edition=2024", "--crate-type=lib", "-Dwarnings", out / "formats.rs", "-o", out / "libformats.rlib")
        names = ("ShapesNodes", "ShapesWriter", "ShapesException", "OnlyReader", "OtherReader")
        run("javac", "-Xlint:all", "-Werror", "-d", out / "classes", *(out / f"{n}.java" for n in names))

    def test_mutations_get_the_same_verdict_from_both_readers(self):
        """Seeded random corruptions of every golden file: no crash, and identical verdicts."""
        rng = random.Random(167)
        mutated = self.work / "mutated"
        mutated.mkdir()
        for path in sorted(GOLDEN.glob("*.bin")):
            data = path.read_bytes()
            for k in range(400):
                b = bytearray(data)
                for _ in range(rng.choice((1, 1, 2, 4))):
                    if not b:
                        break
                    at = rng.randrange(len(b))
                    choice = rng.random()
                    if choice < 0.6:
                        b[at] = rng.randrange(256)
                    elif choice < 0.8 and at + 4 <= len(b):
                        struct.pack_into("<I", b, at - at % 4, rng.choice((0, 1, 2, 3, 4, 0xFFFFFFFF, rng.randrange(1 << 32))))
                    else:
                        b = b[:at] if rng.random() < 0.5 else b + bytes(rng.randrange(1, 9))
                (mutated / f"{path.stem}-m{k:03d}.bin").write_bytes(bytes(b))
        out = self.both("verdict", *sorted(mutated.glob("*.bin")))
        self.assertGreater(out.count("ERR"), 1000)


if __name__ == "__main__": main(sys.modules[__name__], "formats-integration")
