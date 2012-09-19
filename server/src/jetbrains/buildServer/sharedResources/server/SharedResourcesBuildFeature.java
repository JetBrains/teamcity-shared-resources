package jetbrains.buildServer.sharedResources.server;

import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static com.intellij.openapi.util.text.StringUtil.isEmptyOrSpaces;
import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.RESOURCE_PARAM_KEY;

/**
 *
 * @author Oleg Rybak
 */
public class SharedResourcesBuildFeature extends BuildFeature {

  @NotNull
  private final PluginDescriptor myDescriptor;

  public SharedResourcesBuildFeature(@NotNull PluginDescriptor descriptor) {
    myDescriptor = descriptor;

  }

  @NotNull
  @Override
  public String getType() {
    return SharedResourcesPluginConstants.FEATURE_TYPE;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return "Shared Resources Management";
  }

  @Nullable
  @Override
  public String getEditParametersUrl() {
    return myDescriptor.getPluginResourcesPath(SharedResourcesPluginConstants.EDIT_FEATURE_PATH_HTML);
  }

  @Override
  public boolean isMultipleFeaturesPerBuildTypeAllowed() {
    return false;
  }

  @Override
  public Map<String, String> getDefaultParameters() {
    final Map<String, String> result = new HashMap<String, String>();
    result.put(RESOURCE_PARAM_KEY, "");
    result.put(SharedResourcesPluginConstants.BUILD_ID_KEY, "");
    return result;
  }

  @NotNull
  @Override
  public String describeParameters(@NotNull Map<String, String> params) {
    String resourceName = params.get(RESOURCE_PARAM_KEY);
    if (isEmptyOrSpaces(resourceName)) {
      return "Shared resource is not defined yet";
    } else {
      return "Adds shared resource [" + resourceName + "]";
    }
  }

  @Nullable
  @Override
  public PropertiesProcessor getParametersProcessor() {
    return new BuildFeatureParametersProcessor();
  }

}
