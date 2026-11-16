// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.backend;

import dev.elide.myna.model.Seam.*;
import dev.elide.myna.model.Seam.Module;
import java.util.*;

/**
 * The implementer side of a module's imports in Rust, with {@code rust_implementation=trait}: a
 * safe trait over the declared contracts and a macro that defines each {@code extern "C"} entry
 * point from a type implementing it. Call-borrowed buffers arrive as slices, a returned resource as
 * an associated type the implementation owns through {@code Box}, and everything else as the raw
 * binding declares it.
 */
final class RustImplementation {
  private RustImplementation() {}

  /** Name of the support module the entry points call; a hidden item of the including module. */
  static final String SUPPORT = "myna_implementation";

  /** How one ABI parameter reaches the trait. */
  private enum Kind {
    PLAIN,
    BUFFER,
    LENGTH,
    CSTRING,
    DESTROYED,
    RESOURCE,
    /** A constructor's ptr<ptr<R>> out-parameter: the entry point writes the box into it. */
    OUT,
    /** A batch's sources array: a BufferBatch view over it, its lengths and its count. */
    BATCH,
    /** A batch's count, which its view carries. */
    COUNT
  }

  /** One import's projection: what each parameter becomes, and the resource it constructs. */
  private record Shape(
      Function function,
      CallBorrows borrows,
      Map<String, Kind> kinds,
      Resource creates,
      boolean outPointer) {
    Kind kind(Parameter p) {
      return kinds.get(p.name());
    }

    /** The returned resource a {@code ptr<R>} parameter names, or null. */
    String resource(Parameter p) {
      return ((Named) ((Pointer) p.type()).pointee()).name();
    }
  }

  private static Shape shape(Module m, Function f) {
    var returned = new HashMap<String, Resource>();
    m.ownedResources().forEach(r -> returned.put(r.name(), r));
    boolean callBorrows = m.callBorrows().stream().anyMatch(b -> b.function().equals(f.name()));
    CallBorrows borrows = callBorrows ? CallBorrows.of(m, f) : new CallBorrows(f, List.of());
    Resource destroys =
        m.ownedResources().stream()
            .filter(r -> r.destroy().equals(f.name()))
            .findFirst()
            .orElse(null);
    Resource creates =
        m.ownedResources().stream().filter(r -> r.createdBy(f.name())).findFirst().orElse(null);
    boolean outPointer = creates != null && m.outPointer(creates);
    var kinds = new HashMap<String, Kind>();
    for (Parameter p : f.abiParameters()) {
      Kind kind = Kind.PLAIN;
      if (outPointer && p.name().equals(creates.handleParameter())) kind = Kind.OUT;
      else if (borrows.buffer(p) != null && borrows.buffer(p).batch()) kind = Kind.BATCH;
      else if (borrows.count(p) != null) kind = Kind.COUNT;
      else if (borrows.buffer(p) != null) kind = Kind.BUFFER;
      else if (CallBorrows.cstring(p)) kind = Kind.CSTRING;
      else if (borrows.length(p) != null) kind = Kind.LENGTH;
      else if (p.type() instanceof Pointer pointer
          && pointer.pointee() instanceof Named named
          && returned.containsKey(named.name())) {
        if (destroys != null) kind = Kind.DESTROYED;
        else if (p.ownership() == Ownership.BORROWED) kind = Kind.RESOURCE;
      }
      kinds.put(p.name(), kind);
    }
    return new Shape(f, borrows, kinds, creates, outPointer);
  }

  private static List<Function> imports(Module m) {
    return m.functions().stream().filter(f -> f.direction() == Direction.IMPORT).toList();
  }

