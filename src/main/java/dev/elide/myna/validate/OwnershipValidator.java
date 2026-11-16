// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.validate;

import dev.elide.myna.model.Seam;
import dev.elide.myna.model.Seam.*;
import dev.elide.myna.model.Seam.Module;
import java.math.BigInteger;
import java.util.*;

/** Validates the stronger contracts required by generated ownership APIs, even in relaxed mode. */
final class OwnershipValidator {
  private OwnershipValidator() {}

  private static final Set<String> RESERVED =
      Set.of(
          "NativeBuffer",
          "NativeDestructor",
          "ResourceFactory",
          "MemorySegment",
          "Thread",
          "Arena",
          "MethodHandle",
          "SegmentAllocator",
          "SymbolLookup",
          "Linker",
          "ValueLayout",
          "MemoryLayout",
          "StructLayout",
          "FunctionDescriptor",
          "String",
          "Throwable",
          "ExceptionInInitializerError",
          "UnsatisfiedLinkError",
          "IllegalArgumentException",
          "IllegalStateException",
          "WrongThreadException",
          "ReflectiveOperationException",
          "java",
          "org",
          "core",
          "Option",
          "Drop",
          "Send",
          "Sync");

  /** Members of each generated owner that a constructor named after its import would shadow. */
  private static final Set<String> GENERATED_MEMBERS =
      Set.of("adoptUnsafe", "DESTRUCTOR", "from_raw", "into_raw", "borrow", "pointer", "_thread");

