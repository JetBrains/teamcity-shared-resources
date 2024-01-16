

package jetbrains.buildServer.sharedResources.server;

import java.util.Map;
import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.server.feature.FeatureParams;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Oleg Rybak
 */
public class SharedResourcesBuildFeature extends BuildFeature {

  public static final String FEATURE_TYPE = "JetBrains.SharedResources";

  @NotNull
  private final PluginDescriptor myDescriptor;

  @NotNull
  private final FeatureParams myFeatureParams;

  public SharedResourcesBuildFeature(@NotNull final PluginDescriptor descriptor,
                                     @NotNull final FeatureParams featureParams) {
    myDescriptor = descriptor;
    myFeatureParams = featureParams;
  }

  @NotNull
  @Override
  public String getType() {
    return FEATURE_TYPE;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return "Shared Resources";
  }

  @Nullable
  @Override
  public String getEditParametersUrl() {
    return myDescriptor.getPluginResourcesPath(SharedResourcesPluginConstants.EDIT_FEATURE_PATH_HTML);
  }

  @Override
  public boolean isMultipleFeaturesPerBuildTypeAllowed() {
    return true;
  }

  @NotNull
  @Override
  public String describeParameters(@NotNull final Map<String, String> params) {
    return myFeatureParams.describeParams(params);
  }

  @Nullable
  @Override
  public Map<String, String> getDefaultParameters() {
    return myFeatureParams.getDefault();
  }

  @Override
  public boolean isRequiresAgent() {
    return !TeamCityProperties.getBooleanOrTrue(SharedResourcesPluginConstants.RESOURCES_IN_CHAINS_ENABLED);
  }
}