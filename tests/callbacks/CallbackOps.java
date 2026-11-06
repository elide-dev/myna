package fixture.callbacks;

public final class CallbackOps {
  public static long reduce(long left, long right) {
    if (left < 0) throw new IllegalArgumentException("sentinel case");
    return left + right;
  }
}