  static String generate(Module m) {
    var imports = imports(m);
    if (!m.rustImplementation() || imports.isEmpty()) return "";
    String trait = m.rustImplementationTrait();
    var out = new StringBuilder();
    out.append(
            "\n"
                + "/// Implements this module's imports over their declared contracts:"
                + " call-borrowed buffers are\n"
                + "/// slices, a returned resource is an associated type owned through `Box`, and"
                + " every other\n"
                + "/// parameter is as the raw binding declares it. `define_implementations!`"
                + " defines the\n"
                + "/// `extern \"C\"` entry points from an implementing type.\n"
                + "#[allow(non_snake_case)]\n"
                + "pub trait ")
        .append(trait)
        .append(" {\n");
    // A shared resource may be used and destroyed from any thread (threading=shared), which only
    // a Send + Sync type survives; a confined one stays on its creating thread.
    for (Resource r : m.ownedResources())
      out.append("    /// What a `")
          .append(r.name())
          .append("` handle points to: the implementation's own type, never read by callers.\n")
          .append("    type ")
          .append(r.name())
          .append(r.shared() ? ": Send + Sync" : "")
          .append(";\n");
    for (Function f : imports) {
      Shape shape = shape(m, f);
      var params = new ArrayList<String>();
      for (Parameter p : f.abiParameters()) {
        String type = traitType(shape, p);
        if (type != null) params.add(p.name() + ": " + type);
      }
      out.append("    fn ")
          .append(f.name())
          .append('(')
          .append(String.join(", ", params))
          .append(')');
      if (shape.outPointer())
        out.append(" -> Result<Box<Self::")
            .append(shape.creates().name())
            .append(">, ")
            .append(Types.rust(f.returns(), f.returnsGroup()))
            .append('>');
      else if (shape.creates() != null && shape.creates().handle() == null)
        out.append(" -> Option<Box<Self::").append(shape.creates().name()).append(">>");
      else if (f.returns() != Scalar.VOID)
        out.append(" -> ").append(Types.rust(f.returns(), f.returnsGroup()));
      out.append(";\n");
    }
    out.append("}\n");
    support(out);
    out.append("\n/// Defines every import's `extern \"C\"` entry point from a type implementing `")
        .append(trait)
        .append(
            "`.\n/// Pass this module's path after the type when its items are not in scope.\n"
                + "#[allow(unused_macros)]\nmacro_rules! define_implementations {\n"
                + "    ($implementation:ty) => {\n");
    for (Function f : imports) entry(shape(m, f), trait, "", out);
    out.append("    };\n    ($implementation:ty, $($seam:ident)::+) => {\n");
    for (Function f : imports) entry(shape(m, f), trait, "$($seam)::+::", out);
    out.append("    };\n}\n#[allow(unused_imports)]\npub(crate) use define_implementations;\n");
    return out.toString();
  }

  /** The trait's type for one parameter, or null for a length its buffer's slice carries. */
  private static String traitType(Shape shape, Parameter p) {
    return switch (shape.kind(p)) {
      case LENGTH, OUT, COUNT -> null;
      case BATCH -> {
        BufferBorrow b = shape.borrows().buffer(p);
        yield (b.elementsNullable() ? "OptionalBufferBatch<'_, " : "BufferBatch<'_, ")
            + Types.rust(shape.borrows().lengthElement(b))
            + ">";
      }
      case CSTRING -> p.nullable() ? "Option<&::core::ffi::CStr>" : "&::core::ffi::CStr";
      case BUFFER -> {
        String slice = p.access() == Access.READ ? "&[u8]" : "&mut [u8]";
        yield p.nullable() ? "Option<" + slice + ">" : slice;
      }
      case DESTROYED -> {
        String owned = "Box<Self::" + shape.resource(p) + ">";
        yield p.nullable() ? "Option<" + owned + ">" : owned;
      }
      case RESOURCE -> {
        String borrowed = "&Self::" + shape.resource(p);
        yield p.nullable() ? "Option<" + borrowed + ">" : borrowed;
      }
      case PLAIN -> Types.rust(p);
    };
  }

