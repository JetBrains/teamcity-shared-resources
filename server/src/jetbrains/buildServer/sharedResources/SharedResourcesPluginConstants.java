

package jetbrains.buildServer.sharedResources;

import java.util.Comparator;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import org.jetbrains.annotations.NotNull;

/**
 * @author Oleg Rybak
 */
public class SharedResourcesPluginConstants {

  /**
   * Path of the feature parameters page
   */
  public static final String EDIT_FEATURE_PATH_HTML = "editFeature.html";

  /**
   * Page, responsible for feature parameters
   */
  public static final String EDIT_FEATURE_PATH_JSP = "editFeature.jsp";

  /**
   * Name of the plugin
   */
  public static final String PLUGIN_NAME = "JetBrains.SharedResources";

  /**
   * Base directory for created artifacts
   */
  public static final String BASE_ARTIFACT_PATH = ".teamcity/" + PLUGIN_NAME;

  /**
   * Name of the service. Used in project settings factory
   */
  public static final String FEATURE_TYPE = "JetBrains.SharedResources";

  /**
   * Contains constants for web page - controller interaction
   */
  public interface WEB {

    String ACTIONS = "/sharedResourcesActions.html";

    String PARAM_PROJECT_ID = "project_id";
    String PARAM_OLD_RESOURCE_NAME = "old_resource_name";

    String PARAM_RESOURCE_NAME = "resource_name";
    String PARAM_RESOURCE_TYPE = "resource_type";
    String PARAM_RESOURCE_STATE = "resource_state";
    String PARAM_RESOURCE_ID = "resource_id";

    String PARAM_RESOURCE_VALUES = "resource_values";
    String PARAM_RESOURCE_QUOTA = "resource_quota";

    String ACTION_MESSAGE_KEY = "resourceActionResultMessage";
  }

  public interface ProjectFeatureParameters {
    String NAME = "name";
    String TYPE = "type";
    String QUOTA = "quota";
    String VALUES = "values";
    String ENABLED = "enabled";
  }

  public static final Comparator<String> RESOURCE_NAMES_COMPARATOR = String::compareToIgnoreCase;

  public static final Comparator<Resource> RESOURCE_BY_NAME_COMPARATOR = (rc1, rc2) -> rc1.getName().compareToIgnoreCase(rc2.getName());

  public static final String RESOURCES_IN_CHAINS_ENABLED = "teamcity.sharedResources.buildChains.enabled";

  public static String getReservedResourceAttributeKey(@NotNull final String resourceId) {
    return "teamcity.sharedResources." + resourceId;
  }

}