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
                    + "    unsafe impl Send for ")
            .append(n)
            .append(" {}\n    unsafe impl Sync for ")
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
          .append("    impl Drop for ")
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
    for (BufferBorrow b : m.borrows()) {
      Function f =
          m.functions().stream()
              .filter(x -> x.name().equals(b.function()))
              .findFirst()
              .orElseThrow();
      Parameter buffer =
          f.parameters().stream()
              .filter(p -> p.name().equals(b.parameter()))
              .findFirst()
              .orElseThrow();
      var params = new ArrayList<String>();
      var args = new ArrayList<String>();
      for (Parameter p : f.parameters()) {
        if (p == buffer) {
          params.add(p.name() + (p.access() == Access.READ ? ": &[u8]" : ": &mut [u8]"));
          args.add(p.name() + (p.access() == Access.READ ? ".as_ptr()" : ".as_mut_ptr()"));
        } else if (p.name().equals(b.length()))
          args.add(buffer.name() + ".len().try_into().expect(\"buffer length exceeds ABI range\")");
        else {
          params.add(p.name() + ": " + Types.rust(p));
          args.add(p.name());
        }
      }
      out.append("    pub fn ")
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
