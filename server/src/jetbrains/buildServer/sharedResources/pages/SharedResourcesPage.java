/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package jetbrains.buildServer.sharedResources.pages;

import jetbrains.buildServer.controllers.admin.projects.EditProjectTab;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.settings.ProjectSettingsManager;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.server.SharedResourcesUtils;
import jetbrains.buildServer.sharedResources.settings.SharedResourcesProjectSettings;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.SERVICE_NAME;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class SharedResourcesPage extends EditProjectTab {

  private ProjectSettingsManager myProjectSettingsManager;

  public SharedResourcesPage(@NotNull PagePlaces pagePlaces, @NotNull ProjectManager projectManager, @NotNull PluginDescriptor descriptor, @NotNull ProjectSettingsManager projectSettingsManager) {
    super(pagePlaces, SharedResourcesPluginConstants.PLUGIN_NAME, descriptor.getPluginResourcesPath("projectPage.jsp"), "Shared Resources", projectManager);
    myProjectSettingsManager = projectSettingsManager;
    addCssFile("/css/admin/buildTypeForm.css");
  }

  @Override
  public void fillModel(@NotNull Map<String, Object> model, @NotNull HttpServletRequest request) {
    super.fillModel(model, request);
    SharedResourcesBean bean;
    final SProject project = getProject(request);
    if (project != null) {
      final List<SBuildType> buildTypes = project.getBuildTypes();
      // map<ResourceName => Set <buildTypeName>>
      Map<String, Set<SBuildType>> usageMap = new HashMap<String, Set<SBuildType>>();
      for (SBuildType type: buildTypes) {
        final SBuildFeatureDescriptor descriptor = SharedResourcesUtils.searchForFeature(type, false);
        if (descriptor != null) {
          // we have feature. now:
          // 1) get locks
          final Map<String, String> parameters = descriptor.getParameters();
          final String locksString = parameters.get(SharedResourcesPluginConstants.LOCKS_FEATURE_PARAM_KEY);
          final Map<String, Lock> lockMap = SharedResourcesUtils.getLocksMap(locksString); // todo: map or simple list? do we need actual locks here?
          for (String str: lockMap.keySet()) {
            if (usageMap.get(str) == null) {
              usageMap.put(str, new HashSet<SBuildType>());
            }
            usageMap.get(str).add(type);
          }
        }
      }
      // todo: add associations separately for readLocks and writeLocks

      final String projectId = project.getProjectId();
      final SharedResourcesProjectSettings settings = (SharedResourcesProjectSettings) myProjectSettingsManager.getSettings(projectId, SERVICE_NAME);
      bean = new SharedResourcesBean(settings.getResources(), usageMap);
    } else {
      bean = new SharedResourcesBean(Collections.<Resource>emptyList(), Collections.<String, Set<SBuildType>>emptyMap()); // todo: how to differentiate error vs no resources??!
    }
    model.put("bean", bean);



  }
}
