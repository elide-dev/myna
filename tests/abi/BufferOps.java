package fixture;

import dev.elide.seam.generated.SeamNative;
import org.graalvm.nativeimage.StackValue;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CLongPointer;

/** End-to-end fixture: Rust caller -> Native Image sum -> Rust inspect. */
public final class BufferOps {
  public static long sum(CCharPointer data, long length) {
    if (length < 0) throw new IllegalArgumentException("negative length");
    long result = 0;
    for (long i = 0; i < length; i++) result += data.read((int) i) & 0xff;
    CLongPointer slot = StackValue.get(8);
    slot.write(result);
    return SeamNative.inspect(slot);
  }
}
