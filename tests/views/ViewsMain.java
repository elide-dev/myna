// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package fixture.views;

/**
 * Counted views in Native Image, against C's static records: element records through the indexed
 * {@code addressOf(long)}, scalar elements through the C pointer, and the asserted bound (the image
 * is built with {@code -ea}, so index {@code count} fails).
 */
public final class ViewsMain {
  private ViewsMain() {}

  private static void check(boolean condition, String what) {
    if (!condition) throw new AssertionError(what);
  }

  // Words cannot be captured by lambdas (they would live in an object field), hence two helpers.
  private static boolean functionRejected(Views.Program program, long i) {
    try {
      Views.Program_functionsAt(program, i);
      return false;
    } catch (AssertionError expected) {
      return true;
    }
  }

  private static boolean textRejected(Views.Program program, long i) {
    try {
      Views.Program_textAt(program, i);
      return false;
    } catch (AssertionError expected) {
      return true;
    }
  }

  public static void main(String[] args) {
    Views.Program program = Views.program();
    check(program.functionCount() == 2 && program.textLength() == 3, "counts");
    Views.Function second = Views.Program_functionsAt(program, 1);
    check(second.id() == 11 && second.arity() == 2, "element record");
    check(second.entry() == 0xfffffffffL, "element entry");
    check(second.rawValue() - program.functions().rawValue() == Views.Function.SIZE, "stride");
    check(program.functions().addressOf(1).equal(second), "addressOf(long)");
    check(Views.Program_textAt(program, 1) == 'i', "scalar element");
    check(Views.Program_textAt(program, 2) == (short) 0xffff, "unsigned scalar element");
    check(functionRejected(program, 2), "index count of a record view");
    check(textRejected(program, 3), "index count of a scalar view");
    check(textRejected(program, -1), "negative index");
    Views.Program empty = Views.empty();
    check(empty.functions().isNull() && empty.functionCount() == 0, "empty functions");
    check(functionRejected(empty, 0), "index 0 of an empty view");
    System.out.println("Native Image counted views passed");
  }
}
