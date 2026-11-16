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
    boolean batches = m.callBorrows().stream().anyMatch(BufferBorrow::batch);
    var out = new StringBuilder(batches ? BATCH_ADAPTER : "");
    out.append(
        "\n"
            + "/// Ownership APIs. Native implementations must uphold the descriptor's"
            + " lifecycle contracts.\n"
            + "pub mod ownership {\n");
    if (batches) out.append(LEND_BATCH);
    for (Resource r : m.ownedResources()) {
      String n = r.name();
      // A resource whose constructor keeps a buffer carries that buffer's lifetime: the borrow
      // checker then keeps the bytes alive, and unmoved, until the resource is dropped.
      boolean holds = holds(m, r);
      String typed = holds ? n + "<'_>" : n;
      out.append("    pub struct ")
          .append(n)
          .append(holds ? "<'held>" : "")
          .append(" {\n")
          .append("        pointer: core::ptr::NonNull<super::")
          .append(n)
          .append(">,\n")
          .append("        _thread: core::marker::PhantomData<*mut ()>,\n")
          .append(
              holds
                  ? "        /// The buffers its constructor lent until the resource is"
                      + " destroyed.\n        _held: core::marker::PhantomData<&'held [u8]>,\n"
                  : "")
          .append("    }\n");
      if (r.shared())
        out.append(
                "    // Explicit shared resource contract includes destruction on any thread.\n"
                    + "    unsafe impl core::marker::Send for ")
            .append(typed)
            .append(" {}\n    unsafe impl core::marker::Sync for ")
            .append(typed)
            .append(" {}\n");
      out.append(holds ? "    impl<'held> " + n + "<'held>" : "    impl " + n).append(" {\n");
      var constructors = new StringBuilder();
      for (String name : r.creates()) constructor(m, r, name, constructors);
      out.append(
          holds
              ? constructors
                  .toString()
                  .replace(
                      "_thread: core::marker::PhantomData }",
                      "_thread: core::marker::PhantomData, _held: core::marker::PhantomData }")
              : constructors);
      out.append(
              "        /// # Safety\n"
                  + "        /// Transfer sole ownership of a live allocation from this resource's"
                  + " allocator.\n"
                  + "        /// No outstanding borrows; obey the descriptor's thread and"
                  + " destruction contracts.\n"
                  + (holds
                      ? "        /// Any buffer the resource holds stays valid and unmodified for"
                          + " `'held`, and the\n"
                          + "        /// owner is dropped, never leaked, before `'held` ends.\n"
                      : ""))
          .append("        pub unsafe fn from_raw(pointer: *mut super::")
          .append(n)
          .append(
              ") -> Option<Self> {\n"
                  + "            core::ptr::NonNull::new(pointer).map(|pointer| Self { pointer,"
                  + " _thread: core::marker::PhantomData"
                  + (holds ? ", _held: core::marker::PhantomData" : "")
                  + " })\n"
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
          .append(typed)
          .append(" {\n        fn drop(&mut self) { unsafe { super::")
          .append(r.destroy())
          .append("(self.pointer.as_ptr()); } }\n    }\n")
          .append("    pub struct ")
          .append(n)
          .append("Borrow<'owner> { owner: &'owner ")
          .append(n)
          .append(holds ? "<'owner>" : "")
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
    // A constructor with held buffers lends its buffers itself, under the same policy.
    for (Resource r : m.ownedResources())
      for (String name : r.creates())
        for (BufferBorrow b : m.borrows())
          if (b.function().equals(name) && !b.batch() && holds(m, r)) {
            CallBorrows call =
                CallBorrows.of(
                    m,
                    m.functions().stream()
                        .filter(f -> f.name().equals(name))
                        .findFirst()
                        .orElseThrow());
            if (call.align(b) > 1) aligns.add(call.align(b));
          }
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
      // A batch wrapper also has a _with_scratch form that takes, after each batch, the scratch
      // its arrays use above the eight elements the stack holds.
      var batchScratch = new LinkedHashMap<String, String>();
      for (Parameter p : f.parameters()) {
        BufferBorrow buffer = call.buffer(p);
        BufferBorrow length = call.length(p);
        BufferBorrow count = call.count(p);
        if (buffer != null && buffer.batch()) {
          params.add(
              p.name() + ": " + (buffer.elementsNullable() ? "&[Option<&[u8]>]" : "&[&[u8]]"));
          args.add(p.name());
          batchScratch.put(
              p.name() + ": ",
              "super::BatchScratch<'_, " + Types.rust(call.lengthElement(buffer)) + ">");
          lends
              .append("batch::lend::<_, ")
              .append(Types.rust(call.lengthElement(buffer)))
              .append(", _>(")
              .append(p.name())
              .append(", batch::")
              .append(buffer.elementsNullable() ? "optional" : "present")
              .append(", \u0001")
              .append(p.name())
              .append("\u0001, |")
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
      int start = out.length();
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
      String header = out.substring(start);
      for (boolean scratch : batchScratch.isEmpty() ? List.of(false) : List.of(false, true)) {
        var signature = new ArrayList<String>();
        for (String param : params) {
          signature.add(param);
          if (!scratch) continue;
          for (var entry : batchScratch.entrySet())
            if (param.startsWith(entry.getKey()))
              signature.add(
                  entry.getKey().substring(0, entry.getKey().length() - 2)
                      + "_scratch: "
                      + entry.getValue());
        }
        String lent = lends.toString();
        for (String key : batchScratch.keySet()) {
          String name = key.substring(0, key.length() - 2);
          lent =
              lent.replace(
                  "\u0001" + name + "\u0001", scratch ? "Some(" + name + "_scratch)" : "None");
        }
        if (scratch)
          out.append(header)
              .append(
                  "    /// As the wrapper without scratch, with each batch's pointer and length"
                      + " arrays in the\n"
                      + "    /// scratch after it, which must hold every element.\n");
        else if (!batchScratch.isEmpty())
          out.append(
              "    /// # Panics\n    /// When a batch has more than eight buffers: pass scratch to"
                  + " the `_with_scratch` form.\n");
        out.append(pointers ? "    pub unsafe fn " : "    pub fn ")
            .append(f.name())
            .append(scratch ? "_with_scratch(" : "(")
            .append(String.join(", ", signature))
            .append(')');
        if (f.returns() != Scalar.VOID)
          out.append(" -> ").append(Types.rust(f.returns(), f.returnsGroup(), "super::"));
        out.append(" {\n")
            .append(absent)
            .append(alignments)
            .append(counts)
            .append("        ")
            .append(lent)
            .append("unsafe { super::")
            .append(f.name())
            .append('(')
            .append(String.join(", ", args))
            .append(") }")
            .append(close)
            .append("\n    }\n");
      }
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
    // A held borrow outlives the call: the constructor lends its buffers itself, held ones for the
    // resource's lifetime.
    if (m.borrows().stream().anyMatch(b -> b.held() && b.function().equals(name))) {
      heldConstructor(m, r, create, out);
      return;
    }
    // A constructor with call buffers or C strings calls the import's wrapper, which lends them.
    boolean buffers =
        m.callBorrows().stream().anyMatch(b -> b.function().equals(name))
            || create.parameters().stream().anyMatch(CallBorrows::cstring);
    CallBorrows call = CallBorrows.of(m, create);
    boolean outPointer = m.outPointer(r);
    // Each parameter the constructor takes, beside what it passes: the out slot is passed without
    // being taken.
    var parameters = new ArrayList<String>();
    var arguments = new ArrayList<String>();
    var batchOf = new ArrayList<BufferBorrow>();
    for (Parameter p : create.parameters()) {
      if (buffers && call.derived(p)) continue;
      // The out slot is the constructor's own local.
      if (outPointer && p.name().equals(r.handleParameter())) {
        parameters.add(null);
        arguments.add("&mut slot");
        batchOf.add(null);
        continue;
      }
      BufferBorrow buffer = buffers ? call.buffer(p) : null;
      arguments.add(p.name());
      batchOf.add(buffer != null && buffer.batch() ? buffer : null);
      if (buffer != null && buffer.batch())
        parameters.add(
            p.name() + ": " + (buffer.elementsNullable() ? "&[Option<&[u8]>]" : "&[&[u8]]"));
      else if (CallBorrows.cstring(p))
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
                    && (!buffers || (call.buffer(p) == null && call.length(p) == null))
                    && !CallBorrows.cstring(p)
                    && !(outPointer && p.name().equals(r.handleParameter())));
    // A constructor over a batch also has a _with_scratch form, as its import's wrapper does: the
    // plain one keeps up to eight elements on the stack and panics above that.
    boolean batched = buffers && call.batched();
    for (boolean scratch : batched ? List.of(false, true) : List.of(false)) {
      var signature = new ArrayList<String>();
      var passed = new ArrayList<String>();
      for (int i = 0; i < arguments.size(); i++) {
        if (parameters.get(i) != null) signature.add(parameters.get(i));
        passed.add(arguments.get(i));
        BufferBorrow b = batchOf.get(i);
        if (scratch && b != null) {
          signature.add(
              arguments.get(i)
                  + "_scratch: super::BatchScratch<'_, "
                  + Types.rust(call.lengthElement(b))
                  + ">");
          passed.add(arguments.get(i) + "_scratch");
        }
      }
      if (batched && !scratch)
        out.append(
            "        /// # Panics\n"
                + "        /// When a batch has more than eight buffers: pass scratch to the"
                + " `_with_scratch` form.\n");
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
          .append(scratch ? "_with_scratch(" : "(")
          .append(String.join(", ", signature));
      if (outPointer) {
        // Status plus out-pointer: Err carries any status outside the success list, with nothing
        // created; the slot is ignored then. A success that wrote no handle is an Err too.
        String status = Types.rust(create.returns(), create.returnsGroup(), "super::");
        String raw = create.returnsGroup() == null ? "status" : "status.0";
        var succeeded = new ArrayList<String>();
        for (long value : r.success()) succeeded.add(raw + " == " + value);
        out.append(") -> Result<Self, ")
            .append(status)
            .append("> {\n            let mut slot: *mut super::")
            .append(r.name())
            .append(" = core::ptr::null_mut();\n            let status = unsafe { ")
            .append(buffers ? "self::" : "super::")
            .append(name)
            .append(scratch ? "_with_scratch(" : "(")
            .append(String.join(", ", passed))
            .append(") };\n            if !(")
            .append(String.join(" || ", succeeded))
            .append(") {\n                return Err(status);\n            }\n")
            .append(
                "            core::ptr::NonNull::new(slot).map(|pointer| Self { pointer, _thread:"
                    + " core::marker::PhantomData }).ok_or(status)\n"
                    + "        }\n");
        continue;
      }
      out.append(") -> Option<Self> {\n            core::ptr::NonNull::new(")
          // The buffer wrapper is this module's function of the same name.
          .append(
              buffers && !call.passesPointers()
                  ? "self::"
                  : "unsafe { " + (buffers ? "self::" : "super::"))
          .append(name)
          .append(scratch ? "_with_scratch(" : "(")
          .append(String.join(", ", passed))
          .append(buffers && !call.passesPointers() ? ")" : ") }")
          .append(
              ").map(|pointer| Self { pointer, _thread: core::marker::PhantomData })\n"
                  + "        }\n");
    }
  }

  /**
   * Whether some constructor of {@code r} keeps a buffer until the resource is destroyed ({@code
   * lifetime=resource:}), which gives the Rust owner a lifetime.
   */
  static boolean holds(Module m, Resource r) {
    return m.borrows().stream().anyMatch(b -> b.held() && r.createdBy(b.function()));
  }

  /**
   * A constructor with held borrows. It lends every buffer itself: a held one as {@code &'held
   * [u8]}, which the resource's lifetime then keeps borrowed, and any call buffer for the call
   * only, under the align policy of every wrapper. Rust does not guarantee that a destructor runs,
   * so the owner-returning constructor is an {@code unsafe fn} that requires the owner to be
   * dropped before {@code 'held} ends; the safe form, {@code with_NAME}, makes the resource, lends
   * it to a closure as {@code &Self}, which can neither forget nor convert it, and destroys it
   * afterwards, on unwind too. A batch has no such form, so a constructor that also takes one gets
   * no wrapper.
   */
  private static void heldConstructor(Module m, Resource r, Function create, StringBuilder out) {
    CallBorrows call = CallBorrows.of(m, create);
    if (call.batched()) return;
    boolean outPointer = m.outPointer(r);
    var parameters = new ArrayList<String>();
    var names = new ArrayList<String>();
    var arguments = new ArrayList<String>();
    var lets = new StringBuilder();
    var alignments = new StringBuilder();
    boolean pointers = false;
    for (Parameter p : create.parameters()) {
      BufferBorrow buffer = call.buffer(p);
      BufferBorrow length = call.length(p);
      if (outPointer && p.name().equals(r.handleParameter())) {
        arguments.add("&mut slot");
      } else if (buffer != null) {
        boolean shared = p.access() == Access.READ;
        String pointer = shared ? ".as_ptr()" : ".as_mut_ptr()";
        String slice = (shared ? "&" : "&mut ") + (buffer.held() ? "'held " : "") + "[u8]";
        long align = CallBorrows.align(p);
        names.add(p.name());
        if (align > 1) {
          String at = call.nullable(buffer) ? p.name() + ".0" : p.name() + pointer;
          String len = call.nullable(buffer) ? p.name() + ".1" : p.name() + ".len()";
          alignments
              .append("            debug_assert!(")
              .append(len)
              .append(" == 0 || (")
              .append(at)
              .append(" as usize) & ")
              .append(align - 1)
              .append(" == 0, \"")
              .append(create.name())
              .append('.')
              .append(p.name())
              .append(" requires ")
              .append(align)
              .append("-byte alignment\");\n");
        }
        if (call.nullable(buffer)) {
          parameters.add(p.name() + ": Option<" + slice + ">");
          lets.append("            let ")
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
          arguments.add(p.name() + ".0");
        } else {
          parameters.add(p.name() + ": " + slice);
          arguments.add(lent(p.name(), pointer, align, shared));
        }
      } else if (length != null) {
        arguments.add(
            length.parameter()
                + (call.nullable(length) ? ".1" : ".len()")
                + ".try_into().expect(\"buffer length exceeds ABI range\")");
      } else if (CallBorrows.cstring(p)) {
        names.add(p.name());
        parameters.add(
            p.name() + ": " + (p.nullable() ? "Option<&::core::ffi::CStr>" : "&::core::ffi::CStr"));
        arguments.add(
            p.nullable()
                ? p.name() + ".map_or(core::ptr::null(), |text| text.as_ptr().cast())"
                : p.name() + ".as_ptr().cast()");
      } else {
        names.add(p.name());
        parameters.add(p.name() + ": " + Types.rust(p, "super::"));
        arguments.add(p.name());
        pointers |= p.type() instanceof Pointer;
      }
    }
    String wrapper = r.wrapperName(create.name());
    String pointerSafety =
        pointers
            ? "        /// Every pointer argument other than a borrowed buffer must satisfy the raw"
                + " function's requirements, for as long\n"
                + "        /// as the created resource depends on it.\n"
            : "";
    out.append("        /// # Safety\n")
        .append(
            "        /// The owner must be dropped, never leaked (`mem::forget`, or `into_raw`"
                + " without a\n"
                + "        /// matching `from_raw`), before `'held` ends: the native side may"
                + " read the held\n"
                + "        /// buffers until its destroy returns. `with_")
        .append(wrapper)
        .append("` guarantees it.\n")
        .append(pointerSafety)
        .append("        pub unsafe fn ")
        .append(wrapper)
        .append('(')
        .append(String.join(", ", parameters));
    String invoke = "unsafe { super::" + create.name() + "(" + String.join(", ", arguments) + ") }";
    String status = null;
    if (outPointer) {
      status = Types.rust(create.returns(), create.returnsGroup(), "super::");
      String raw = create.returnsGroup() == null ? "status" : "status.0";
      var succeeded = new ArrayList<String>();
      for (long value : r.success()) succeeded.add(raw + " == " + value);
      out.append(") -> Result<Self, ")
          .append(status)
          .append("> {\n")
          .append(lets)
          .append(alignments)
          .append("            let mut slot: *mut super::")
          .append(r.name())
          .append(" = core::ptr::null_mut();\n            let status = ")
          .append(invoke)
          .append(";\n            if !(")
          .append(String.join(" || ", succeeded))
          .append(") {\n                return Err(status);\n            }\n")
          .append(
              "            core::ptr::NonNull::new(slot).map(|pointer| Self { pointer, _thread:"
                  + " core::marker::PhantomData }).ok_or(status)\n"
                  + "        }\n");
    } else
      out.append(") -> Option<Self> {\n")
          .append(lets)
          .append(alignments)
          .append("            core::ptr::NonNull::new(")
          .append(invoke)
          .append(
              ").map(|pointer| Self { pointer, _thread: core::marker::PhantomData })\n"
                  + "        }\n");
    // The safe form: the closure sees the resource only by reference, and it is destroyed after.
    var scoped = new ArrayList<>(parameters);
    scoped.add("scope: impl FnOnce(&Self) -> T");
    out.append(
            "        /// Makes the resource, lends it to `scope`, and destroys it afterwards, on"
                + " unwind too.\n")
        .append(
            pointers
                ? "        /// # Safety\n" + pointerSafety + "        pub unsafe fn with_"
                : "        pub fn with_")
        .append(wrapper)
        .append("<T>(")
        .append(String.join(", ", scoped))
        .append(") -> ")
        .append(outPointer ? "Result<T, " + status + ">" : "Option<T>")
        .append(" {\n            let owner = unsafe { Self::")
        .append(wrapper)
        .append('(')
        .append(String.join(", ", names))
        .append(") }?;\n            ")
        .append(outPointer ? "Ok" : "Some")
        .append("(scope(&owner))\n        }\n");
  }

  /**
   * The implementation side of a batch, and the scratch a caller lends for a large one: emitted
   * once, at the top level of {@code seam.rs}, when a module declares a batch.
   */
  private static final String BATCH_ADAPTER =
"""
/// A length or count of a borrowed batch, as its import declares it.
///
/// Sealed: only the carriers a batch's lengths and count may have (`u32`, `u64`, `i32`, `i64`)
/// implement it, so `to_usize` is never code that could race when a view crosses threads.
pub trait BatchLength: Copy + ownership::batch::sealed::Sealed {
    /// The value as a byte length or element count. Negative and oversized values break the
    /// caller's contract, which debug builds assert.
    fn to_usize(self) -> usize;
}
impl ownership::batch::sealed::Sealed for u32 {}
impl ownership::batch::sealed::Sealed for i32 {}
impl ownership::batch::sealed::Sealed for u64 {}
impl ownership::batch::sealed::Sealed for i64 {}
impl BatchLength for u32 { #[inline(always)] fn to_usize(self) -> usize { self as usize } }
impl BatchLength for i32 {
    #[inline(always)]
    fn to_usize(self) -> usize { debug_assert!(self >= 0, "negative batch length"); self as usize }
}
impl BatchLength for u64 {
    #[inline(always)]
    fn to_usize(self) -> usize { debug_assert!(self <= isize::MAX as u64, "batch length exceeds isize::MAX"); self as usize }
}
impl BatchLength for i64 {
    #[inline(always)]
    fn to_usize(self) -> usize { debug_assert!(self >= 0 && self as u64 <= isize::MAX as u64, "invalid batch length"); self as usize }
}

/// Caller-provided room for a batch's pointer and length arrays, for the `_with_scratch` wrappers
/// of batches above the eight elements a wrapper keeps on its stack. Both slices must hold at
/// least as many elements as the batch.
pub struct BatchScratch<'s, L = u64> {
    pointers: &'s mut [core::mem::MaybeUninit<*mut u8>],
    lengths: &'s mut [core::mem::MaybeUninit<L>],
}
impl<'s, L> BatchScratch<'s, L> {
    #[inline(always)]
    pub fn new(pointers: &'s mut [core::mem::MaybeUninit<*mut u8>], lengths: &'s mut [core::mem::MaybeUninit<L>]) -> Self {
        Self { pointers, lengths }
    }
    /// The most elements the scratch holds.
    #[inline(always)]
    pub fn capacity(&self) -> usize { core::cmp::min(self.pointers.len(), self.lengths.len()) }
}

/// The buffers of a borrowed batch, as an implementation receives them: `sources[i]` holds
/// `lengths[i]` bytes for `i < count`. A `Copy` view; `iter()` yields each buffer in order.
#[derive(Clone, Copy)]
pub struct BufferBatch<'a, L: BatchLength = u64> {
    sources: *const *mut u8,
    lengths: *const L,
    count: usize,
    _buffers: core::marker::PhantomData<&'a [u8]>,
}
// The view reads immutable bytes, as the `&[&[u8]]` it stands for may be read from any thread.
unsafe impl<L: BatchLength> Send for BufferBatch<'_, L> {}
unsafe impl<L: BatchLength> Sync for BufferBatch<'_, L> {}
impl<'a, L: BatchLength> BufferBatch<'a, L> {
    /// # Safety
    /// - Unless `count` is zero, `sources` and `lengths` are non-null and valid for `count` reads,
    ///   and both arrays stay valid and unmodified for `'a`; with a zero count neither is read.
    /// - Every length is non-negative and at most `isize::MAX`.
    /// - Each `sources[i]` is valid for reads of `lengths[i]` bytes; it may be null only when its
    ///   length is zero.
    /// - The bytes are not mutated for `'a`.
    #[inline(always)]
    pub unsafe fn from_raw<C: BatchLength>(sources: *const *mut u8, lengths: *const L, count: C) -> Self {
        let count = count.to_usize();
        debug_assert!(count == 0 || (!sources.is_null() && !lengths.is_null()), "null batch arrays");
        Self { sources, lengths, count, _buffers: core::marker::PhantomData }
    }
    /// The number of elements.
    #[inline(always)]
    pub fn len(&self) -> usize { self.count }
    /// Whether the batch has no elements.
    #[inline(always)]
    pub fn is_empty(&self) -> bool { self.count == 0 }
    /// The element at `index`, counted from the start, or `None` past the end.
    #[inline(always)]
    pub fn get(&self, index: usize) -> Option<&'a [u8]> {
        if index >= self.count { return None; }
        let (pointer, length) = unsafe { (self.sources.add(index).read(), self.lengths.add(index).read().to_usize()) };
        debug_assert!(!pointer.is_null() || length == 0, "null buffer with a nonzero length");
        Some(if length == 0 { &[] } else { unsafe { core::slice::from_raw_parts(pointer, length) } })
    }
    /// The elements in order.
    #[inline(always)]
    pub fn iter(&self) -> BufferBatchIter<'a, L> { BufferBatchIter { batch: *self, next: 0 } }
}
impl<'a, L: BatchLength> IntoIterator for BufferBatch<'a, L> {
    type Item = &'a [u8];
    type IntoIter = BufferBatchIter<'a, L>;
    #[inline(always)]
    fn into_iter(self) -> BufferBatchIter<'a, L> { BufferBatchIter { batch: self, next: 0 } }
}
/// The elements of a [`BufferBatch`], in order.
#[derive(Clone)]
pub struct BufferBatchIter<'a, L: BatchLength = u64> { batch: BufferBatch<'a, L>, next: usize }
impl<'a, L: BatchLength> Iterator for BufferBatchIter<'a, L> {
    type Item = &'a [u8];
    #[inline(always)]
    fn next(&mut self) -> Option<&'a [u8]> {
        let item = self.batch.get(self.next)?;
        self.next += 1;
        Some(item)
    }
    #[inline(always)]
    fn nth(&mut self, n: usize) -> Option<&'a [u8]> {
        self.next = self.next.saturating_add(n).min(self.batch.count);
        self.next()
    }
    #[inline(always)]
    fn size_hint(&self) -> (usize, Option<usize>) { let left = self.batch.count - self.next; (left, Some(left)) }
}
impl<L: BatchLength> ExactSizeIterator for BufferBatchIter<'_, L> {}
impl<L: BatchLength> core::iter::FusedIterator for BufferBatchIter<'_, L> {}

/// The buffers of a borrowed batch whose elements may be absent (`elements_nullable=true`): a null
/// element, which has length zero, is `None`.
#[derive(Clone, Copy)]
pub struct OptionalBufferBatch<'a, L: BatchLength = u64> {
    sources: *const *mut u8,
    lengths: *const L,
    count: usize,
    _buffers: core::marker::PhantomData<&'a [u8]>,
}
// The view reads immutable bytes, as the `&[&[u8]]` it stands for may be read from any thread.
unsafe impl<L: BatchLength> Send for OptionalBufferBatch<'_, L> {}
unsafe impl<L: BatchLength> Sync for OptionalBufferBatch<'_, L> {}
impl<'a, L: BatchLength> OptionalBufferBatch<'a, L> {
    /// # Safety
    /// - Unless `count` is zero, `sources` and `lengths` are non-null and valid for `count` reads,
    ///   and both arrays stay valid and unmodified for `'a`; with a zero count neither is read.
    /// - Every length is non-negative and at most `isize::MAX`.
    /// - Each non-null `sources[i]` is valid for reads of `lengths[i]` bytes; a null one has length
    ///   zero.
    /// - The bytes are not mutated for `'a`.
    #[inline(always)]
    pub unsafe fn from_raw<C: BatchLength>(sources: *const *mut u8, lengths: *const L, count: C) -> Self {
        let count = count.to_usize();
        debug_assert!(count == 0 || (!sources.is_null() && !lengths.is_null()), "null batch arrays");
        Self { sources, lengths, count, _buffers: core::marker::PhantomData }
    }
    /// The number of elements.
    #[inline(always)]
    pub fn len(&self) -> usize { self.count }
    /// Whether the batch has no elements.
    #[inline(always)]
    pub fn is_empty(&self) -> bool { self.count == 0 }
    /// The element at `index`, counted from the start, or `None` past the end.
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
    /// The elements in order.
    #[inline(always)]
    pub fn iter(&self) -> OptionalBufferBatchIter<'a, L> { OptionalBufferBatchIter { batch: *self, next: 0 } }
}
impl<'a, L: BatchLength> IntoIterator for OptionalBufferBatch<'a, L> {
    type Item = Option<&'a [u8]>;
    type IntoIter = OptionalBufferBatchIter<'a, L>;
    #[inline(always)]
    fn into_iter(self) -> OptionalBufferBatchIter<'a, L> { OptionalBufferBatchIter { batch: self, next: 0 } }
}
/// The elements of a [`OptionalBufferBatch`], in order.
#[derive(Clone)]
pub struct OptionalBufferBatchIter<'a, L: BatchLength = u64> { batch: OptionalBufferBatch<'a, L>, next: usize }
impl<'a, L: BatchLength> Iterator for OptionalBufferBatchIter<'a, L> {
    type Item = Option<&'a [u8]>;
    #[inline(always)]
    fn next(&mut self) -> Option<Option<&'a [u8]>> {
        let item = self.batch.get(self.next)?;
        self.next += 1;
        Some(item)
    }
    #[inline(always)]
    fn nth(&mut self, n: usize) -> Option<Option<&'a [u8]>> {
        self.next = self.next.saturating_add(n).min(self.batch.count);
        self.next()
    }
    #[inline(always)]
    fn size_hint(&self) -> (usize, Option<usize>) { let left = self.batch.count - self.next; (left, Some(left)) }
}
impl<L: BatchLength> ExactSizeIterator for OptionalBufferBatchIter<'_, L> {}
impl<L: BatchLength> core::iter::FusedIterator for OptionalBufferBatchIter<'_, L> {}
""";

  /**
   * The caller side of a batch, in a hidden submodule so that no declared name collides with its
   * helpers. It allocates nothing, so {@code seam.rs} stays usable without an allocator.
   */
  private static final String LEND_BATCH =
"""
    /// Lends batches to the calls above; not part of the API.
    #[doc(hidden)]
    pub mod batch {
        /// Seals `BatchLength`: nameable from `seam.rs` only, so no other type can implement it.
        pub(in super::super) mod sealed {
            pub trait Sealed {}
        }
        #[inline(always)]
        pub fn present(buffer: &&[u8]) -> (*mut u8, usize) { (buffer.as_ptr().cast_mut(), buffer.len()) }
        #[inline(always)]
        pub fn optional(buffer: &Option<&[u8]>) -> (*mut u8, usize) {
            buffer.map_or((core::ptr::null_mut(), 0), |buffer| (buffer.as_ptr().cast_mut(), buffer.len()))
        }
        /// The pointer and length arrays of one batch, alive for exactly the closure that makes the
        /// call: on the stack for up to eight buffers, otherwise in the caller's scratch. An empty
        /// batch passes null arrays. Nothing is allocated.
        #[inline(always)]
        pub fn lend<B, L: Copy + Default + TryFrom<usize>, R>(
            buffers: &[B],
            lend: fn(&B) -> (*mut u8, usize),
            scratch: Option<super::super::BatchScratch<'_, L>>,
            call: impl FnOnce(*const *mut u8, *const L) -> R,
        ) -> R {
            if buffers.is_empty() { return call(core::ptr::null(), core::ptr::null()); }
            let length = |bytes: usize| L::try_from(bytes).ok().expect("buffer length exceeds ABI range");
            if let Some(scratch) = scratch {
                assert!(scratch.capacity() >= buffers.len(), "batch scratch holds fewer elements than the batch");
                for (i, buffer) in buffers.iter().enumerate() {
                    let (pointer, bytes) = lend(buffer);
                    scratch.pointers[i].write(pointer);
                    scratch.lengths[i].write(length(bytes));
                }
                return call(scratch.pointers.as_ptr().cast(), scratch.lengths.as_ptr().cast());
            }
            assert!(buffers.len() <= 8, "a batch of more than eight buffers needs scratch: use the _with_scratch wrapper");
            let mut pointers = [core::ptr::null_mut::<u8>(); 8];
            let mut lengths = [L::default(); 8];
            for (i, buffer) in buffers.iter().enumerate() {
                let (pointer, bytes) = lend(buffer);
                pointers[i] = pointer;
                lengths[i] = length(bytes);
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
