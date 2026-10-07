package dev.elide.myna.backend;

import dev.elide.myna.model.Seam.*;
import dev.elide.myna.model.Seam.Module;
import java.util.*;

/**
 * Native Image's C interface carries only primitives and words, so a by-value record crosses it in
 * reference form: {@code <symbol>_myna_ref}, with records as {@code const T *} and a record return
 * through a trailing {@code T *result}. C, Rust and FFM keep the real by-value ABI; generated shims
 * and inline wrappers translate, and ThinLTO can inline them away.
 */
public final class ByValue {
  private ByValue() {}

  public static final String SUFFIX = "_myna_ref";
  public static final String RESULT = "result";

  public static boolean applies(Function f) {
    return f.returns() instanceof Named || f.parameters().stream().anyMatch(p -> p.type() instanceof Named);
  }

  public static String symbol(Function f) {
    return f.symbol() + SUFFIX;
  }

  public static Struct struct(Module m, Type type) {
    String name = ((Named) type).name();
    return m.structs().stream()
        .filter(s -> s.name().equals(name))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("not a record: " + name));
  }

  private static Fact contract(String value) {
    return new Fact(value, Provenance.EXPLICIT_CONTRACT, "by-value reference lowering");
  }

  /** Facts the generated shims and wrappers guarantee for record storage they pass. */
  private static Parameter reference(Module m, String name, Type type, boolean result) {
    Struct s = struct(m, type);
    var facts = new TreeMap<String, Fact>();
    facts.put("nonnull", contract("true"));
    facts.put("nocapture", contract("true"));
    facts.put(result ? "writeonly" : "readonly", contract("true"));
    facts.put("align", contract(Integer.toString(s.alignment())));
    facts.put("dereferenceable", contract(Long.toString(s.size())));
    return new Parameter(
        name, new Pointer(type), false, Ownership.BORROWED, result ? Access.WRITE : Access.READ, facts);
  }

  /** The reference form; identity for functions without by-value records. */
  public static Function lower(Module m, Function f) {
    if (!applies(f)) return f;
    var params = new ArrayList<Parameter>();
    for (Parameter p : f.parameters())
      params.add(p.type() instanceof Named ? reference(m, p.name(), p.type(), false) : p);
    Type returns = f.returns();
    if (returns instanceof Named) {
      params.add(reference(m, RESULT, returns, true));
      returns = Scalar.VOID;
    }
    return new Function(
        f.name(),
        symbol(f),
        f.direction(),
        returns,
        params,
        f.error(),
        f.sentinel(),
        f.javaTarget(),
        f.include(),
        f.callback(),
        f.isolateThread(),
        f.facts());
  }
}
