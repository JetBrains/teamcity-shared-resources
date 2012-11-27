package jetbrains.buildServer.sharedResources.server;

import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.parameters.AbstractBuildParametersProvider;
import jetbrains.buildServer.serverSide.parameters.BuildParametersProvider;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static jetbrains.buildServer.sharedResources.server.SharedResourcesUtils.searchForFeature;

/**
 * Class {@code BuildFeatureParametersProvider}
 *
 * Exposes {@code SharedResourcesBuildFeature} parameters to build
 *
 * @see SharedResourcesBuildFeature
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class BuildFeatureParametersProvider extends AbstractBuildParametersProvider implements BuildParametersProvider {

  @NotNull
  @Override
  public Map<String, String> getParameters(@NotNull SBuild build, boolean emulationMode) {
    final Map<String, String> result = new HashMap<String, String>();
    final SBuildType buildType = build.getBuildType();
    if (buildType != null) {
      SBuildFeatureDescriptor myFeatureDescriptor = searchForFeature(buildType, false);
      if (myFeatureDescriptor != null) {
        myFeatureDescriptor = searchForFeature(buildType, true); // resolving params here
        if (myFeatureDescriptor != null) {
          final String serializedBuildParams = myFeatureDescriptor.getParameters().get(SharedResourcesPluginConstants.LOCKS_FEATURE_PARAM_KEY);
          result.putAll(SharedResourcesUtils.featureParamToBuildParams(serializedBuildParams));
        }
      }
    }
    return result;
  }
}
