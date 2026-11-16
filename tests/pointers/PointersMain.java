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

        // Through the record layout's field elements. Node.next, Node.leaf and Leaf.owner are in one
        // cycle, so they target a padding layout of the pointee's size and alignment.
        MemorySegment next = head.get(field(Node_LAYOUT, "next"), Node_next_OFFSET);
        check(next.equals(second) && next.byteSize() == Node_LAYOUT.byteSize(), "self-reference field");
        MemorySegment leaf = head.get(field(Node_LAYOUT, "leaf"), Node_leaf_OFFSET);
        check(leaf.byteSize() == Leaf_LAYOUT.byteSize(), "cycle field");
        check(leaf.get(ValueLayout.JAVA_DOUBLE, Leaf_weight_OFFSET) == 2.5, "leaf weight");
        check(field(Leaf_LAYOUT, "owner").targetLayout().orElseThrow() instanceof PaddingLayout, "owner shell");
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

      // A pointer outside every cycle targets the full layout.
      MemorySegment anchor = binding.anchor();
      check(anchor.byteSize() == Anchor_LAYOUT.byteSize(), "anchor");
      check(field(Anchor_LAYOUT, "first").targetLayout().orElseThrow().equals(Node_LAYOUT), "full target");
      MemorySegment first = anchor.get(field(Anchor_LAYOUT, "first"), Anchor_first_OFFSET);
      check(first.byteSize() == 40 && first.get(ValueLayout.JAVA_LONG, Node_value_OFFSET) == 1, "first");

      // A callback's ptr<Node> argument is sized too.
      MemorySegment visit = PointersFFM.VisitUpcalls.create(node -> node.byteSize() != 40 ? -2L
          : node.get(ValueLayout.JAVA_LONG, Node_value_OFFSET)
              + 10 * node.get(Node_POINTER, Node_next_OFFSET).get(ValueLayout.JAVA_LONG, Node_value_OFFSET),
          -1L, library);
      check(binding.visit(visit) == 21L, "callback argument");

      // A struct passed only by value (no Pair_POINTER; the JVM test checks the source).
      MemorySegment pair = library.allocate(Pair_LAYOUT);
      pair.set(ValueLayout.JAVA_LONG, Pair_left_OFFSET, 3L);
      pair.set(ValueLayout.JAVA_LONG, Pair_right_OFFSET, 4L);
      check(binding.sum(pair) == 7L, "by value");
      MemorySegment none = binding.none();
      check(none.address() == 0 && none.byteSize() == Node_LAYOUT.byteSize(), "null return");
    }
    try (Arena library = Arena.ofConfined()) {
      var binding = new PointersFFM.LibraryBinding(Path.of(args[0]), library);
      // An out-pointer constructor hands its factory a struct resource sized to the struct.
      long width = PointersFFM.Box.<Long>create(binding, (address, destroy, thread) -> {
        check(address.byteSize() == Box_LAYOUT.byteSize(), "sized resource");
        long value = address.get(ValueLayout.JAVA_LONG, Box_width_OFFSET);
        destroy.destroy(address);
        return value;
      }, status -> { throw new AssertionError("status " + status); }, 7L);
      check(width == 7L, "resource width");
      // A failed one leaves junk misaligned for a Box in its slot: the status still reaches onFailure.
      long failed = PointersFFM.Box.<Long>create(binding, (address, destroy, thread) -> {
        throw new AssertionError("adopted junk");
      }, status -> (long) status, -1L);
      check(failed == 3L, "failure status");
    }
    System.out.println("FFM struct pointers passed");
  }
}