  static void validate(Module m) {
    if (!m.resources().isEmpty() || !m.borrows().isEmpty()) {
      require(
          !m.opaqueTypes().contains("ownership")
              && m.structs().stream().noneMatch(s -> s.name().equals("ownership"))
              && m.callbacks().stream().noneMatch(c -> c.name().equals("ownership")),
          "ownership is reserved when wrappers are generated");
      require(!RESERVED.contains(m.nativeClass()), "reserved native/ownership class name");
    }
    if (m.rustImplementation()) rustImplementation(m);
    // Each FFM binding has a NAME_address() accessor beside the NAME(...) call.
    var importNames = new HashSet<String>();
    m.functions().stream()
        .filter(f -> f.direction() == Direction.IMPORT)
        .forEach(f -> importNames.add(f.name()));
    for (Function f : m.functions())
      require(
          f.direction() != Direction.IMPORT
              || !importNames.contains(f.name() + "_address")
              || m.functions().stream()
                  .filter(g -> g.name().equals(f.name() + "_address"))
                  .allMatch(g -> !g.abiParameters().isEmpty()),
          "FFM address accessor " + f.name() + "_address() collides with the import of that name");
    var functions = new HashMap<String, Function>();
    m.functions().forEach(f -> functions.put(f.name(), f));
    var names = new HashSet<String>();
    // Each lifecycle function, by the resource it belongs to.
    var lifecycle = new HashMap<String, String>();
    var rustTypes = new HashSet<String>();
    for (Resource r : m.resources()) {
      // A resource initialized in place is the struct its handle parameter points to; every other
      // resource is an opaque allocation.
      // A resource written through a ptr<ptr<R>> out-parameter may be either.
      boolean outPointer = m.outPointer(r);
      boolean inPlace = r.handle() != null && r.handleField() == null && !outPointer;
      if (outPointer)
        require(
            m.opaqueTypes().contains(r.name())
                || m.structs().stream().anyMatch(s -> s.name().equals(r.name())),
            "a resource written through an out-pointer must name an opaque type or a struct: "
                + r.name());
      else if (inPlace)
        require(
            m.structs().stream().anyMatch(s -> s.name().equals(r.name())),
            "a resource initialized in place must name a struct: " + r.name());
      else
        require(
            m.opaqueTypes().contains(r.name()), "resource must name an opaque type: " + r.name());
      require(names.add(r.name()), "duplicate resource: " + r.name());
      require(
          rustTypes.add(r.name()) && rustTypes.add(r.name() + "Borrow"),
          "resource names collide with generated borrow types");
      require(!r.name().equals(m.nativeClass()), "resource shadows native API class");
      require(!RESERVED.contains(r.name()), "reserved resource name");
      require(!r.name().equals(m.ownershipClass()), "resource shadows generated ownership class");
      require(!r.name().equals(m.ffmClass()), "resource shadows generated FFM class");
      String form = r.name() + " create=" + String.join(",", r.creates());
      require(
          r.creates().stream().noneMatch(String::isEmpty),
          "resource create lists the names of imports: " + form);
      var destroy = functions.get(r.destroy());
      require(
          destroy != null,
          "unknown resource lifecycle function: " + r.name() + " destroy=" + r.destroy());
      require(
          lifecycle.putIfAbsent(r.destroy(), r.name()) == null,
          "lifecycle functions must be distinct per resource: "
              + r.destroy()
              + " is a lifecycle function of "
              + lifecycle.get(r.destroy())
              + " and "
              + r.name());
      nativeCall(destroy);
      for (String name : r.creates()) {
        var create = functions.get(name);
        require(create != null, "unknown resource lifecycle function: " + form + ": " + name);
        // A function constructs at most one resource, and is never also a destroy.
        String other = lifecycle.putIfAbsent(name, r.name());
        require(
            other == null,
            "lifecycle functions must be distinct per resource: "
                + name
                + (r.name().equals(other)
                    ? (name.equals(r.destroy())
                        ? " is both a constructor and the destroy of "
                        : " is listed twice in the constructors of ")
                    : " is a lifecycle function of " + other + " and ")
                + r.name());
        nativeCall(create);
        Type pointer = new Pointer(new Named(r.name()));
        if (r.handle() == null) {
          require(
              create.returns().equals(pointer),
              "resource create must return ptr<resource>: " + name + " for " + r.name());
          require(
              r.success().isEmpty(),
              "success applies to a resource create with a handle: " + r.name());
        } else {
          handle(m, r, create);
        }
        // Pointer arguments pass through each create wrapper under their own declared contracts,
        // including another resource's handle: that parent's lifetime is the consumer's to keep.
        for (Parameter argument : create.parameters())
          require(
              argument.type() instanceof Scalar || argument.type() instanceof Pointer,
              "resource create takes only scalar and pointer arguments: "
                  + name
                  + " "
                  + argument.name());
      }
      // One Failure callback takes every out-pointer constructor's status, in one carrier.
      if (outPointer)
        for (String name : r.creates())
          require(
              functions.get(name).returns().equals(functions.get(r.creates().getFirst()).returns())
                  && Objects.equals(
                      functions.get(name).returnsGroup(),
                      functions.get(r.creates().getFirst()).returnsGroup()),
              "the out-pointer constructors of "
                  + r.name()
                  + " return one status type: "
                  + name
                  + " differs from "
                  + r.creates().getFirst());
      // Several constructors are named after their imports beside the generated members.
      if (r.creates().size() > 1)
        for (String name : r.creates())
          require(
              !GENERATED_MEMBERS.contains(name),
              "a resource constructor's wrapper is named after its import, which "
                  + name
                  + " shadows: "
                  + form);
      require(
          destroy.returns() == Scalar.VOID && destroy.parameters().size() == 1,
          "resource destroy must take one pointer and return void: " + r.destroy());
      var p = destroy.parameters().getFirst();
      // A destroy may accept null, as free does: no generated owner passes it one.
      require(
          p.type().equals(new Pointer(new Named(r.name())))
              && p.ownership() == Ownership.TRANSFERRED_TO_CALLEE
              && p.access() == Access.READ_WRITE,
          "resource destroy requires a transferred_to_callee read_write resource pointer: "
              + r.destroy()
              + " "
              + p.name());
      require(
          p.facts().isEmpty(),
          "resource destroy cannot require additional pointer preconditions: "
              + r.destroy()
              + " "
              + p.name());
    }
    // A C string: a read-only NUL-terminated ptr<u8> with no length, lent for the call.
    for (Function f : m.functions())
      for (Parameter p : f.parameters()) {
        if (p.access() != Access.CSTRING) continue;
        String text = f.name() + " " + p.name();
        require(
            f.direction() == Direction.IMPORT,
            "access=cstring applies to import parameters: " + text);
        nativeCall(f);
        require(
            p.type().equals(new Pointer(Scalar.U8)) && p.ownership() == Ownership.BORROWED,
            "access=cstring requires a borrowed ptr<u8>: " + text);
        require(
            approved(p.facts(), "nocapture") && approved(p.facts(), "readonly"),
            "access=cstring requires approved nocapture=true and readonly=true: " + text);
        require(
            !p.facts().containsKey("align")
                && !p.facts().containsKey("truffle_buffer")
                && !p.facts().containsKey("dereferenceable"),
            "access=cstring takes no align, dereferenceable or truffle_buffer fact: " + text);
        // Its wrappers pass every other parameter through in the raw binding's carrier, callbacks
        // and isolate threads included; a by-value record has no such carrier in every wrapper.
        for (Parameter other : f.parameters())
          require(
              !(other.type() instanceof Named),
              "C string wrappers pass no by-value record beside their strings: "
                  + f.name()
                  + " "
                  + other.name());
        require(
            m.borrows().stream()
                .noneMatch(b ->
                    b.function().equals(f.name())
                        && (b.parameter().equals(p.name()) || b.length().equals(p.name()))),
            "a C string has no length and is no borrow's buffer or length: " + text);
      }
    // A function may declare several borrows; each has a buffer and a length of its own.
    // Identifiers contain no space, so "function parameter" is a unique key.
    var buffers = new HashSet<String>();
    var lengths = new HashSet<String>();
    for (BufferBorrow b : m.borrows()) {
      require(
          buffers.add(b.function() + " " + b.parameter()),
          "duplicate borrow: " + b.function() + " " + b.parameter());
      require(
          lengths.add(b.function() + " " + b.length()),
          "borrows cannot share a length parameter: " + b.function() + " " + b.length());
    }
    for (BufferBorrow b : m.borrows()) {
      Function f = functions.get(b.function());
      require(f != null, "unknown borrow function: " + b.function());
      String form = b.function() + " " + b.parameter();
      nativeCall(f);
      var params = new HashMap<String, Parameter>();
      f.parameters().forEach(p -> params.put(p.name(), p));
      var p = params.get(b.parameter());
      var length = params.get(b.length());
      require(
          p != null && length != null && p != length,
          "borrow must identify distinct buffer and length parameters: "
              + form
              + " length="
              + b.length());
      if (b.held()) held(m, b, f, p);
      // A declared constructor returns a fresh owned resource, never a view into its buffers, and
      // a call borrow ends before it returns. Nothing ties any other returned pointer to a buffer.
      require(
          f.returns() instanceof Scalar || constructor(m, f),
          "call borrow cannot return a pointer: " + form);
      require(
          !lengths.contains(b.function() + " " + b.parameter()),
          "call buffer cannot be another borrow's length: " + b.function() + " " + b.parameter());
      require(
          p.type().equals(new Pointer(Scalar.U8)) && p.ownership() == Ownership.BORROWED,
          "call buffer requires borrowed ptr<u8>: " + form);
      // A nullable buffer may be absent: its wrappers pass the null pointer with length zero, which
      // the callee must accept. Nothing more is required of the declaration.
      // A write buffer is a writable one: no wrapper relies on the callee not reading it.
      require(
          p.access() == Access.READ
              || p.access() == Access.WRITE
              || p.access() == Access.READ_WRITE,
          "call buffer requires read, write or read_write access: " + form);
      require(
          // usize is size_t, the 64-bit unsigned case on the supported (LP64) targets.
          Set.of(Scalar.U32, Scalar.I32, Scalar.U64, Scalar.I64, Scalar.USIZE)
              .contains(length.type()),
          "buffer length must be a 32/64-bit integer or usize (bytes): "
              + form
              + " length="
              + b.length());
      // A held buffer is captured by its create, which is the point of holding it.
      if (!b.held())
        require(
            approved(p.facts(), "nocapture"),
            "call buffer requires approved nocapture=true: " + form);
      if (p.access() == Access.READ)
        require(
            approved(p.facts(), "readonly"),
            "shared call buffer requires approved readonly=true: " + form);
      // An align fact is a caller contract: seam.ll promises it, debug builds of the wrappers check
      // it, and Truffle cells copy a buffer that violates it. Extent and noalias stay rejected.
      require(
          !p.facts().containsKey("dereferenceable") && !p.facts().containsKey("noalias"),
          "call buffer cannot impose extent or noalias preconditions: " + form);
      // Every wrapper lends a present empty buffer an aligned dummy, which bounds what it can be.
      if (p.facts().containsKey("align"))
        require(
            Long.parseLong(p.facts().get("align").value()) <= 4096,
            "call buffer align must be at most 4096: " + form);
      // Other pointers pass through each wrapper under their own declared contracts. Callbacks,
      // by-value records and isolate threads have no pass-through carrier in every backend.
      for (Parameter other : f.parameters())
        require(
            other.type() instanceof Scalar || other.type() instanceof Pointer,
            "call buffer wrappers pass only scalars and pointers beside their buffers: "
                + b.function()
                + " "
                + other.name());
    }
  }

