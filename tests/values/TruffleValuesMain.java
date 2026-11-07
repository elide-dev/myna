package fixture.values;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.runtime.OptimizedCallTarget;
import dev.elide.myna.values.NativeValueTape;
import dev.elide.myna.values.ValueTape;

/** Real SVM runtime compilation, including deoptimization and reader rebinding. */
public final class TruffleValuesMain {
  private static final class Read extends RootNode {
    private final ValueTape.Utf8Key key = ValueTape.Utf8Key.of("costarring");
    Read() { super(null); }
    @Override public Object execute(VirtualFrame frame) {
      NativeValueTape tape = (NativeValueTape) frame.getArguments()[0];
      long index = (long) frame.getArguments()[1];
      return tape.kindAt(index) == ValueTape.STRING && tape.keyMatches(index, key);
    }
  }
  public static void main(String[] args) {
    System.setProperty("polyglot.engine.BackgroundCompilation", "false");
    System.setProperty("polyglot.engine.CompilationFailureAction", "Throw");
    ValuesMain.compiledCheck = tape -> {
      var target = (OptimizedCallTarget) new Read().getCallTarget();
      boolean stable = false;
      for (int attempt = 0; attempt < 8 && !stable; attempt++) {
        for (int i = 0; i < 1000; i++) {
          if (!Boolean.TRUE.equals(target.call(tape, 2L)) || !Boolean.FALSE.equals(target.call(tape, 4L)))
            throw new AssertionError("Compiled key comparison");
        }
        target.compile(true);
        target.waitForCompilation();
        if (!Boolean.TRUE.equals(target.call(tape, 2L))) throw new AssertionError("Compiled kind");
        stable = target.isValidLastTier();
      }
      if (!stable) throw new AssertionError("Reader did not reach stable compiled tier");
      boolean rejected = false;
      try { target.call(tape, -1L); } catch (IndexOutOfBoundsException expected) { rejected = true; }
      if (!rejected || !Boolean.TRUE.equals(target.call(tape, 2L)))
        throw new AssertionError("Reader deoptimization recovery");
    };
    ValuesMain.main(new String[0]);
    System.out.println("Truffle reader runtime compilation passed");
  }
}
