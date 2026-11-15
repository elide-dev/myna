// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package fixture.bemo;

import java.util.List;
import org.graalvm.nativeimage.c.CContext;

/**
 * Bemo's own C context. The generated {@code BemoImportsDirectives} delegates to it and adds the
 * generated seam.h after these headers; {@code context.h} fails the C query unless the macro, the
 * option and the header all arrive, and arrive before seam.h.
 */
public final class BemoDirectives implements CContext.Directives {
  public List<String> getLibraryPaths() {
    return List.of(System.getProperty("myna.bemo.libraryPath"));
  }

  public List<String> getHeaderFiles() {
    return List.of("\"" + System.getProperty("myna.bemo.contextHeader") + "\"");
  }

  public List<String> getMacroDefinitions() {
    return List.of("BEMO_CONTEXT_MACRO");
  }

  public List<String> getOptions() {
    return List.of("-DBEMO_CONTEXT_OPTION=1");
  }
}
