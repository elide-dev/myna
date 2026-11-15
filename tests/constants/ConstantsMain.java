// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package fixture.constants;

import com.oracle.svm.shared.NeverInline;
import org.graalvm.nativeimage.StackValue;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CFunction;

/** Constants, @CEnum enums, typed wrappers and opt-in @CBitfield in a real image. */
public final class ConstantsMain {
  private ConstantsMain() {}

  /** A consumer-written, transitioning C function typed with the generated @CEnum. */
  @CContext(ConstsDirectives.class)
  static final class Consumer {
    private Consumer() {}

    @CFunction("consts_echo_status")
    static native Consts.Status echo(Consts.Status value);
  }

  private static void check(boolean condition, String what) {
    if (!condition) throw new AssertionError(what);
  }

  // Measured: a switch on the raw leaf import's result, with javac-inlined case labels from
  // ConstsConstants. Expected: immediate compares, no CEnumRuntimeData, no transition.
  @NeverInline("measured")
  static int rawSwitch(Consts.Record view) {
    switch (Consts.status(view)) {
      case ConstsConstants.Status.OK:
        return 1;
      case ConstsConstants.Status.SYNTAX_ERROR:
        return 2;
      case ConstsConstants.Status.NULL_RESULT:
        return 3;
      default:
        return 0;
    }
  }

  // Measured: a constant enum argument through the typed overload is the immediate 255.
  @NeverInline("measured")
  static void passConst(Consts.Session session) {
    Consts.setLevel(Consts.Level.HIGH, session);
  }

  @NeverInline("measured")
  static boolean bitfieldLast(Consts.Record view) {
    return view.wideLast();
  }

  @NeverInline("measured")
  static boolean maskLast(Consts.Record view) {
    return ConstsConstants.Wide.last(view.wide());
  }

  // Measured: a range getter, a range setter, the @CBitfield range read and an enum-case compare.
  // Expected: shifts and masks or one narrow load, compares with an immediate; no call, no
  // CEnumRuntimeData, no allocation.
  @NeverInline("measured")
  static byte rangeGet(int flags) {
    return ConstsConstants.Packed.kind(flags);
  }

  @NeverInline("measured")
  static int rangeSet(int flags, byte kind) {
    return ConstsConstants.Packed.withKind(flags, kind);
  }

  @NeverInline("measured")
  static byte bitfieldRange(Consts.Node node) {
    return node.packedKind();
  }

  @NeverInline("measured")
  static boolean caseIsSub(Consts.Node node) {
    return Consts.operandAsBinaryOp(node) == Consts.BinaryOp.SUB;
  }

  public static void main(String[] args) {
    Consts.Record view = StackValue.get(Consts.Record.class);
    Consts.Session session = StackValue.get(8);

    view.status(16);
    check(rawSwitch(view) == 2, "raw switch on syntax_error");
    check(Consts.statusEnum(view) == Consts.Status.SYNTAX_ERROR, "statusEnum(16)");
    view.status(7);
    check(Consts.status(view) == 7, "the raw import keeps an unlisted value");
    check(Consts.statusEnum(view) == null, "statusEnum is null for an unlisted value");
    check(rawSwitch(view) == 0, "raw switch default");
    view.status(-1);
    check(rawSwitch(view) == 3, "raw switch on null_result");
    System.out.println("statusEnum(7) == null, status(7) == 7: true");

    passConst(session);
    check(Consts.blockingStatus(session) == 255, "setLevel(Level.HIGH) passed 255");
    check(Consts.blockingStatusEnum(session) == null, "255 is no Status");
    Consts.setLevel(Consts.Level.LOW, session);
    check(Consts.blockingStatusEnum(session) == Consts.Status.OK, "setLevel(Level.LOW) passed 0");

    check(Consumer.echo(Consts.Status.INTERNAL_ERROR) == Consts.Status.INTERNAL_ERROR, "echo");
    check(Consts.Status.INTERNAL_ERROR.cValue() == 3, "cValue");
    check(Consts.Status.lookup(16) == Consts.Status.SYNTAX_ERROR, "lookup");
    check(Consts.Status.of(16) == Consts.Status.SYNTAX_ERROR, "of");

    // @CBitfield accessors and the masks name the same bits of the same word.
    view.wide(ConstsConstants.Wide.LAST);
    check(view.wideLast() && !view.wideFirst(), "bitfield read of LAST");
    check(bitfieldLast(view) == maskLast(view), "bitfield and mask agree");
    view.wideFirst(true);
    check(view.wide() == ConstsConstants.Wide.ALL, "bitfield write of FIRST");
    view.wideLast(false);
    check(view.wide() == ConstsConstants.Wide.FIRST, "bitfield clear of LAST");
    check(!ConstsConstants.Wide.hasUnknown(view.wide()), "no unknown bits");
    check(Consts.magic() == ConstsConstants.MAGIC, "@CConstant agrees with the literal");

    // A range's @CBitfield accessor and the static getter and setter name the same bits.
    Consts.Node node = StackValue.get(Consts.Node.class);
    node.packed(
        ConstsConstants.Packed.withDepth(
            ConstsConstants.Packed.withKind(ConstsConstants.Packed.OPEN, ConstsConstants.Kind.LAST),
            9));
    check(node.packed() == 0x9ff01, "range setters");
    // Typed like ConstsConstants: Kind.LAST is (byte) 255, and so is the enum range's bit-field read.
    check(node.packedKind() == ConstsConstants.Kind.LAST, "range bitfield read is the Kind carrier");
    check(bitfieldRange(node) == ConstsConstants.Kind.LAST, "measured bitfield read");
    check(node.packedDepth() == 9, "raw range bitfield read");
    check(ConstsConstants.Packed.kind(node.packed()) == ConstsConstants.Kind.LAST, "range getter");
    check(rangeGet(node.packed()) == ConstsConstants.Kind.LAST, "measured range getter");
    // Two kinds, so the builder cannot specialize the measured setter to one constant argument.
    check(rangeSet(node.packed(), ConstsConstants.Kind.BOXED) == 0x90501, "measured range setter");
    check(rangeSet(node.packed(), ConstsConstants.Kind.PLAIN) == 0x90001, "measured range setter");
    node.packedKind(ConstsConstants.Kind.BOXED);
    check(ConstsConstants.Packed.kind(node.packed()) == ConstsConstants.Kind.BOXED, "range bitfield write");
    check(node.packedOpen() && !ConstsConstants.Packed.hasUnknown(node.packed()), "no unknown bits");
    check(ConstsConstants.Packed.hasUnknown(node.packed() | 0x2), "bit 1 is unknown");

    // A tagged field reads through the enum its tag selects; nothing checks the tag.
    node.tag(ConstsConstants.NodeTag.BINARY);
    node.operand(ConstsConstants.BinaryOp.SUB);
    check(Consts.operandAsBinaryOp(node) == Consts.BinaryOp.SUB, "operand as BinaryOp");
    check(caseIsSub(node), "measured enum case compare");
    check(Consts.operandAsUnaryOp(node) == Consts.UnaryOp.INVERT, "operand as UnaryOp");
    node.operand(7);
    check(Consts.operandAsBinaryOp(node) == null, "an unlisted operand is null");
    check(!caseIsSub(node), "measured enum case compare of an unlisted value");

    System.out.println("Native Image constants, @CEnum enums and @CBitfield passed");
  }
}
