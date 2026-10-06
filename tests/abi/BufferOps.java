package fixture;

import dev.elide.seam.generated.SeamNative;
import org.graalvm.nativeimage.StackValue;
import org.graalvm.word.Pointer;
import org.graalvm.word.PointerBase;

/** End-to-end fixture: Rust caller -> Native Image sum -> Rust inspect. */
public final class BufferOps {
  public static long sum(PointerBase bytes, long length) {
    if (length < 0) throw new IllegalArgumentException("negative length");
    Pointer data = (Pointer) bytes;
    long result = 0;
    for (long i = 0; i < length; i++) result += data.readByte((int) i) & 0xff;
    Pointer slot = StackValue.get(8);
    slot.writeLong(0, result);
    return SeamNative.inspect(slot);
  }
}
