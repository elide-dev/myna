// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package bench.nodes;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;

/**
 * The smallest possible language: evaluating {@code library|symbol|signature} loads the library
 * through Truffle NFI, which is internal and therefore only reachable from a language, and returns
 * the bound NFI function. The measured roots are language-less cells; this exists only to obtain the
 * dynamic-FFI baseline.
 */
@TruffleLanguage.Registration(id = "cellbench", name = "cellbench", version = "0", dependentLanguages = "nfi")
public final class BenchLanguage extends TruffleLanguage<TruffleLanguage.Env> {
  private static final ContextReference<Env> CONTEXT = ContextReference.create(BenchLanguage.class);

  @Override
  protected Env createContext(Env env) {
    return env;
  }

  @Override
  protected CallTarget parse(ParsingRequest request) {
    String[] spec = request.getSource().getCharacters().toString().split("\\|");
    return new RootNode(this) {
      @Override
      public Object execute(VirtualFrame frame) {
        return bind(spec[0], spec[1], spec[2]);
      }
    }.getCallTarget();
  }

  @TruffleBoundary
  private static Object bind(String library, String symbol, String signature) {
    Env env = CONTEXT.get(null);
    try {
      Object loaded = env.parseInternal(Source.newBuilder("nfi", "load \"" + library + "\"", "library").internal(true).build()).call();
      Object parsed = env.parseInternal(Source.newBuilder("nfi", signature, "signature").internal(true).build()).call();
      InteropLibrary interop = InteropLibrary.getUncached();
      return interop.invokeMember(parsed, "bind", interop.readMember(loaded, symbol));
    } catch (InteropException e) {
      throw new AssertionError(e);
    }
  }
}
