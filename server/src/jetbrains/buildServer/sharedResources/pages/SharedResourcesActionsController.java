/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

import jetbrains.buildServer.controllers.BaseAjaxActionController;
import jetbrains.buildServer.serverSide.ConfigActionFactory;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.pages.actions.AddResourceAction;
import jetbrains.buildServer.sharedResources.pages.actions.DeleteResourceAction;
import jetbrains.buildServer.sharedResources.pages.actions.EditResourceAction;
import jetbrains.buildServer.sharedResources.pages.actions.EnableDisableResourceAction;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.sharedResources.server.project.ResourceProjectFeatures;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class SharedResourcesActionsController extends BaseAjaxActionController {

  public SharedResourcesActionsController(@NotNull final WebControllerManager controllerManager,
                                          @NotNull final ProjectManager projectManager,
                                          @NotNull final ResourceProjectFeatures projectFeatures,
                                          @NotNull final ResourceHelper resourceHelper,
                                          @NotNull final SharedResourcesFeatures features,
                                          @NotNull final Messages messages,
                                          @NotNull final ConfigActionFactory configActionFactory) {
    super(controllerManager);
    controllerManager.registerController(SharedResourcesPluginConstants.WEB.ACTIONS, this);
    registerAction(new AddResourceAction(projectManager, projectFeatures, resourceHelper, messages, configActionFactory));
    registerAction(new DeleteResourceAction(projectManager, projectFeatures, resourceHelper, messages, configActionFactory));
    registerAction(new EditResourceAction(projectManager, projectFeatures, features, resourceHelper, messages, configActionFactory));
    registerAction(new EnableDisableResourceAction(projectManager, projectFeatures, resourceHelper, messages, configActionFactory));
  }
}
