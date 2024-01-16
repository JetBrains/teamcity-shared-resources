

package jetbrains.buildServer.sharedResources.server;

import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.parameters.AbstractBuildParametersProvider;
import jetbrains.buildServer.serverSide.parameters.BuildParametersProvider;
import jetbrains.buildServer.sharedResources.server.feature.Locks;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.sharedResources.server.runtime.LocksStorage;
import org.jetbrains.annotations.NotNull;


/**
 * Class {@code BuildFeatureParametersProvider}
 *
 * Exposes {@code SharedResourcesBuildFeature} parameters to build
 *
 * @see jetbrains.buildServer.sharedResources.server.SharedResourcesBuildFeature
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class BuildFeatureParametersProvider extends AbstractBuildParametersProvider implements BuildParametersProvider {

  @NotNull
  private final SharedResourcesFeatures myFeatures;

  @NotNull
  private final LocksStorage myStorage;

  @NotNull
  private final Locks myLocks;

  public BuildFeatureParametersProvider(@NotNull final SharedResourcesFeatures features,
                                        @NotNull final Locks locks,
                                        @NotNull final LocksStorage storage) {
    myFeatures = features;
    myLocks = locks;
    myStorage = storage;
  }

  @NotNull
  @Override
  public Map<String, String> getParameters(@NotNull final SBuild build, boolean emulationMode) {
    if (emulationMode || !build.getBuildPromotion().isCompositeBuild()) {
      return getParametersFromFeatures(build);
    } else {
      return myLocks.asBuildParameters(myStorage.load(build.getBuildPromotion()).values());
    }
  }

  private Map<String, String> getParametersFromFeatures(@NotNull final SBuild build) {
    final Map<String, String> result = new HashMap<>();
    final SBuildType buildType = build.getBuildType();
    if (buildType != null) {
      myLocks.fromBuildFeaturesAsMap(myFeatures.searchForFeatures(buildType))
             .values()
             .forEach(lock -> result.put(myLocks.asBuildParameter(lock), lock.getValue()));
    }
    return result;
  }
}