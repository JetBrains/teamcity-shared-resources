package jetbrains.buildServer.sharedResources.server;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created with IntelliJ IDEA.
 * Date: 07.09.12
 * Time: 15:24
 *
 * @author Oleg Rybak
 */
public class SharedResourcesFeatureContext {

  private final ConcurrentMap<String, Set<String>> context = new ConcurrentHashMap<String, Set<String>>();

  public void putNamesInContext(String buildId, Set<String> resourceNames) {
    context.put(buildId, resourceNames);
  }

  public Set<String> getNames(String buildId) {
    return Collections.unmodifiableSet(context.get(buildId));
  }

  public void clear(String buildId) {
    context.remove(buildId);
  }

}
