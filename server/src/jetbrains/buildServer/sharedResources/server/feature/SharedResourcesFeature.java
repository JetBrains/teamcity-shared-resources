

package jetbrains.buildServer.sharedResources.server.feature;

import java.util.Map;
import jetbrains.buildServer.serverSide.BuildTypeSettings;
import jetbrains.buildServer.sharedResources.model.Lock;
import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public interface SharedResourcesFeature {

  /**
   * Gets locked resources from current build feature
   *
   * @return map of locked resources. Map format is {@code <LockName, Lock>}
   */
  @NotNull
  Map<String, Lock> getLockedResources();

  /**
   * Updates lock inside build feature
   *
   * @param settings build type or template
   * @param oldName name of the existing lock
   * @param newName new lock name
   * @return {@code true} if lock was updated (i.e. there were usages of resource), {@code false} otherwise
   *
   */
  boolean updateLock(@NotNull final BuildTypeSettings settings,
                     @NotNull final String oldName,
                     @NotNull final String newName);
  
}