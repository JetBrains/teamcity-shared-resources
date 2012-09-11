package jetbrains.buildServer.sharedResources.server;

import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.util.FeatureUtil;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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

    return new PropertiesProcessor() {

      private final String ERROR_EMPTY = "Resource name must not be empty";

      private final String ERROR_NON_UNIQUE = "Non unique resource names found";

      private final SharedResourcesPluginConstants c = new SharedResourcesPluginConstants();

      private void checkNotEmpty(@NotNull final Collection<String> strings,
                                 @NotNull final Collection<InvalidProperty> res) {
        if (strings.isEmpty()) {
          res.add(new InvalidProperty(RESOURCE_PARAM_KEY, ERROR_EMPTY));
        }
      }

      private void checkUnique(@NotNull final Collection<String> strings,
                               @NotNull final Collection<InvalidProperty> res) {
        Set<String> set = new HashSet<String>(strings.size());
        set.addAll(strings);
        if (set.size() != strings.size()) {
          res.add(new InvalidProperty(RESOURCE_PARAM_KEY, ERROR_NON_UNIQUE));
        }
      }

      public Collection<InvalidProperty> process(Map<String, String> properties) {
        final Collection<InvalidProperty> result = new ArrayList<InvalidProperty>();
        if (properties != null) {
          final Collection<String> resourceNames = FeatureUtil.toCollection(properties.get(RESOURCE_PARAM_KEY));
          checkNotEmpty(resourceNames, result);
          checkUnique(resourceNames, result);
          properties.put(RESOURCE_PARAM_KEY, FeatureUtil.fromCollection(resourceNames));
        }
        return result;
      }
    };
  }

}
