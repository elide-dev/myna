package dev.elide.myna.bench;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** Seam inputs shared by the benchmarks: repository examples plus a synthetic large module. */
final class Seams {
  private Seams() {}

  static String load(String name) {
    if (name.equals("synthetic")) return synthetic(128);
    try (InputStream in = Seams.class.getResourceAsStream("/seams/" + name + ".seam")) {
      if (in == null) throw new IllegalArgumentException("missing seam resource: " + name);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** A module with {@code count} imports and exports, a struct, and a callback per group. */
  static String synthetic(int count) {
    var out = new StringBuilder("module synthetic abi=1 target=x86_64-unknown-linux-gnu\n");
    out.append("opaque Handle\n");
    out.append(
        """
        struct Record size=24 align=8
          field data type=ptr<u8> offset=0
          field length type=u64 offset=8
          field flags type=u32 offset=16
        end
        callback Visitor return=i32
          param isolate type=isolate_thread
          param value type=i64
        end
        """);
    for (int i = 0; i < count; i++) {
      out.append("import read")
          .append(i)
          .append(" native_leaf=true@explicit_contract symbol=syn_read_")
          .append(i)
          .append(
              " return=i64 error=no_failure nounwind=true@explicit_contract"
                  + " willreturn=true@explicit_contract\n")
          .append(
              "  param value type=ptr<i64> nullable=false ownership=borrowed access=read"
                  + " nonnull=true@explicit_contract nocapture=true@explicit_contract"
                  + " readonly=true@explicit_contract align=8@explicit_contract"
                  + " dereferenceable=8@explicit_contract\n")
          .append("  param flags type=u32\n")
          .append("end\n");
      out.append("export write")
          .append(i)
          .append(" symbol=syn_write_")
          .append(i)
          .append(" return=i64 java=fixture.Synthetic.write")
          .append(i)
          .append(" isolate=thread error=integer_sentinel sentinel=-1\n")
          .append(
              "  param data type=ptr<u8> nullable=false ownership=borrowed access=read"
                  + " nonnull=true@explicit_contract\n")
          .append("  param length type=i64\n")
          .append("end\n");
    }
    return out.toString();
  }
}