  /**
   * Slices from pointer and length, each checked once. A null pointer for a buffer declared {@code
   * nullable=false}, an absent buffer with a nonzero length, and a 64-bit length that is negative
   * or past {@code isize::MAX} break the caller's contract: debug builds assert, and release builds
   * read an empty slice rather than build one from null or past the address space.
   */
  private static void support(StringBuilder out) {
    out.append("\n#[doc(hidden)]\n#[allow(dead_code)]\npub mod ")
        .append(SUPPORT)
        .append(
            """
             {
                #[inline(always)]
                pub fn signed(length: i64) -> usize {
                    // A u64 length past isize::MAX arrives here negative: both read as empty.
                    debug_assert!(length >= 0, "buffer length negative or past isize::MAX");
                    if length < 0 { 0 } else { length as usize }
                }
                #[inline(always)]
                pub unsafe fn slice<'a>(pointer: *const u8, length: usize) -> &'a [u8] {
                    if pointer.is_null() {
                        debug_assert!(false, "null buffer declared nullable=false");
                        return &[];
                    }
                    debug_assert!(length <= isize::MAX as usize, "buffer length overflows isize");
                    // SAFETY: the caller lends `length` readable bytes at `pointer` for the call.
                    unsafe { ::core::slice::from_raw_parts(pointer, length) }
                }
                #[inline(always)]
                pub unsafe fn slice_mut<'a>(pointer: *mut u8, length: usize) -> &'a mut [u8] {
                    if pointer.is_null() {
                        debug_assert!(false, "null buffer declared nullable=false");
                        return &mut [];
                    }
                    debug_assert!(length <= isize::MAX as usize, "buffer length overflows isize");
                    // SAFETY: the caller lends `length` exclusive bytes at `pointer` for the call.
                    unsafe { ::core::slice::from_raw_parts_mut(pointer, length) }
                }
                #[inline(always)]
                pub unsafe fn optional<'a>(pointer: *const u8, length: usize) -> Option<&'a [u8]> {
                    if pointer.is_null() {
                        debug_assert!(length == 0, "absent buffer with a nonzero length");
                        return None;
                    }
                    Some(unsafe { slice(pointer, length) })
                }
                #[inline(always)]
                pub unsafe fn optional_mut<'a>(pointer: *mut u8, length: usize) -> Option<&'a mut [u8]> {
                    if pointer.is_null() {
                        debug_assert!(length == 0, "absent buffer with a nonzero length");
                        return None;
                    }
                    Some(unsafe { slice_mut(pointer, length) })
                }
            }
            """);
  }

