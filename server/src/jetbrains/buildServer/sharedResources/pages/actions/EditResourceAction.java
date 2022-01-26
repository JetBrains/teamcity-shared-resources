/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

package jetbrains.buildServer.sharedResources.pages.actions;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.pages.Messages;
import jetbrains.buildServer.sharedResources.pages.ResourceHelper;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeature;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.sharedResources.server.project.ResourceProjectFeatures;
import jetbrains.buildServer.web.openapi.ControllerAction;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

/**
 * Class {@code EditResourceAction}
 *
 * Defines action for editing resources. Supports changing name and type
 * as well as updating all build configurations that use resource being edited
 *
 * @see BaseResourceAction
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public final class EditResourceAction extends BaseResourceAction implements ControllerAction {

  @NotNull
  private final SharedResourcesFeatures myBuildFeatures;

  public EditResourceAction(@NotNull final ProjectManager projectManager,
                            @NotNull final ResourceProjectFeatures projectFeatures,
                            @NotNull final SharedResourcesFeatures buildFeatures,
                            @NotNull final ResourceHelper resourceHelper,
                            @NotNull final Messages messages,
                            @NotNull final ConfigActionFactory configActionFactory,
                            @NotNull final Resources resources) {
    super(projectManager, projectFeatures, resourceHelper, messages, configActionFactory, resources);
    myBuildFeatures = buildFeatures;
  }

  @NotNull
  @Override
  public String getActionName() {
    return "editResource";
  }

  @Override
  protected void doProcess(@NotNull final HttpServletRequest request,
                           @NotNull final HttpServletResponse response,
                           @NotNull final Element ajaxResponse) {
    final String oldName = request.getParameter(SharedResourcesPluginConstants.WEB.PARAM_OLD_RESOURCE_NAME);
    final String projectId = request.getParameter(SharedResourcesPluginConstants.WEB.PARAM_PROJECT_ID);
    final SProject project = myProjectManager.findProjectById(projectId);
    if (project != null) {
      final Resource resource = myResourceHelper.getResourceFromRequest(projectId, request);
      if (resource != null) {
        final String newName = resource.getName();
        boolean changedName = !newName.equals(oldName);
        // check if there is already resource with such name
        if (changedName && containsDuplicateName(project, newName)) {
          createNameError(ajaxResponse, newName);
          return;
        }
        boolean selfPersisted = false;
        myProjectFeatures.updateFeature(project, resource.getId(), resource.getParameters());
        ConfigAction cause = myConfigActionFactory.createAction(project, "'" + resource.getName() + "' shared resource was updated");
        if (changedName) {
          // my resource can be used only in my build configurations or in build configurations in my subtree
          final List<SProject> projects = project.getProjects();
          projects.add(project);
          for (SProject p: projects) {
            boolean updated = false;
            final List<BuildTypeSettings> containers = getSettingsContainers(p);
            for (BuildTypeSettings settings: containers) {
              for (SharedResourcesFeature feature: myBuildFeatures.searchForFeatures(settings)) {
                updated = feature.updateLock(settings, oldName, newName) || updated;
              }
            }
            if (updated) {
              p.schedulePersisting(cause);
              if (!selfPersisted && p.equals(project)) { // make sure we persist current project even if no resource usages were detected
                selfPersisted = true;
              }
            }
          }
          if (!selfPersisted) {
            project.schedulePersisting(cause);
          }
        } else { // just persist project so that settings will be saved
          project.schedulePersisting(cause);
        }
        addMessage(request, "Resource " + newName + " was updated");
      }
    }
  }

  private List<BuildTypeSettings> getSettingsContainers(@NotNull final SProject project) {
    List<BuildTypeSettings> result = new ArrayList<>();
    result.addAll(project.getOwnBuildTypes());
    result.addAll(project.getOwnBuildTypeTemplates());
    return result;
  }
}
