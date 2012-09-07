package jetbrains.buildServer.sharedResources.server;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * Date: 07.09.12
 * Time: 15:24
 *
 * @author Oleg Rybak
 */
public class SharedResourcesFeatureContext {

  @NotNull
  private final Set<String> resourceNames = new HashSet<String>();

  public void putNamesInContext(Collection<String> names) {
    resourceNames.clear();
    resourceNames.addAll(names);
  }

  public Set<String> getNames() {
    return Collections.unmodifiableSet(resourceNames);
  }

}
