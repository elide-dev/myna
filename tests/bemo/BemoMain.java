package fixture.bemo;

import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.function.CEntryPointLiteral;

public final class BemoMain {
  public static final class Literals {
    public static final CEntryPointLiteral<HandwrittenSink> SINK =
        CEntryPointLiteral.create(BemoMain.class, "receive", IsolateThread.class, long.class);
  }

  @CEntryPoint(name = "bemo_receive")
  public static long receive(IsolateThread thread, long value) {
    return value + 2;
  }

  public static void main(String[] args) {
    long result =
        BemoImports.invokeSink(
            Literals.SINK.getFunctionPointer(), CurrentIsolate.getCurrentThread(), 40);
    if (result != 42) throw new AssertionError(result);
    System.out.println("Bemo static linkage and handwritten callbacks passed");
  }
}
