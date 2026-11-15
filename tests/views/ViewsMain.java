// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package fixture.views;

import com.oracle.svm.shared.NeverInline;
import org.graalvm.word.WordFactory;

/**
 * Counted views in Native Image, against C's static records: element records through the indexed
 * {@code addressOf(long)} and scalar elements through the C pointer. The image is built as
 * consumers build theirs, without {@code -ea}: the bound is a caller contract, and the test checks
 * that each helper compiles to the raw access it wraps.
 */
public final class ViewsMain {
  private ViewsMain() {}

  private static void check(boolean condition, String what) {
    if (!condition) throw new AssertionError(what);
  }

  // Measured pairs: each generated helper beside the raw access it wraps. The test compares their
  // code: one load of the pointer field and the scaled access, with no call.
  @NeverInline("measured")
  static Views.Function functionAt(Views.Program program, long i) {
    return Views.Program_functionsAt(program, i);
  }

  @NeverInline("measured")
  static Views.Function rawFunctionAt(Views.Program program, long i) {
    return program.functions().addressOf(i);
  }

  @NeverInline("measured")
  static short textAt(Views.Program program, long i) {
    return Views.Program_textAt(program, i);
  }

  @NeverInline("measured")
  static short rawTextAt(Views.Program program, long i) {
    return program.text().read(WordFactory.signed(i));
  }

  public static void main(String[] args) {
    // Indexes the image builder cannot see (no arguments are passed), so the measured methods
    // keep their index parameter instead of folding a constant one.
    long first = args.length;
    long second = first + 1;
    long third = first + 2;
    Views.Program program = Views.program();
    check(program.functionCount() == 2 && program.textLength() == 3, "counts");
    Views.Function element = functionAt(program, second);
    check(element.id() == 11 && element.arity() == 2, "element record");
    check(element.entry() == 0xfffffffffL, "element entry");
    check(element.rawValue() - program.functions().rawValue() == Views.Function.SIZE, "stride");
    check(rawFunctionAt(program, second).equal(element), "addressOf(long)");
    check(functionAt(program, first).id() == 10, "first element");
    check(textAt(program, second) == 'i' && rawTextAt(program, second) == 'i', "scalar element");
    check(textAt(program, third) == (short) 0xffff, "unsigned scalar element");
    Views.Program empty = Views.empty();
    check(empty.functions().isNull() && empty.functionCount() == 0, "empty functions");
    check(empty.text().isNull() && empty.textLength() == 0, "empty text");
    System.out.println("Native Image counted views passed");
  }
}
