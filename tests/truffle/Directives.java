// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package fixture.cells;
import java.util.List;
import org.graalvm.nativeimage.c.CContext;
public final class Directives implements CContext.Directives {
  public List<String> getLibraryPaths() { return List.of(System.getProperty("myna.cells.libraryPath")); }
  /** The generated header, for the {@code @CStruct} records; see tests/directives. */
  public List<String> getHeaderFiles() { return List.of("\"" + System.getProperty("myna.seam.header") + "\""); }
}
