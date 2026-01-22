

package jetbrains.buildServer.sharedResources.server.report;

import java.util.Collection;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import org.jetbrains.annotations.NotNull;

/**
 * Represents {@code Resource} used by the build
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class UsedResource {

  @NotNull
  private final Resource myResource;

  @NotNull
  private final Collection<Lock> myLocks;

  UsedResource(@NotNull final Resource resource,
               @NotNull final Collection<Lock> locks) {
    myResource = resource;
    myLocks = locks;
  }

  @NotNull
  public Resource getResource() {
    return myResource;
  }

  @NotNull
  public Collection<Lock> getLocks() {
    return myLocks;
  }
}