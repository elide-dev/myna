// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.backend;

import dev.elide.myna.model.Seam.*;
import dev.elide.myna.model.Seam.Module;
import java.util.*;

/** Safe owners and call borrows over the raw ABI, relying on explicit lifecycle contracts. */
final class RustOwnership {
  private RustOwnership() {}

  static String generate(Module m) {
    if (m.returnedResources().isEmpty() && m.callBorrows().isEmpty()) return "";
    boolean batches = m.callBorrows().stream().anyMatch(BufferBorrow::batch);
    var out = new StringBuilder(batches ? BATCH_ADAPTER : "");
    out.append(
        "\n"
            + "/// Ownership APIs. Native implementations must uphold the descriptor's"
            + " lifecycle contracts.\n"
            + "pub mod ownership {\n");
    if (batches) out.append(LEND_BATCH);
    for (Resource r : m.returnedResources()) {
      String n = r.name();
      out.append("    pub struct ")
          .append(n)
          .append(" {\n")
          .append("        pointer: core::ptr::NonNull<super::")
          .append(n)
          .append(">,\n")
          .append("        _thread: core::marker::PhantomData<*mut ()>,\n    }\n");
      if (r.shared())
        out.append(
                "    // Explicit shared resource contract includes destruction on any thread.\n"
                    + "    unsafe impl core::marker::Send for ")
            .append(n)
            .append(" {}\n    unsafe impl core::marker::Sync for ")
            .append(n)
            .append(" {}\n");
      out.append("    impl ").append(n).append(" {\n");
      for (String name : r.creates()) constructor(m, r, name, out);
      out.append(
              "        /// # Safety\n"
                  + "        /// Transfer sole ownership of a live allocation from this resource's"
                  + " allocator.\n"
                  + "        /// No outstanding borrows; obey the descriptor's thread and"
                  + " destruction contracts.\n")
          .append("        pub unsafe fn from_raw(pointer: *mut super::")
          .append(n)
          .append(
              ") -> Option<Self> {\n"
                  + "            core::ptr::NonNull::new(pointer).map(|pointer| Self { pointer,"
                  + " _thread: core::marker::PhantomData })\n"
                  + "        }\n")
          .append("        pub fn into_raw(self) -> *mut super::")
          .append(n)
          .append(
              " {\n"
                  + "            let owned = core::mem::ManuallyDrop::new(self);\n"
                  + "            owned.pointer.as_ptr()\n"
                  + "        }\n")
          .append("        pub fn borrow(&self) -> ")
          .append(n)
          .append("Borrow<'_> {\n            ")
          .append(n)
          .append("Borrow { owner: self }\n        }\n    }\n")
          .append("    impl core::ops::Drop for ")
          .append(n)
          .append(" {\n        fn drop(&mut self) { unsafe { super::")
          .append(r.destroy())
          .append("(self.pointer.as_ptr()); } }\n    }\n")
          .append("    pub struct ")
          .append(n)
          .append("Borrow<'owner> { owner: &'owner ")
          .append(n)
          .append(" }\n")
          .append("    impl ")
          .append(n)
          .append("Borrow<'_> {\n")
          .append("        /// Raw escape hatch. Dereferencing or passing it on remains unsafe.\n")
          .append("        pub fn as_ptr(&self) -> *mut super::")
          .append(n)
          .append(" { self.owner.pointer.as_ptr() }\n    }\n");
    }
    // A present empty slice is a dangling pointer, which no align fact holds for (LLVM's align
    // attribute does not depend on the extent): it is lent an aligned static byte instead.
    var aligns = new TreeSet<Long>();
    for (CallBorrows call : CallBorrows.of(m))
      for (BufferBorrow b : call.borrows()) if (call.align(b) > 1) aligns.add(call.align(b));
    for (long align : aligns)
      out.append("    #[repr(align(")
          .append(align)
          .append("))]\n    struct Empty")
          .append(align)
          .append("([u8; 1]);\n    static EMPTY_")
          .append(align)
          .append(": Empty")
          .append(align)
          .append(" = Empty")
          .append(align)
          .append("([0]);\n");
    for (CallBorrows call : CallBorrows.of(m)) {
      Function f = call.function();
      var params = new ArrayList<String>();
      var args = new ArrayList<String>();
      // A nullable buffer is an Option of the slice. It is consumed once, into the pointer and
      // byte count the call passes, under the parameter's own name: no other name is introduced.
      var absent = new StringBuilder();
      // Caller contracts of align facts, checked in debug builds only. An empty slice is lent an
      // aligned static in its place, so only the address of a non-empty one is checked.
      var alignments = new StringBuilder();
      // Each batch lends its slices through lend_batch, whose closure makes the call: its arrays
      // live on that function's stack, or in its vectors, for exactly the call.
      var counts = new StringBuilder();
      var lends = new StringBuilder();
      String close = "";
      for (Parameter p : f.parameters()) {
        BufferBorrow buffer = call.buffer(p);
        BufferBorrow length = call.length(p);
        BufferBorrow count = call.count(p);
        if (buffer != null && buffer.batch()) {
          params.add(
              p.name() + ": " + (buffer.elementsNullable() ? "&[Option<&[u8]>]" : "&[&[u8]]"));
          args.add(p.name());
          lends
              .append("lend_batch::<_, ")
              .append(Types.rust(call.lengthElement(buffer)))
              .append(", _>(")
              .append(p.name())
              .append(", ")
              .append(buffer.elementsNullable() ? "optional" : "present")
              .append(", |")
              .append(p.name())
              .append(", ")
              .append(buffer.length())
              .append("| ");
          close += ")";
          continue;
        }
        if (length != null && length.batch()) {
          args.add(p.name());
          continue;
        }
        if (count != null) {
          counts
              .append("        let ")
              .append(p.name())
              .append(": ")
              .append(Types.rust(p.type()))
              .append(" = ")
              .append(count.parameter())
              .append(".len().try_into().expect(\"batch count exceeds ABI range\");\n");
          args.add(p.name());
          continue;
        }
        if (buffer != null) {
          boolean shared = p.access() == Access.READ;
          String slice = shared ? "&[u8]" : "&mut [u8]";
          String pointer = shared ? ".as_ptr()" : ".as_mut_ptr()";
          long align = CallBorrows.align(p);
          if (align > 1) {
            String at = call.nullable(buffer) ? p.name() + ".0" : p.name() + pointer;
            String len = call.nullable(buffer) ? p.name() + ".1" : p.name() + ".len()";
            alignments
                .append("        debug_assert!(")
                .append(len)
                .append(" == 0 || (")
                .append(at)
                .append(" as usize) & ")
                .append(align - 1)
                .append(" == 0, \"")
                .append(f.name())
                .append('.')
                .append(p.name())
                .append(" requires ")
                .append(align)
                .append("-byte alignment\");\n");
          }
          if (call.nullable(buffer)) {
            params.add(p.name() + ": Option<" + slice + ">");
            args.add(p.name() + ".0");
            absent
                .append("        let ")
                .append(p.name())
                .append(" = ")
                .append(p.name())
                .append(".map_or((core::ptr::")
                .append(shared ? "null" : "null_mut")
                .append("(), 0), |")
                .append(p.name())
                .append("| (")
                .append(lent(p.name(), pointer, align, shared))
                .append(", ")
                .append(p.name())
                .append(".len()));\n");
          } else {
            params.add(p.name() + ": " + slice);
            args.add(lent(p.name(), pointer, align, shared));
          }
        } else if (length != null)
          args.add(
              length.parameter()
                  + (call.nullable(length) ? ".1" : ".len()")
                  + ".try_into().expect(\"buffer length exceeds ABI range\")");
        else {
          // Declared types live in the parent module; scalars are spelled the same either way.
          params.add(p.name() + ": " + Types.rust(p, "super::"));
          args.add(p.name());
        }
      }
      // The slices vouch for the buffers. Nothing vouches for any other pointer, so a wrapper that
      // passes one through is as unsafe as the raw function is for that argument.
      boolean pointers = call.passesPointers();
      if (pointers)
        out.append(
            "    /// # Safety\n"
                + "    /// Every pointer other than a borrowed buffer must satisfy the raw"
                + " function's requirements.\n");
      // Only a constructor's buffer wrapper returns a pointer: an owned one, which leaks unless the
      // caller adopts it (the resource's own constructor does).
      if (f.returns() instanceof Pointer)
        out.append(
            "    #[must_use = \"the result is an owned resource; adopt it with from_raw or use the"
                + " owner's constructor\"]\n");
      out.append(pointers ? "    pub unsafe fn " : "    pub fn ")
          .append(f.name())
          .append('(')
          .append(String.join(", ", params))
          .append(')');
      if (f.returns() != Scalar.VOID)
        out.append(" -> ").append(Types.rust(f.returns(), f.returnsGroup(), "super::"));
      out.append(" {\n")
          .append(absent)
          .append(alignments)
          .append(counts)
          .append("        ")
          .append(lends)
          .append("unsafe { super::")
          .append(f.name())
          .append('(')
          .append(String.join(", ", args))
          .append(") }")
          .append(close)
          .append("\n    }\n");
    }
    return out.append("}\n").toString();
  }

