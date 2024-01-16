

package jetbrains.buildServer.sharedResources.server.project;

import jetbrains.buildServer.sharedResources.model.resources.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Oleg Rybak <oleg.rybak@jetbrains.com>
 */
public interface ResourceProjectFeature {

  @NotNull
  String getId();

  @Nullable
  Resource getResource();

}