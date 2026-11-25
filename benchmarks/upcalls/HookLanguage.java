// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package bench.upcalls;

import com.oracle.truffle.api.TruffleLanguage;

@TruffleLanguage.Registration(id = "upcallbench", name = "Upcall benchmark", version = "1")
public final class HookLanguage extends TruffleLanguage<HookLanguage.State> {
  static final ContextReference<State> CURRENT = ContextReference.create(HookLanguage.class);

  static final class State {
    final Guest.CountingRoot root;
    volatile int calls;
    volatile int compiled;
    volatile int value;

    State(HookLanguage language) { root = new Guest.CountingRoot(language); }
  }

  @Override protected State createContext(Env env) { return new State(this); }
}
