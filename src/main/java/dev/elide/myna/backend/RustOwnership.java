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
    if (m.resources().isEmpty() && m.borrows().isEmpty()) return "";
    var out =
        new StringBuilder(
            "\n"
                + "/// Ownership APIs. Native implementations must uphold the descriptor's"
                + " lifecycle contracts.\n"
                + "pub mod ownership {\n");
    for (Resource r : m.resources()) {
      String n = r.name();
      Function create =
          m.functions().stream().filter(f -> f.name().equals(r.create())).findFirst().orElseThrow();
      String parameters =
          String.join(
              ", ",
              create.parameters().stream().map(p -> p.name() + ": " + Types.rust(p)).toList());
      String arguments =
          String.join(", ", create.parameters().stream().map(Parameter::name).toList());
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
      out.append("    impl ")
          .append(n)
          .append(" {\n")
          .append("        pub fn create(")
          .append(parameters)
          .append(") -> Option<Self> {\n            core::ptr::NonNull::new(unsafe { super::")
          .append(r.create())
          .append('(')
          .append(arguments)
          .append(
              ") }).map(|pointer| Self { pointer, _thread: core::marker::PhantomData })\n"
                  + "        }\n")
          .append(
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
    for (CallBorrows call : CallBorrows.of(m)) {
      Function f = call.function();
      var params = new ArrayList<String>();
      var args = new ArrayList<String>();
      for (Parameter p : f.parameters()) {
        BufferBorrow length = call.length(p);
        if (call.buffer(p) != null) {
          params.add(p.name() + (p.access() == Access.READ ? ": &[u8]" : ": &mut [u8]"));
          args.add(p.name() + (p.access() == Access.READ ? ".as_ptr()" : ".as_mut_ptr()"));
        } else if (length != null)
          args.add(
              length.parameter() + ".len().try_into().expect(\"buffer length exceeds ABI range\")");
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
      out.append(pointers ? "    pub unsafe fn " : "    pub fn ")
          .append(f.name())
          .append('(')
          .append(String.join(", ", params))
          .append(')');
      if (f.returns() != Scalar.VOID) out.append(" -> ").append(Types.rust(f.returns()));
      out.append(" {\n        unsafe { super::")
          .append(f.name())
          .append('(')
          .append(String.join(", ", args))
          .append(") }\n    }\n");
    }
    return out.append("}\n").toString();
  }
}
