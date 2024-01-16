

package jetbrains.buildServer.sharedResources.server.feature;

import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public final class SharedResourcesFeatureFactoryImpl implements SharedResourcesFeatureFactory {

  @NotNull
  private final Locks myLocks;

  public SharedResourcesFeatureFactoryImpl(@NotNull final Locks locks) {
    myLocks = locks;
  }

  @NotNull
  @Override
  public SharedResourcesFeature createFeature(@NotNull final SBuildFeatureDescriptor descriptor) {
    return new SharedResourcesFeatureImpl(myLocks, descriptor);
  }
}