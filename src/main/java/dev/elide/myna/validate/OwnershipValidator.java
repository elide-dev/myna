// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.validate;

import dev.elide.myna.model.Seam;
import dev.elide.myna.model.Seam.*;
import dev.elide.myna.model.Seam.Module;
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

  static void validate(Module m) {
    if (!m.resources().isEmpty() || !m.borrows().isEmpty()) {
      require(
          !m.opaqueTypes().contains("ownership")
              && m.structs().stream().noneMatch(s -> s.name().equals("ownership"))
              && m.callbacks().stream().noneMatch(c -> c.name().equals("ownership")),
          "ownership is reserved when wrappers are generated");
      require(!RESERVED.contains(m.nativeClass()), "reserved native/ownership class name");
    }
    var functions = new HashMap<String, Function>();
    m.functions().forEach(f -> functions.put(f.name(), f));
    var names = new HashSet<String>();
    var lifecycle = new HashSet<String>();
    var rustTypes = new HashSet<String>();
    for (Resource r : m.resources()) {
      // A resource initialized in place is the struct its handle parameter points to; every other
      // resource is an opaque allocation.
      boolean inPlace = r.handle() != null && r.handleField() == null;
      if (inPlace)
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
      var create = functions.get(r.create());
      var destroy = functions.get(r.destroy());
      require(create != null && destroy != null, "unknown resource lifecycle function");
      require(
          lifecycle.add(r.create()) && lifecycle.add(r.destroy()),
          "lifecycle functions must be distinct per resource");
      nativeCall(create);
      nativeCall(destroy);
      Type pointer = new Pointer(new Named(r.name()));
      if (r.handle() == null) {
        require(create.returns().equals(pointer), "resource create must return ptr<resource>");
        require(r.success().isEmpty(), "success applies to a resource create with a handle");
      } else {
        handle(m, r, create);
      }
      // Pointer arguments pass through each create wrapper under their own declared contracts,
      // including another resource's handle: that parent's lifetime is the consumer's to keep.
      for (Parameter argument : create.parameters())
        require(
            argument.type() instanceof Scalar || argument.type() instanceof Pointer,
            "resource create takes only scalar and pointer arguments: "
                + r.create()
                + " "
                + argument.name());
      require(
          destroy.returns() == Scalar.VOID && destroy.parameters().size() == 1,
          "resource destroy must take one pointer and return void");
      var p = destroy.parameters().getFirst();
      require(
          p.type().equals(pointer)
              && !p.nullable()
              && p.ownership() == Ownership.TRANSFERRED_TO_CALLEE
              && p.access() == Access.READ_WRITE,
          "resource destroy requires a non-null transferred_to_callee read_write resource pointer");
      require(
          p.facts().isEmpty(), "resource destroy cannot require additional pointer preconditions");
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
      nativeCall(f);
      var params = new HashMap<String, Parameter>();
      f.parameters().forEach(p -> params.put(p.name(), p));
      var p = params.get(b.parameter());
      var length = params.get(b.length());
      require(
          p != null && length != null && p != length,
          "borrow must identify distinct buffer and length parameters");
      if (b.held()) held(m, b, f, p);
      require(f.returns() instanceof Scalar, "call borrow cannot return a pointer");
      require(
          !lengths.contains(b.function() + " " + b.parameter()),
          "call buffer cannot be another borrow's length: " + b.function() + " " + b.parameter());
      require(
          p.type().equals(new Pointer(Scalar.U8)) && p.ownership() == Ownership.BORROWED,
          "call buffer requires borrowed ptr<u8>");
      // A nullable buffer may be absent: its wrappers pass the null pointer with length zero, which
      // the callee must accept. Nothing more is required of the declaration.
      // A write buffer is a writable one: no wrapper relies on the callee not reading it.
      require(
          p.access() == Access.READ
              || p.access() == Access.WRITE
              || p.access() == Access.READ_WRITE,
          "call buffer requires read, write or read_write access");
      require(
          Set.of(Scalar.U32, Scalar.I32, Scalar.U64, Scalar.I64).contains(length.type()),
          "buffer length must be a 32/64-bit integer (bytes)");
      // A held buffer is captured by its create, which is the point of holding it.
      if (!b.held())
        require(approved(p.facts(), "nocapture"), "call buffer requires approved nocapture=true");
      if (p.access() == Access.READ)
        require(
            approved(p.facts(), "readonly"), "shared call buffer requires approved readonly=true");
      require(
          !p.facts().containsKey("align")
              && !p.facts().containsKey("dereferenceable")
              && !p.facts().containsKey("noalias"),
          "call buffer cannot impose alignment, extent, or noalias preconditions");
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
        r.create().equals(b.function()),
        "a held borrow belongs to its resource's create, " + r.create() + ": " + form);
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
    require(
        m.truffleConfig() != null && Seam.truffleCell(m, create),
        "a resource handle is generated for Truffle cells only; "
            + r.create()
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
    require(
        out != null
            && out.type() instanceof Pointer
            && !out.nullable()
            && (out.access() == Access.WRITE || out.access() == Access.READ_WRITE),
        "resource handle must name a non-null pointer parameter with write access: " + form);
    Type pointee = ((Pointer) out.type()).pointee();
    if (field == null) {
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
    }
  }

  private static void nativeCall(Function f) {
    require(
        f.direction() == Direction.IMPORT
            && f.error() == ErrorConvention.NO_FAILURE
            && !Validator.enabled(f.facts(), "noreturn"),
        "ownership wrappers require returning no_failure native imports");
    require(approved(f.facts(), "nounwind"), "ownership wrappers require approved nounwind=true");
  }

  private static boolean approved(Map<String, Fact> facts, String key) {
    Fact f = facts.get(key);
    return f != null && f.enabled() && f.approved();
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalArgumentException(message);
  }
}
