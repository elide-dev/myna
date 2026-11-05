package fixture.fast;
import java.util.List;
import org.graalvm.nativeimage.c.CContext;
public final class Directives implements CContext.Directives {
  public List<String> getLibraryPaths() { return List.of(System.getProperty("myna.fast.libraryPath")); }
}