  /**
   * One constructor of a resource, named {@code create} for a resource's only constructor and after
   * its import otherwise. A constructor with call borrows takes their slices and calls the buffer
   * wrapper of its import, which lends them for the call only: the resource it returns is fresh,
   * never a view into a buffer.
   */
  private static void constructor(Module m, Resource r, String name, StringBuilder out) {
    Function create =
        m.functions().stream().filter(f -> f.name().equals(name)).findFirst().orElseThrow();
    boolean buffers = m.callBorrows().stream().anyMatch(b -> b.function().equals(name));
    CallBorrows call = CallBorrows.of(m, create);
    var parameters = new ArrayList<String>();
    var arguments = new ArrayList<String>();
    for (Parameter p : create.parameters()) {
      if (buffers && call.derived(p)) continue;
      BufferBorrow buffer = buffers ? call.buffer(p) : null;
      arguments.add(p.name());
      if (buffer != null && buffer.batch())
        parameters.add(
            p.name() + ": " + (buffer.elementsNullable() ? "&[Option<&[u8]>]" : "&[&[u8]]"));
      else if (buffer == null)
        // Declared types live in the parent module; scalars are spelled the same either way.
        parameters.add(p.name() + ": " + Types.rust(p, "super::"));
      else {
        String slice = p.access() == Access.READ ? "&[u8]" : "&mut [u8]";
        parameters.add(p.name() + ": " + (call.nullable(buffer) ? "Option<" + slice + ">" : slice));
      }
    }
    // Nothing vouches for a pointer argument, or for how long the resource depends on it. The
    // slices of a constructor's buffers vouch for themselves, for exactly the call.
    boolean pointers =
        create.parameters().stream()
            .anyMatch(p ->
                p.type() instanceof Pointer
                    && (!buffers || (call.buffer(p) == null && call.length(p) == null)));
    out.append(
            pointers
                ? "        /// # Safety\n"
                    + "        /// Every pointer argument"
                    + (buffers ? " other than a borrowed buffer" : "")
                    + " must satisfy the raw function's requirements, for as long\n"
                    + "        /// as the created resource depends on it.\n"
                    + "        pub unsafe fn "
                : "        pub fn ")
        .append(r.wrapperName(name))
        .append('(')
        .append(String.join(", ", parameters))
        .append(") -> Option<Self> {\n            core::ptr::NonNull::new(")
        // The buffer wrapper is this module's function of the same name.
        .append(
            buffers && !call.passesPointers()
                ? "self::"
                : "unsafe { " + (buffers ? "self::" : "super::"))
        .append(name)
        .append('(')
        .append(String.join(", ", arguments))
        .append(buffers && !call.passesPointers() ? ")" : ") }")
        .append(
            ").map(|pointer| Self { pointer, _thread: core::marker::PhantomData })\n"
                + "        }\n");
  }

