// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package fixture.directives;

import java.util.List;
import org.graalvm.nativeimage.c.CContext;

/**
 * The C context of fixtures that name none of their own: {@code @CStruct} records read their
 * layout from the generated {@code seam.h}, passed to the image build as {@code -Dmyna.seam.header}.
 * Temporary until the generator supplies its own directives class.
 */
public final class SeamHeaderDirectives implements CContext.Directives {
  public List<String> getHeaderFiles() {
    return List.of("\"" + System.getProperty("myna.seam.header") + "\"");
  }
}
