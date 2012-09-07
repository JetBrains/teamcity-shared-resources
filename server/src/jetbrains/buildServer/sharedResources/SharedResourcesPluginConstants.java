package jetbrains.buildServer.sharedResources;

import org.jetbrains.annotations.NotNull;

/**
 * @author Oleg Rybak
 */
public class SharedResourcesPluginConstants {

  public static final String FEATURE_TYPE = "JetBrains.SharedResources";

  public static final String RESOURCE_PARAM_KEY = "resource-name";

  @NotNull public String getResourceKey() {
    return RESOURCE_PARAM_KEY;
  }

}