  /**
   * The implementation side of a batch: a safe view of the raw arrays an import receives. Emitted
   * once, at the top level of {@code seam.rs}, when a module declares a batch.
   */
  private static final String BATCH_ADAPTER =
"""

/// A length or count of a borrowed batch, as its import declares it.
pub trait BatchLength: Copy {
    /// The value as a byte length or element count. Negative and oversized values break the
    /// caller's contract, which debug builds assert.
    fn to_usize(self) -> usize;
}
impl BatchLength for u32 { #[inline(always)] fn to_usize(self) -> usize { self as usize } }
impl BatchLength for i32 {
    #[inline(always)]
    fn to_usize(self) -> usize { debug_assert!(self >= 0, "negative batch length"); self as usize }
}
impl BatchLength for u64 {
    #[inline(always)]
    fn to_usize(self) -> usize { debug_assert!(usize::try_from(self).is_ok(), "batch length exceeds usize"); self as usize }
}
impl BatchLength for i64 {
    #[inline(always)]
    fn to_usize(self) -> usize { debug_assert!(self >= 0 && usize::try_from(self).is_ok(), "invalid batch length"); self as usize }
}

/// The buffers of a borrowed batch, as an implementation receives them: `sources[i]` holds
/// `lengths[i]` bytes for `i < count`. Iterating yields each buffer in order; `get(i)` indexes the
/// buffers not yet yielded.
#[derive(Clone)]
pub struct BufferBatch<'a, L: BatchLength = u64> {
    sources: *const *mut u8,
    lengths: *const L,
    count: usize,
    _buffers: core::marker::PhantomData<&'a [u8]>,
}
impl<'a, L: BatchLength> BufferBatch<'a, L> {
    /// # Safety
    /// Unless `count` is zero, when either array may be null, `sources` and `lengths` are valid for
    /// `count` reads, and each `sources[i]` is a non-null buffer valid for reads of `lengths[i]`
    /// bytes (it may be null when its length is zero), unmutated for `'a`.
    #[inline(always)]
    pub unsafe fn from_raw<C: BatchLength>(sources: *const *mut u8, lengths: *const L, count: C) -> Self {
        let count = count.to_usize();
        debug_assert!(count == 0 || (!sources.is_null() && !lengths.is_null()), "null batch arrays");
        Self { sources, lengths, count, _buffers: core::marker::PhantomData }
    }
    /// The buffer at `index` among those not yet yielded, or `None` past the end.
    #[inline(always)]
    pub fn get(&self, index: usize) -> Option<&'a [u8]> {
        if index >= self.count { return None; }
        let (pointer, length) = unsafe { (self.sources.add(index).read(), self.lengths.add(index).read().to_usize()) };
        debug_assert!(!pointer.is_null() || length == 0, "null buffer with a nonzero length");
        Some(if length == 0 { &[] } else { unsafe { core::slice::from_raw_parts(pointer, length) } })
    }
}
impl<'a, L: BatchLength> Iterator for BufferBatch<'a, L> {
    type Item = &'a [u8];
    #[inline(always)]
    fn next(&mut self) -> Option<&'a [u8]> {
        let item = self.get(0)?;
        self.sources = unsafe { self.sources.add(1) };
        self.lengths = unsafe { self.lengths.add(1) };
        self.count -= 1;
        Some(item)
    }
    #[inline(always)]
    fn size_hint(&self) -> (usize, Option<usize>) { (self.count, Some(self.count)) }
}
impl<L: BatchLength> ExactSizeIterator for BufferBatch<'_, L> {}
impl<L: BatchLength> core::iter::FusedIterator for BufferBatch<'_, L> {}

/// The buffers of a borrowed batch whose elements may be absent (`elements_nullable=true`): a null
/// element, which has length zero, is `None`.
#[derive(Clone)]
pub struct OptionalBufferBatch<'a, L: BatchLength = u64> {
    sources: *const *mut u8,
    lengths: *const L,
    count: usize,
    _buffers: core::marker::PhantomData<&'a [u8]>,
}
impl<'a, L: BatchLength> OptionalBufferBatch<'a, L> {
    /// # Safety
    /// As for [`BufferBatch::from_raw`], except that any element may be null with length zero.
    #[inline(always)]
    pub unsafe fn from_raw<C: BatchLength>(sources: *const *mut u8, lengths: *const L, count: C) -> Self {
        let count = count.to_usize();
        debug_assert!(count == 0 || (!sources.is_null() && !lengths.is_null()), "null batch arrays");
        Self { sources, lengths, count, _buffers: core::marker::PhantomData }
    }
    /// The element at `index` among those not yet yielded, or `None` past the end.
    #[inline(always)]
    pub fn get(&self, index: usize) -> Option<Option<&'a [u8]>> {
        if index >= self.count { return None; }
        let (pointer, length) = unsafe { (self.sources.add(index).read(), self.lengths.add(index).read().to_usize()) };
        if pointer.is_null() {
            debug_assert!(length == 0, "null buffer with a nonzero length");
            return Some(None);
        }
        Some(Some(if length == 0 { &[] } else { unsafe { core::slice::from_raw_parts(pointer, length) } }))
    }
}
impl<'a, L: BatchLength> Iterator for OptionalBufferBatch<'a, L> {
    type Item = Option<&'a [u8]>;
    #[inline(always)]
    fn next(&mut self) -> Option<Option<&'a [u8]>> {
        let item = self.get(0)?;
        self.sources = unsafe { self.sources.add(1) };
        self.lengths = unsafe { self.lengths.add(1) };
        self.count -= 1;
        Some(item)
    }
    #[inline(always)]
    fn size_hint(&self) -> (usize, Option<usize>) { (self.count, Some(self.count)) }
}
impl<L: BatchLength> ExactSizeIterator for OptionalBufferBatch<'_, L> {}
impl<L: BatchLength> core::iter::FusedIterator for OptionalBufferBatch<'_, L> {}
""";

