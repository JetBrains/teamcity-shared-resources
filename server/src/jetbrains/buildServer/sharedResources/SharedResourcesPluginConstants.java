/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.sharedResources;

import java.util.Comparator;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.server.project.ResourceProjectFeature;

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

  public static Comparator<String> RESOURCE_NAMES_COMPARATOR = String::compareToIgnoreCase;
}
