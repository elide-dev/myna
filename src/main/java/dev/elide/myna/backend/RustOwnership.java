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
    if (m.ownedResources().isEmpty() && CallBorrows.of(m).isEmpty()) return "";
    var out =
        new StringBuilder(
            "\n"
                + "/// Ownership APIs. Native implementations must uphold the descriptor's"
                + " lifecycle contracts.\n"
                + "pub mod ownership {\n");
    for (Resource r : m.ownedResources()) {
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
      for (Parameter p : f.parameters()) {
        BufferBorrow buffer = call.buffer(p);
        BufferBorrow length = call.length(p);
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
        else if (CallBorrows.cstring(p)) {
          // A C string is its own terminator: the CStr vouches for it.
          if (p.nullable()) {
            params.add(p.name() + ": Option<&::core::ffi::CStr>");
            args.add(p.name() + ".map_or(::core::ptr::null(), |text| text.as_ptr().cast())");
          } else {
            params.add(p.name() + ": &::core::ffi::CStr");
            args.add(p.name() + ".as_ptr().cast()");
          }
        } else {
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
          .append("        unsafe { super::")
          .append(f.name())
          .append('(')
          .append(String.join(", ", args))
          .append(") }\n    }\n");
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
    // A held borrow outlives the call, which only a Truffle cell can express.
    if (m.borrows().stream().anyMatch(b -> b.held() && b.function().equals(name))) return;
    // A constructor with call buffers or C strings calls the import's wrapper, which lends them.
    boolean buffers =
        m.callBorrows().stream().anyMatch(b -> b.function().equals(name))
            || create.parameters().stream().anyMatch(CallBorrows::cstring);
    CallBorrows call = CallBorrows.of(m, create);
    boolean outPointer = m.outPointer(r);
    var parameters = new ArrayList<String>();
    var arguments = new ArrayList<String>();
    for (Parameter p : create.parameters()) {
      if (buffers && call.length(p) != null) continue;
      // The out slot is the constructor's own local.
      if (outPointer && p.name().equals(r.handleParameter())) {
        arguments.add("&mut slot");
        continue;
      }
      BufferBorrow buffer = buffers ? call.buffer(p) : null;
      arguments.add(p.name());
      if (CallBorrows.cstring(p))
        parameters.add(
            p.name() + ": " + (p.nullable() ? "Option<&::core::ffi::CStr>" : "&::core::ffi::CStr"));
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
                    && (!buffers || call.buffer(p) == null)
                    && !CallBorrows.cstring(p)
                    && !(outPointer && p.name().equals(r.handleParameter())));
    if (outPointer) {
      // Status plus out-pointer: Err carries any status outside the success list, with nothing
      // created; the slot is ignored then. A success that wrote no handle is an Err too.
      String status = Types.rust(create.returns(), create.returnsGroup(), "super::");
      String raw = create.returnsGroup() == null ? "status" : "status.0";
      var succeeded = new ArrayList<String>();
      for (long value : r.success()) succeeded.add(raw + " == " + value);
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
          .append(") -> Result<Self, ")
          .append(status)
          .append("> {\n            let mut slot: *mut super::")
          .append(r.name())
          .append(" = core::ptr::null_mut();\n            let status = unsafe { ")
          .append(buffers ? "self::" : "super::")
          .append(name)
          .append('(')
          .append(String.join(", ", arguments))
          .append(") };\n            if !(")
          .append(String.join(" || ", succeeded))
          .append(") {\n                return Err(status);\n            }\n")
          .append(
              "            core::ptr::NonNull::new(slot).map(|pointer| Self { pointer, _thread:"
                  + " core::marker::PhantomData }).ok_or(status)\n"
                  + "        }\n");
      return;
    }
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