  /**
   * A borrow held by a resource: the create keeps the buffer until that resource is destroyed. Only
   * a Truffle cell's owner can release a buffer then, so the create must be a cell, and one that
   * reports a status and writes its resource through a parameter: a returned pointer is rejected
   * beside any borrow. The callee keeps the buffer, so it must not be declared {@code nocapture},
   * and it only reads it: nothing is written back once the call has returned.
   */
  private static void held(Module m, BufferBorrow b, Function f, Parameter p) {
    String form = b.function() + " " + b.parameter() + " lifetime=resource:" + b.resource();
    Resource r =
        m.resources().stream().filter(x -> x.name().equals(b.resource())).findFirst().orElse(null);
    require(r != null, "a held borrow names a declared resource: " + form);
    require(
        r.createdBy(b.function()),
        "a held borrow belongs to its resource's create, "
            + String.join(",", r.creates())
            + ": "
            + form);
    require(
        m.truffleConfig() != null && Seam.truffleCell(m, f),
        "a held borrow is generated for Truffle cells only; " + f.name() + " needs one: " + form);
    require(
        r.handle() != null,
        "a held borrow needs a create that reports a status and writes its resource through a"
            + " parameter (handle=...): "
            + form);
    require(p.access() == Access.READ, "a held buffer is read-only (access=read): " + form);
    require(
        !Validator.enabled(p.facts(), "nocapture"),
        "a held buffer is captured by its create and cannot be nocapture: " + form);
  }

