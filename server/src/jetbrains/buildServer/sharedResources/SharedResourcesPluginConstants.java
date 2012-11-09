package jetbrains.buildServer.sharedResources;

/**
 * @author Oleg Rybak
 */
public class SharedResourcesPluginConstants {

  public static final String FEATURE_TYPE = "JetBrains.SharedResources";

  public static final String EDIT_FEATURE_PATH_HTML = "editFeature.html";

  public static final String EDIT_FEATURE_PATH_JSP = "editFeature.jsp";

  public static final String LOCK_PREFIX = "teamcity.locks.";

  public static final String ATTR_READ_LOCKS = "readLocks";

  public static final String ATTR_WRITE_LOCKS = "writeLocks";

  /**
   * Key in feature parameters collection, that contains all locks
   */
  public static final String LOCKS_FEATURE_PARAM_KEY = "LOCKS";

  public String getLocksFeatureParamKey() {
    return LOCKS_FEATURE_PARAM_KEY;
  }

}
