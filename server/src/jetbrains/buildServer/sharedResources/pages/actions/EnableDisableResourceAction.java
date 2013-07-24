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

package jetbrains.buildServer.sharedResources.pages.actions;

import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.pages.Messages;
import jetbrains.buildServer.sharedResources.pages.ResourceHelper;
import jetbrains.buildServer.sharedResources.server.exceptions.DuplicateResourceException;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.ControllerAction;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class EnableDisableResourceAction extends BaseResourceAction implements ControllerAction {

  public EnableDisableResourceAction(@NotNull final ProjectManager projectManager,
                                     @NotNull final Resources resources,
                                     @NotNull final ResourceHelper resourceHelper,
                                     @NotNull final Messages messages) {
    super(projectManager, resources, resourceHelper, messages);
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
    final String resourceName = request.getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_NAME);
    final String projectId = request.getParameter(SharedResourcesPluginConstants.WEB.PARAM_PROJECT_ID);
    final boolean newState = StringUtil.isTrue(request.getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_STATE));
    final SProject project = myProjectManager.findProjectById(projectId);
    if (project != null) {
      final Resource resource = myResources.asMap(projectId).get(resourceName);
      final Resource resourceInState = myResourceHelper.getResourceInState(resource, newState);
      try {
        myResources.editResource(projectId, resourceName, resourceInState);
      } catch (DuplicateResourceException e) {
        createNameError(ajaxResponse, resourceName);
      }
      project.persist();
      addMessage(request, "Resource " + resourceName + " was " + (newState ? "enabled" : "disabled"));
    } else {
      LOG.error("Project [" + projectId + "] no longer exists!");
    }
  }
}
