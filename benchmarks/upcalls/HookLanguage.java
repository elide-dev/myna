// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package bench.upcalls;

import com.oracle.truffle.api.TruffleLanguage;

@TruffleLanguage.Registration(id = "upcallbench", name = "Upcall benchmark", version = "1", dependentLanguages = "nfi")
public final class HookLanguage extends TruffleLanguage<HookLanguage.State> {
  static final ContextReference<State> CURRENT = ContextReference.create(HookLanguage.class);

  static final class State {
    final Guest.CountingRoot root;
    final ReducerBench.ReducerRoot reducer;
    final Env env;
    volatile int calls;
    volatile int compiled;
    volatile int value;
    volatile long lastReduction;

    State(HookLanguage language, Env env) {
      this.env = env;
      root = new Guest.CountingRoot(language);
      reducer = new ReducerBench.ReducerRoot(language);
    }
  }

  @Override protected State createContext(Env env) { return new State(this, env); }
}
