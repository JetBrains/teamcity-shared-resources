package jetbrains.buildServer.sharedResources;

/**
 * @author Oleg Rybak
 */
public class SharedResourcesPluginConstants {

  public static final String FEATURE_TYPE = "JetBrains.SharedResources";

  /** Virtual path of the feature parameters page */
  public static final String EDIT_FEATURE_PATH_HTML = "editFeature.html";

  /** Page, responsible for feature parameters */
  public static final String EDIT_FEATURE_PATH_JSP = "editFeature.jsp";

  /** Page, responsible for resource management */
  public static final String EDIT_RESOURCES = "editResources.jsp";

  /** Lock prefix, used in build parameters */
  public static final String LOCK_PREFIX = "teamcity.locks.";

  /**
   * Key in feature parameters collection, that contains all locks
   */
  public static final String LOCKS_FEATURE_PARAM_KEY = "locks-param";

  public String getLocksFeatureParamKey() {
    return LOCKS_FEATURE_PARAM_KEY;
  }

  /** Name of the plugin */
  public static final String PLUGIN_NAME = "JetBrains.SharedResources";

  /** Name of the service. Used in project settings factory */
  public static final String SERVICE_NAME = "JetBrains.SharedResources";

}
