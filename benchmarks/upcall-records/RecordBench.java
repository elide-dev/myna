// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package bench.records;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.TruffleSafepoint;
import dev.elide.truffledata.json.JsonSession;
import dev.elide.truffledata.tape.TapeExportNode;
import dev.elide.truffledata.tape.TapeSink;
import dev.elide.truffledata.tape.TapeText;
import dev.elide.truffledata.tape.TapeValue;
import java.lang.foreign.Arena;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.graalvm.polyglot.Context;

/** Compare a native span producer with compiled guest traversal of the same TruffleData tape. */
public final class RecordBench {
  private static final long SEED = 0x6729b03d;

  private static long fold(long previous, long id, long delta) {
    return Long.rotateLeft(previous ^ id, 11) + delta;
  }

  private static long delta(int i) { return (i * 47L % 8191) - 4095; }
  private static long id(int i) { return 0x1_0000_0000L + i; }

  private static byte[] dataset(int rows, int width) {
    var json = new StringBuilder("[");
    for (int i = 0; i < rows; i++) {
      if (i > 0) json.append(',');
      if (i % width == 0) json.append('[');
      if ((i & 1) == 0) {
        json.append("{\"id\":").append(id(i)).append(",\"unused\":true,\"delta\":").append(delta(i));
      } else {
        json.append("{\"delta\":").append(delta(i)).append(",\"unused\":false,\"id\":").append(id(i));
      }
      json.append('}');
      if ((i + 1) % width == 0 || i + 1 == rows) json.append(']');
    }
    return json.append(']').toString().getBytes(StandardCharsets.UTF_8);
  }

  private static final class Projection implements TapeSink {
    private static final byte[] ID = {'i', 'd'};
    private static final byte[] DELTA = {'d', 'e', 'l', 't', 'a'};
    long hash, rows, compiled;
    long id, delta;
    int field;
    boolean object;

    void reset() { rows = compiled = 0; object = false; hash = SEED; }
    public void beginObject(long size) { object = true; }
    public void key(TapeText key) {
      field = key.utf8Equals(ID) ? 1 : key.utf8Equals(DELTA) ? 2 : 0;
    }
    public void longValue(long v) {
      if (field == 1) id = v;
      else if (field == 2) delta = v;
    }
    public void end() {
      if (object) {
        hash = fold(hash, id, delta);
        rows++;
        if (CompilerDirectives.inCompiledCode()) compiled++;
        object = false;
      }
    }
    public void beginArray(long n) {}
    public void booleanValue(boolean b) {}
    public void nullValue() { throw new AssertionError(); }
    public void stringValue(TapeText t) { throw new AssertionError(); }
    public void unsignedValue(long n) { throw new AssertionError(); }
    public void doubleValue(double d) { throw new AssertionError(); }
    public void decimalValue(TapeText t) { throw new AssertionError(); }
  }

  private static final class SpanRoot extends RootNode {
    private final TapeValue[] spans;
    private final int width, rows;
    private final Projection sink;
    int calls, lastCount;
    @Child private TapeExportNode export = TapeExportNode.create();

    SpanRoot(RecordLanguage language, TapeValue[] spans, int width, int rows, Projection sink) {
      super(language);
      this.spans = spans;
      this.width = width;
      this.rows = rows;
      this.sink = sink;
    }

    @Override public Object execute(VirtualFrame frame) {
      long first = (long) frame.getArguments()[0];
      long count = (long) frame.getArguments()[1];
      if (first < 0 || first >= rows || first % width != 0 || first != sink.rows
          || count != Math.min(width, rows - first)) return -1L;
      export.execute(spans[(int) (first / width)], sink);
      calls++;
      lastCount = (int) count;
      return sink.rows - first;
    }
  }

  private static final class PullSpans extends RootNode {
    private final int rows, width;
    @Child private DirectCallNode span;
    PullSpans(RecordLanguage language, int rows, int width, SpanRoot root) {
      super(language);
      this.rows = rows;
      this.width = width;
      span = DirectCallNode.create(root.getCallTarget());
    }
    @Override public Object execute(VirtualFrame frame) {
      long first = 0;
      while (first < rows) {
        long count = Math.min(width, rows - first);
        if ((long) span.call(first, count) != count) return -1L;
        first += count;
        TruffleSafepoint.poll(this);
      }
      return first;
    }
  }

