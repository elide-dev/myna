// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
import static fixture.pointers.PointersFFM.*;

import fixture.pointers.PointersFFM;
import java.lang.foreign.*;
import java.lang.foreign.MemoryLayout.PathElement;
import java.nio.file.Path;

/**
 * Reads a struct an import returns, and the structs its fields point to, without one reinterpret.
 * Runs unchanged on HotSpot and Native Image.
 */
public final class PointersMain {
  private static void check(boolean condition, String what) {
    if (!condition) throw new AssertionError(what);
  }

  private static AddressLayout field(StructLayout layout, String name) {
    return (AddressLayout) layout.select(PathElement.groupElement(name));
  }

  public static void main(String[] args) throws Throwable {
    try (Arena library = Arena.ofConfined()) {
      var binding = new PointersFFM.LibraryBinding(Path.of(args[0]), library);
      var critical = new PointersFFM.CriticalLibraryBinding(Path.of(args[0]), library);
      for (MemorySegment head : new MemorySegment[] {binding.head(), critical.head()}) {
        check(head.byteSize() == Node_LAYOUT.byteSize(), "a returned record is sized to its layout");
        check(head.scope().equals(Arena.global().scope()), "a returned record is in the global scope");
        check(head.get(ValueLayout.JAVA_LONG, Node_value_OFFSET) == 1, "head value");

        // Through the struct's own pointer layout.
        MemorySegment second = head.get(Node_POINTER, Node_next_OFFSET);
        check(second.byteSize() == 40 && second.get(ValueLayout.JAVA_LONG, Node_value_OFFSET) == 2, "next");
        MemorySegment end = second.get(Node_POINTER, Node_next_OFFSET);
        check(end.address() == 0 && end.equals(MemorySegment.NULL) && end.byteSize() == 40, "null next");

        // Through the record layout's field elements. Node.next and Node.leaf close the cycles, so
        // they target a padding layout of the pointee's size; Leaf.owner targets Node_LAYOUT itself.
        MemorySegment next = head.get(field(Node_LAYOUT, "next"), Node_next_OFFSET);
        check(next.equals(second) && next.byteSize() == Node_LAYOUT.byteSize(), "self-reference field");
        MemorySegment leaf = head.get(field(Node_LAYOUT, "leaf"), Node_leaf_OFFSET);
        check(leaf.byteSize() == Leaf_LAYOUT.byteSize(), "cycle field");
        check(leaf.get(ValueLayout.JAVA_DOUBLE, Leaf_weight_OFFSET) == 2.5, "leaf weight");
        check(field(Leaf_LAYOUT, "owner").targetLayout().orElseThrow().equals(Node_LAYOUT), "owner target");
        MemorySegment owner = leaf.get(field(Leaf_LAYOUT, "owner"), Leaf_owner_OFFSET);
        check(owner.equals(head) && owner.byteSize() == 40, "owner");
        check(owner.get(ValueLayout.JAVA_LONG, Node_value_OFFSET) == 1, "owner value");

        // Opaque and scalar pointers stay unsized.
        check(head.get(field(Node_LAYOUT, "handle"), Node_handle_OFFSET).byteSize() == 0, "opaque");
        MemorySegment name = head.get(field(Node_LAYOUT, "name"), Node_name_OFFSET);
        check(name.address() != 0 && name.byteSize() == 0, "scalar pointer");

        // A typed parameter takes any segment.
        check(binding.weigh(leaf) == 2.5 && critical.weigh(leaf) == 2.5, "typed parameter");
      }
      check(head_DESCRIPTOR.returnLayout().orElseThrow().equals(Node_POINTER), "typed descriptor");
      MemorySegment none = binding.none();
      check(none.address() == 0 && none.byteSize() == Node_LAYOUT.byteSize(), "null return");
    }
    System.out.println("FFM struct pointers passed");
  }
}
