

package jetbrains.buildServer.sharedResources.server.feature;

import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public interface SharedResourcesFeatureFactory {

  /**
   * Wraps descriptor into {@code SharedResourcesFeature}
   * @param descriptor descriptor to wrap
   * @return shared resources feature
   */
  @NotNull
  SharedResourcesFeature createFeature(@NotNull final SBuildFeatureDescriptor descriptor);


}