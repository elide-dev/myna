// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package fixture.header;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * On HotSpot: the generated context writes seam.h once and reports it after the consumer's headers.
 */
public final class DirectivesProbe {
  private DirectivesProbe() {}

  public static void main(String[] args) throws Exception {
    List<String> first = new HeaderApiDirectives().getHeaderFiles();
    List<String> second = new HeaderApiDirectives().getHeaderFiles();
    if (first.size() != 1 || !first.equals(second)) throw new AssertionError(first + " " + second);
    String include = first.get(0);
    Path header = Path.of(include.substring(1, include.length() - 1));
    if (!include.startsWith("\"") || !header.isAbsolute()) throw new AssertionError(include);
    if (!header.getParent().getFileName().toString().startsWith("myna-seam"))
      throw new AssertionError(header);
    if (!Files.readString(header).equals(Files.readString(Path.of(args[0]))))
      throw new AssertionError("written header differs from " + args[0]);
    System.out.println("header " + header);
  }
}
