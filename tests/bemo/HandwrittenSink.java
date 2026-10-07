package fixture.bemo;

import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;

/** Consumer-maintained ABI. myna references this type without redeclaring it. */
public interface HandwrittenSink extends CFunctionPointer {
  @InvokeCFunctionPointer
  long invoke(IsolateThread thread, long value);
}