  private static final class PullWhole extends RootNode {
    private final TapeValue document;
    private final Projection sink;
    @Child private TapeExportNode export = TapeExportNode.create();
    PullWhole(RecordLanguage language, TapeValue document, Projection sink) {
      super(language);
      this.document = document;
      this.sink = sink;
    }
    @Override public Object execute(VirtualFrame frame) {
      export.execute(document, sink);
      return sink.rows;
    }
  }

  private static void measure(TapeValue document, int rows, int width, String mode,
      RecordsFFM.LibraryBinding library) throws Throwable {
    var lang = RecordLanguage.CURRENT.get(null);
    var sink = new Projection();
    // Each view holds the original borrowed owner. No record or field is copied into the receipt.
    var spans = new TapeValue[Math.ceilDiv(rows, width)];
    int at = 0;
    for (var part : document.elements()) spans[at++] = part;
    if (at != spans.length) throw new AssertionError("source span count");
    var span = new SpanRoot(lang, spans, width, rows, sink);
    var direct = switch (mode) {
      case "whole" -> new PullWhole(lang, document, sink).getCallTarget();
      case "pull" -> new PullSpans(lang, rows, width, span).getCallTarget();
      case "push", "bound" -> null;
      default -> throw new IllegalArgumentException(mode);
    };
    long expected = SEED;
    for (int i = 0; i < rows; i++) expected = fold(expected, id(i), delta(i));
    // Confined callbacks reenter the thread that owns the session. Close the arena before with() returns.
    try (var arena = Arena.ofConfined()) {
      var target = span.getCallTarget();
      var callback = switch (mode) {
        case "push" -> RecordsTruffle.SpanUpcall.bindEntered(target, -1L, arena);
        case "bound" -> RecordsFFM.SpanUpcalls.createDirect(
            (first, count) -> (Long) target.call(first, count), -1L, arena);
        default -> null;
      };
      for (int round = -3; round < 12; round++) {
        sink.reset();
        span.calls = span.lastCount = 0;
        long start = System.nanoTime();
        long result = direct != null ? (long) direct.call() : library.deliver(callback, rows, width);
        long elapsed = System.nanoTime() - start;
        if (result != rows || sink.hash != expected || sink.rows != rows || (round >= 0 && sink.compiled != rows))
          throw new AssertionError(mode + " result=" + result + " rows=" + sink.rows
              + " compiled=" + sink.compiled + " hash=" + sink.hash + " expected=" + expected);
        if (!mode.equals("whole") && (span.calls != spans.length || span.lastCount != (rows - 1) % width + 1))
          throw new AssertionError("span count or tail");
        if (round >= 0) System.out.println(elapsed);
      }
      System.err.println("verified " + mode + " rows=" + rows + " compiled=" + sink.compiled
          + " spans=" + span.calls + " last=" + span.lastCount + " hash=" + expected);
    }
  }

  public static void main(String[] args) {
    int rows = Integer.parseInt(args[2]), width = Integer.parseInt(args[3]);
    if (rows < 1 || width < 1) throw new IllegalArgumentException("positive rows and width required");
    try (var context = Context.newBuilder("recordbench").allowExperimentalOptions(true)
        .option("engine.CompileImmediately", "true").option("engine.BackgroundCompilation", "false")
        .option("engine.MultiTier", "false").option("engine.CompilationFailureAction", "Throw").build();
        var arena = Arena.ofConfined(); var session = JsonSession.open()) {
      context.initialize("recordbench");
      context.enter();
      try {
        var library = new RecordsFFM.LibraryBinding(Path.of(args[0]), arena);
        session.with(dataset(rows, width), document -> {
          try {
            measure(document, rows, width, args[1], library);
          } catch (Throwable cause) {
            throw new AssertionError("record traversal failed", cause);
          }
          return null;
        });
      } finally { context.leave(); }
    }
  }
}