  /**
   * The caller side of a batch: the pointer and length arrays a call reads, on the stack for up to
   * eight buffers and in two vectors otherwise, alive for exactly the closure that makes the call.
   * An empty batch passes null arrays.
   */
  private static final String LEND_BATCH =
"""
    extern crate alloc;
    #[inline(always)]
    fn present(buffer: &&[u8]) -> (*mut u8, usize) { (buffer.as_ptr().cast_mut(), buffer.len()) }
    #[inline(always)]
    fn optional(buffer: &Option<&[u8]>) -> (*mut u8, usize) {
        buffer.map_or((core::ptr::null_mut(), 0), |buffer| (buffer.as_ptr().cast_mut(), buffer.len()))
    }
    #[inline(always)]
    fn lend_batch<B, L: Copy + Default + TryFrom<usize>, R>(
        buffers: &[B],
        lend: fn(&B) -> (*mut u8, usize),
        call: impl FnOnce(*const *mut u8, *const L) -> R,
    ) -> R {
        if buffers.is_empty() { return call(core::ptr::null(), core::ptr::null()); }
        let length = |bytes: usize| L::try_from(bytes).ok().expect("buffer length exceeds ABI range");
        if buffers.len() <= 8 {
            let mut pointers = [core::ptr::null_mut::<u8>(); 8];
            let mut lengths = [L::default(); 8];
            for (i, buffer) in buffers.iter().enumerate() {
                let (pointer, bytes) = lend(buffer);
                pointers[i] = pointer;
                lengths[i] = length(bytes);
            }
            call(pointers.as_ptr(), lengths.as_ptr())
        } else {
            let mut pointers = alloc::vec::Vec::with_capacity(buffers.len());
            let mut lengths = alloc::vec::Vec::with_capacity(buffers.len());
            for buffer in buffers {
                let (pointer, bytes) = lend(buffer);
                pointers.push(pointer);
                lengths.push(length(bytes));
            }
            call(pointers.as_ptr(), lengths.as_ptr())
        }
    }
""";

  /**
   * The pointer a call receives for a slice: the slice's own, or, for an empty one under an align
   * fact, a static byte of that alignment instead of the dangling pointer.
   */
  private static String lent(String slice, String pointer, long align, boolean shared) {
    if (align <= 1) return slice + pointer;
    String cast = shared ? "*const u8" : "*mut u8";
    return "if "
        + slice
        + ".is_empty() { EMPTY_"
        + align
        + ".0.as_ptr() as "
        + cast
        + " } else { "
        + slice
        + pointer
        + " }";
  }
}