  /**
   * One entry point: converts each argument once, calls the implementation, converts back. Rust
   * ignores {@code #[inline]} on an exported function (and warns), so the entry point carries none;
   * the conversions it calls are {@code #[inline(always)]} and the implementation is a static call.
   */
  private static void entry(Shape shape, String trait, String q, StringBuilder out) {
    Function f = shape.function();
    String implementation = "<$implementation as " + q + trait + ">";
    String pad = "            ";
    var body = new StringBuilder();
    var args = new ArrayList<String>();
    String early = null;
    for (Parameter p : f.abiParameters()) {
      switch (shape.kind(p)) {
        case LENGTH, OUT, COUNT -> {}
        case BATCH -> {
          // The view checks nothing more than a raw caller's contract allows: see from_raw.
          BufferBorrow b = shape.borrows().buffer(p);
          body.append(pad)
              .append("let ")
              .append(p.name())
              .append(" = unsafe { ")
              .append(q)
              .append(b.elementsNullable() ? "OptionalBufferBatch" : "BufferBatch")
              .append("::from_raw(")
              .append(p.name())
              .append(", ")
              .append(b.length())
              .append(", ")
              .append(b.count())
              .append(") };\n");
          args.add(p.name());
        }
        case CSTRING -> {
          String text = "unsafe { ::core::ffi::CStr::from_ptr(" + p.name() + ".cast()) }";
          if (p.nullable())
            args.add("if " + p.name() + ".is_null() { None } else { Some(" + text + ") }");
          else {
            // A CStr from null is undefined, and no result is safe to make up.
            body.append(pad)
                .append("if ")
                .append(p.name())
                .append(".is_null() { ::core::panic!(\"")
                .append(f.name())
                .append(": null ")
                .append(p.name())
                .append("\") }\n");
            args.add(text);
          }
        }
        case PLAIN -> args.add(p.name());
        case BUFFER -> {
          BufferBorrow b = shape.borrows().buffer(p);
          Parameter length =
              f.abiParameters().stream()
                  .filter(x -> x.name().equals(b.length()))
                  .findFirst()
                  .orElseThrow();
          // A 64-bit length past isize::MAX would be undefined in a slice: it is negative as an
          // i64, and reads as empty as a negative one does. A u32 always fits.
          String count =
              length.type() == Scalar.U32
                  ? length.name() + " as usize"
                  : q + SUPPORT + "::signed(" + length.name() + " as i64)";
          boolean writable = p.access() != Access.READ;
          String helper = (p.nullable() ? "optional" : "slice") + (writable ? "_mut" : "");
          body.append(pad)
              .append("let ")
              .append(p.name())
              .append(" = unsafe { ")
              .append(q)
              .append(SUPPORT)
              .append("::")
              .append(helper)
              .append('(')
              .append(p.name())
              .append(", ")
              .append(count)
              .append(") };\n");
          args.add(p.name());
        }
        case DESTROYED -> {
          String owned = "unsafe { Box::from_raw(" + p.name() + ".cast()) }";
          if (p.nullable())
            args.add("if " + p.name() + ".is_null() { None } else { Some(" + owned + ") }");
          else {
            // Box::from_raw(null) is undefined: a null the declaration rules out is a no-op.
            early =
                "if "
                    + p.name()
                    + ".is_null() {\n"
                    + pad
                    + "    ::core::debug_assert!(false, \""
                    + f.name()
                    + ": null "
                    + shape.resource(p)
                    + "\");\n"
                    + pad
                    + "    return;\n"
                    + pad
                    + "}\n";
            args.add(owned);
          }
        }
        case RESOURCE -> {
          String target = implementation + "::" + shape.resource(p);
          String reference = "unsafe { &*" + p.name() + ".cast::<" + target + ">() }";
          if (p.nullable())
            args.add("if " + p.name() + ".is_null() { None } else { Some(" + reference + ") }");
          else {
            // A reference from null is undefined, and no result is safe to make up: a panic in an
            // extern "C" function aborts, in every build.
            body.append(pad)
                .append("if ")
                .append(p.name())
                .append(".is_null() { ::core::panic!(\"")
                .append(f.name())
                .append(": null ")
                .append(shape.resource(p))
                .append("\") }\n");
            args.add(reference);
          }
        }
      }
    }
    out.append("        #[unsafe(no_mangle)]\n")
        .append("        #[allow(non_snake_case)]\n")
        .append("        pub unsafe extern \"C\" fn ")
        .append(f.symbol())
        .append('(')
        .append(
            String.join(
                ", ",
                f.abiParameters().stream().map(p -> p.name() + ": " + Types.rust(p, q)).toList()))
        .append(')');
    if (f.returns() != Scalar.VOID)
      out.append(" -> ").append(Types.rust(f.returns(), f.returnsGroup(), q));
    out.append(" {\n");
    if (early != null) out.append(pad).append(early);
    out.append(body);
    String call = implementation + "::" + f.name() + "(" + String.join(", ", args) + ")";
    if (shape.outPointer()) {
      // Ok writes the box into the slot (or drops it when the slot is null, which only a nullable
      // parameter allows) and returns the first success status; Err returns its own status.
      String slot = shape.creates().handleParameter();
      String group = f.returnsGroup() == null ? null : q + f.returnsGroup().name();
      long first = shape.creates().success().getFirst();
      String success = group == null ? String.valueOf(first) : group + "(" + first + ")";
      var succeeded = new ArrayList<String>();
      for (long value : shape.creates().success())
        succeeded.add((group == null ? "status" : "status.0") + " == " + value);
      out.append(pad)
          .append("match ")
          .append(call)
          .append(" {\n")
          .append(pad)
          // A name no parameter takes: the slot is read inside the arm.
          .append("    Ok(myna_owned) => {\n")
          .append(pad)
          .append("        if ")
          .append(slot)
          .append(".is_null() {\n")
          .append(pad)
          .append("            drop(myna_owned);\n")
          .append(pad)
          .append("        } else {\n")
          .append(pad)
          .append("            unsafe { ")
          .append(slot)
          .append(".write(Box::into_raw(myna_owned).cast()) };\n")
          .append(pad)
          .append("        }\n")
          .append(pad)
          .append("        ")
          .append(success)
          .append('\n')
          .append(pad)
          .append("    }\n")
          .append(pad)
          .append("    Err(status) => {\n")
          .append(pad)
          // A success status without a handle in the slot cannot be represented safely: the caller
          // would own whatever the slot held. Abort in every build.
          .append("        if ")
          .append(String.join(" || ", succeeded))
          .append(" {\n")
          .append(pad)
          .append("            ::core::panic!(\"")
          .append(f.name())
          .append(": Err with a success status\");\n")
          .append(pad)
          .append("        }\n")
          .append(pad)
          .append("        status\n")
          .append(pad)
          .append("    }\n")
          .append(pad)
          .append("}\n");
    } else if (shape.creates() != null && shape.creates().handle() == null)
      out.append(pad)
          .append("match ")
          .append(call)
          .append(" {\n")
          .append(pad)
          .append("    Some(owned) => Box::into_raw(owned).cast(),\n")
          .append(pad)
          .append("    None => ::core::ptr::null_mut(),\n")
          .append(pad)
          .append("}\n");
    else out.append(pad).append(call).append('\n');
    out.append("        }\n");
  }
}
