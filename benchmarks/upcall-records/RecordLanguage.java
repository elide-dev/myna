// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package bench.records;

import com.oracle.truffle.api.TruffleLanguage;

@TruffleLanguage.Registration(id = "recordbench", name = "Record traversal benchmark", version = "1")
public final class RecordLanguage extends TruffleLanguage<RecordLanguage> {
  static final ContextReference<RecordLanguage> CURRENT = ContextReference.create(RecordLanguage.class);

  @Override protected RecordLanguage createContext(Env env) { return this; }
}
