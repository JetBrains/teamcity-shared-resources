package jetbrains.buildServer.sharedResources;

import org.jetbrains.annotations.NotNull;

/**
 * @author Oleg Rybak
 */
public class SharedResourcesPluginConstants {

  public static final String FEATURE_TYPE = "JetBrains.SharedResources";

  public static final String RESOURCE_PARAM_KEY = "resource-name";

  public static final String EDIT_FEATURE_PATH_HTML = "editFeature.html";

  public static final String EDIT_FEATURE_PATH_JSP = "editFeature.jsp";

  @NotNull
  public String getResourceKey() {
    return RESOURCE_PARAM_KEY;
  }

}
