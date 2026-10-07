package fixture.bemo;

import java.util.List;
import org.graalvm.nativeimage.c.CContext;

public final class BemoDirectives implements CContext.Directives {
  public List<String> getLibraryPaths() {
    return List.of(System.getProperty("svmgen.bemo.libraryPath"));
  }
}
