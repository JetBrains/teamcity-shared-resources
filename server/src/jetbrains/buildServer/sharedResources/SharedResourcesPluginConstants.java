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

/**
 * @author Oleg Rybak
 */
public class SharedResourcesPluginConstants {

  /** Virtual path of the feature parameters page */
  public static final String EDIT_FEATURE_PATH_HTML = "editFeature.html";

  /** Page, responsible for feature parameters */
  public static final String EDIT_FEATURE_PATH_JSP = "editFeature.jsp";

  /** Name of the plugin */
  public static final String PLUGIN_NAME = "JetBrains.SharedResources";

  /** Name of the service. Used in project settings factory */
  public static final String SERVICE_NAME = "JetBrains.SharedResources";

  /**
   * Contains constants for web page - controller interaction
   */
  public interface WEB {
    public static final String ACTION_ADD = "/sharedResourcesAdd.html";
    public static final String ACTION_EDIT = "/sharedResourcesEdit.html";
    public static final String ACTION_DELETE = "/sharedResourcesDelete.html";


    public static final String PARAM_PROJECT_ID = "project_id";
    public static final String PARAM_RESOURCE_NAME = "resource_name";
    public static final String PARAM_RESOURCE_QUOTA = "resource_quota";
    public static final String PARAM_OLD_RESOURCE_NAME = "old_resource_name";

    //public static final String PARAM_VALUE_TYPE = "value_type";


    // quota used -> value is quota;
    // quota is not used -> value is -1;
    public static final String VALUE_TYPE_QUOTA = "quota";
    public static final String VALUE_TYPE_CUSTOM = "custom";


  }
}
