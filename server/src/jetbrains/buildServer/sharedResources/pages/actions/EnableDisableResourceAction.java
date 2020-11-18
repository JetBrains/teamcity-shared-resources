/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.serverSide.ConfigActionFactory;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.pages.Messages;
import jetbrains.buildServer.sharedResources.pages.ResourceHelper;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.sharedResources.server.project.ResourceProjectFeature;
import jetbrains.buildServer.sharedResources.server.project.ResourceProjectFeatures;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.ControllerAction;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class EnableDisableResourceAction extends BaseResourceAction implements ControllerAction {

  public EnableDisableResourceAction(@NotNull final ProjectManager projectManager,
                                     @NotNull final ResourceProjectFeatures projectFeatures,
                                     @NotNull final ResourceHelper resourceHelper,
                                     @NotNull final Messages messages,
                                     @NotNull final ConfigActionFactory configActionFactory,
                                     @NotNull final Resources resources) {
    super(projectManager, projectFeatures, resourceHelper, messages, configActionFactory, resources);
  }

  @NotNull
  @Override
  public String getActionName() {
    return "enableDisableResource";
  }

  @Override
  protected void doProcess(@NotNull final HttpServletRequest request,
                           @NotNull final HttpServletResponse response,
                           @NotNull final Element ajaxResponse) {
    final String resourceId = request.getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_ID);
    final String projectId = request.getParameter(SharedResourcesPluginConstants.WEB.PARAM_PROJECT_ID);
    final boolean newState = StringUtil.isTrue(request.getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_STATE));
    final SProject project = myProjectManager.findProjectById(projectId);
    if (project != null) {
      ResourceProjectFeature feature = myProjectFeatures.getOwnFeatures(project).stream()
                                                        .filter(rf -> rf.getId().equals(resourceId))
                                                        .findFirst()
                                                        .orElse(null);
      if (feature != null && feature.getResource() != null) {
        final Resource resource = feature.getResource();
        final Resource resourceInState = myResourceHelper.getResourceInState(projectId, resource, newState);
        myProjectFeatures.updateFeature(project, resourceInState.getId(), resourceInState.getParameters());
        final String changed = newState ? "enabled" : "disabled";
        project.schedulePersisting(myConfigActionFactory.createAction(project, "'" + resource.getName() + "' shared resource was " + changed));
        addMessage(request, "Resource " + resource.getName() + " was " + changed);
      }
    } else {
      LOG.error("Project [" + projectId + "] no longer exists!");
    }
  }
}