  /**
   * A create that reports through its result and writes the resource where {@code handle} says.
   * Only Truffle cells generate an owner for it, so the module must generate cells and the create
   * must be one.
   */
  private static void handle(Module m, Resource r, Function create) {
    String form = r.name() + " handle=" + r.handle();
    // A ptr<ptr<R>> out-parameter: every ownership API wraps it, not only Truffle cells.
    boolean outPointer = m.outPointer(r);
    require(
        outPointer || (m.truffleConfig() != null && Seam.truffleCell(m, create)),
        "a resource handle is generated for Truffle cells only; "
            + create.name()
            + " needs one: "
            + form);
    String parameter = r.handleParameter();
    String field = r.handleField();
    require(
        !parameter.isEmpty() && (field == null || (!field.isEmpty() && !field.contains("."))),
        "resource handle is PARAMETER or PARAMETER.FIELD: " + form);
    Parameter out =
        create.parameters().stream()
            .filter(p -> p.name().equals(parameter))
            .findFirst()
            .orElse(null);
    if (outPointer) {
      // Every wrapper passes a slot of its own, so the parameter may be declared nullable.
      require(
          out != null
              && out.type().equals(new Pointer(new Pointer(new Named(r.name()))))
              && out.access() == Access.WRITE,
          "a resource out-pointer must be a ptr<ptr<"
              + r.name()
              + ">> parameter with access=write: "
              + create.name()
              + " "
              + parameter);
      require(
          create.returns() instanceof Scalar status
              && status != Scalar.VOID
              && status != Scalar.BOOL
              && status != Scalar.F32
              && status != Scalar.F64,
          "a resource create with an out-pointer returns an integer status: " + create.name());
      // Every wrapper passes an 8-byte, 8-aligned slot.
      for (String fact : List.of("align", "dereferenceable"))
        require(
            !out.facts().containsKey(fact)
                || !out.facts().get(fact).enabled()
                || Long.parseLong(out.facts().get(fact).value()) <= 8,
            "a resource out-pointer is an 8-byte slot; "
                + fact
                + " promises more: "
                + create.name()
                + " "
                + parameter);
    }
    require(
        out != null
            && out.type() instanceof Pointer
            && (outPointer || !out.nullable())
            && (out.access() == Access.WRITE || out.access() == Access.READ_WRITE),
        "resource handle must name a non-null pointer parameter with write access: " + form);
    Type pointee = ((Pointer) out.type()).pointee();
    if (outPointer) {
      // Checked above.
    } else if (field == null) {
      require(
          pointee.equals(new Named(r.name())),
          "a resource initialized in place is what its handle parameter points to: " + form);
    } else {
      Struct struct =
          pointee instanceof Named named
              ? m.structs().stream()
                  .filter(s -> s.name().equals(named.name()))
                  .findFirst()
                  .orElse(null)
              : null;
      require(struct != null, "resource handle field requires a pointer to a struct: " + form);
      require(
          struct.fields().stream()
              .anyMatch(
                  f -> f.name().equals(field) && f.type().equals(new Pointer(new Named(r.name())))),
          "resource handle field must be a ptr<resource> field of " + struct.name() + ": " + form);
    }
    if (create.returns() == Scalar.VOID) {
      require(
          r.success().isEmpty(),
          "a create that returns void has no result to compare with success: " + form);
    } else {
      require(
          create.returns() instanceof Scalar status
              && status != Scalar.BOOL
              && status != Scalar.F32
              && status != Scalar.F64,
          "a resource create with a handle returns an integer status or void: " + form);
      require(
          !r.success().isEmpty(),
          "a resource create with a handle must list the results that mean success: " + form);
      // Each success value must be representable in the create's return scalar after guest
      // conversion: the Truffle cell compares the converted carrier against each value verbatim,
      // so a status the carrier can never take makes the success guard always fail and every
      // create throw at runtime. Mirrors the integer_sentinel range check in Validator.
      Scalar scalar = (Scalar) create.returns();
      var abi = Seam.targetAbi(m.targetTriple());
      for (long value : r.success())
        require(
            Ranges.fits(scalar, BigInteger.valueOf(value), abi),
            "a resource create's success status is outside its return scalar's carrier range: "
                + form);
    }
  }

