package jetbrains.buildServer.sharedResources.server;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.intellij.openapi.util.text.StringUtil.isEmptyOrSpaces;

/**
 *
 * @author Oleg Rybak
 */
public class SharedResourcesBuildFeature extends BuildFeature {

  private static final Logger LOG = Logger.getInstance(SharedResourcesBuildFeature.class.getName());

  @NotNull
  private final PluginDescriptor myDescriptor;

  @NotNull
  private final SharedResourcesFeatureContext myContext;

  public SharedResourcesBuildFeature(
          @NotNull PluginDescriptor descriptor,
          @NotNull SharedResourcesFeatureContext context) {
    myDescriptor = descriptor;
    myContext = context;
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
    return myDescriptor.getPluginResourcesPath("editFeature.html");
  }

  @Override
  public boolean isMultipleFeaturesPerBuildTypeAllowed() {
    return true;
  }

  @Override
  public Map<String, String> getDefaultParameters() {
    return Collections.singletonMap(new SharedResourcesPluginConstants().getResourceKey(), "");
  }

  @NotNull
  @Override
  public String describeParameters(@NotNull Map<String, String> params) {
    String resourceName = params.get(SharedResourcesPluginConstants.RESOURCE_PARAM_KEY);
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

      final SharedResourcesPluginConstants c = new SharedResourcesPluginConstants();

      final Set<String> values = myContext.getNames();

      private void checkNotEmpty(@NotNull final Map<String, String> properties,
                                 @NotNull final String key,
                                 @NotNull final String message,
                                 @NotNull final Collection<InvalidProperty> res) {
        if (isEmptyOrSpaces(properties.get(key))) {
          res.add(new InvalidProperty(key, message));
        }
      }

      private void checkUnique(@NotNull final Map<String, String> properties,
                               @NotNull final String key,
                               @NotNull final String message,
                               @NotNull final Collection<InvalidProperty> res) {
        if (values.contains(properties.get(key))) {
          res.add(new InvalidProperty(key, message));
        }
      }

      public Collection<InvalidProperty> process(Map<String, String> properties) {
        final Collection<InvalidProperty> result = new ArrayList<InvalidProperty>();
        if (properties != null) {
          checkNotEmpty(properties, c.getResourceKey(), "Resource name must not be empty", result);
          checkUnique(properties, c.getResourceKey(), "Resource name is already used in current configuration", result);
        }
        return result;
      }

    };
  }


}
