package dev.elide.myna.validate;

import dev.elide.myna.model.Seam.*;
import dev.elide.myna.model.Seam.Module;
import java.util.*;

/** Validates the stronger contracts required by generated ownership APIs, even in relaxed mode. */
final class OwnershipValidator {
  private OwnershipValidator() {}

  static void validate(Module m) {
    if (!m.resources().isEmpty() || !m.borrows().isEmpty()) {
      require(
          !m.opaqueTypes().contains("ownership")
              && m.structs().stream().noneMatch(s -> s.name().equals("ownership")),
          "ownership is reserved when wrappers are generated");
      require(
          !Set.of(
                  "NativeBuffer",
                  "NativeDestructor",
                  "ResourceFactory",
                  "java",
                  "org",
                  "core",
                  "Option")
              .contains(m.ownershipClass()),
          "reserved ownership class name");
    }
    var functions = new HashMap<String, Function>();
    m.functions().forEach(f -> functions.put(f.name(), f));
    var names = new HashSet<String>();
    var lifecycle = new HashSet<String>();
    var rustTypes = new HashSet<String>();
    for (Resource r : m.resources()) {
      require(m.opaqueTypes().contains(r.name()), "resource must name an opaque type: " + r.name());
      require(names.add(r.name()), "duplicate resource: " + r.name());
      require(
          rustTypes.add(r.name()) && rustTypes.add(r.name() + "Borrow"),
          "resource names collide with generated borrow types");
      require(!r.name().equals(m.nativeClass()), "resource shadows native API class");
      require(
          !Set.of(
                  "NativeBuffer",
                  "NativeDestructor",
                  "ResourceFactory",
                  "java",
                  "org",
                  "core",
                  "Option")
              .contains(r.name()),
          "reserved resource name");
      require(!r.name().equals(m.ownershipClass()), "resource shadows generated ownership class");
      var create = functions.get(r.create());
      var destroy = functions.get(r.destroy());
      require(create != null && destroy != null, "unknown resource lifecycle function");
      require(
          lifecycle.add(r.create()) && lifecycle.add(r.destroy()),
          "lifecycle functions must be distinct per resource");
      nativeCall(create);
      nativeCall(destroy);
      Type pointer = new Pointer(new Named(r.name()));
      require(
          create.parameters().stream().allMatch(p -> p.type() instanceof Scalar)
              && create.returns().equals(pointer),
          "resource create must take scalar arguments and return ptr<resource>");
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
    names.clear();
    for (BufferBorrow b : m.borrows()) {
      require(names.add(b.function()), "only one call buffer per function is supported");
      Function f = functions.get(b.function());
      require(f != null, "unknown borrow function: " + b.function());
      nativeCall(f);
      require(f.returns() instanceof Scalar, "call borrow cannot return a pointer");
      var params = new HashMap<String, Parameter>();
      f.parameters().forEach(p -> params.put(p.name(), p));
      var p = params.get(b.parameter());
      var length = params.get(b.length());
      require(
          p != null && length != null && p != length,
          "borrow must identify distinct buffer and length parameters");
      require(
          p.type().equals(new Pointer(Scalar.U8)) && p.ownership() == Ownership.BORROWED,
          "call buffer requires borrowed ptr<u8>");
      require(
          p.access() == Access.READ || p.access() == Access.READ_WRITE,
          "call buffer requires read or read_write access");
      require(
          Set.of(Scalar.U32, Scalar.I32, Scalar.U64, Scalar.I64).contains(length.type()),
          "buffer length must be a 32/64-bit integer (bytes)");
      require(approved(p.facts(), "nocapture"), "call buffer requires approved nocapture=true");
      if (p.access() == Access.READ)
        require(
            approved(p.facts(), "readonly"), "shared call buffer requires approved readonly=true");
      require(
          !p.facts().containsKey("align")
              && !p.facts().containsKey("dereferenceable")
              && !p.facts().containsKey("noalias"),
          "call buffer cannot impose alignment, extent, or noalias preconditions");
      for (Parameter other : f.parameters())
        require(
            other == p || other.type() instanceof Scalar,
            "additional pointers are unsupported in call buffer wrappers");
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
