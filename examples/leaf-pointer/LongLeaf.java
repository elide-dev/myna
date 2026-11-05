package example.leaf;

import com.oracle.svm.shared.AlwaysInline;
import com.oracle.svm.shared.Uninterruptible;
import org.graalvm.nativeimage.c.function.CFunction;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;

/**
 * Opt-in pinned-Graal adapter for a primitive-only native leaf.
 * Every target must be short, bounded, nonblocking, and unable to reenter Java.
 * The address and its containing library must remain valid for every invocation.
 */
public interface LongLeaf extends CFunctionPointer {
  @AlwaysInline("Remove the primitive-only native leaf adapter")
  @Uninterruptible(reason="Atomic primitive-only native call with no managed references", mayBeInlined=true)
  @InvokeCFunctionPointer(transition=CFunction.Transition.NO_TRANSITION)
  long invoke(long value);
}