  /**
   * The Rust implementer trait and its support module are items of the module {@code seam.rs} is
   * included in, beside the declared types and the raw functions.
   */
  private static void rustImplementation(Module m) {
    var names = new HashSet<String>(m.opaqueTypes());
    m.structs().forEach(s -> names.add(s.name()));
    m.callbacks().forEach(c -> names.add(c.name()));
    m.enums().forEach(e -> names.add(e.name()));
    m.flagSets().forEach(f -> names.add(f.name()));
    m.functions().forEach(f -> names.add(f.name()));
    // Box is named unqualified, so that a no_std crate can bring alloc's into scope.
    for (String generated : List.of(m.rustImplementationTrait(), "myna_implementation", "Box"))
      require(
          !names.contains(generated),
          "rust_implementation=trait names " + generated + ", which a declaration shadows");
  }

  /** Whether {@code f} is the constructor of a resource it returns a pointer to. */
  private static boolean constructor(Module m, Function f) {
    return m.returnedResources().stream().anyMatch(r -> r.createdBy(f.name()));
  }

  private static void nativeCall(Function f) {
    require(
        f.direction() == Direction.IMPORT
            && f.error() == ErrorConvention.NO_FAILURE
            && !Validator.enabled(f.facts(), "noreturn"),
        "ownership wrappers require returning no_failure native imports: " + f.name());
    require(
        approved(f.facts(), "nounwind"),
        "ownership wrappers require approved nounwind=true: " + f.name());
  }

  private static boolean approved(Map<String, Fact> facts, String key) {
    Fact f = facts.get(key);
    return f != null && f.enabled() && f.approved();
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalArgumentException(message);
  }
}
