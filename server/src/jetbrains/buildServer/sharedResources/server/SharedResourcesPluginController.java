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

package jetbrains.buildServer.sharedResources.server;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.controllers.admin.projects.BuildFeaturesBean;
import jetbrains.buildServer.controllers.admin.projects.EditBuildTypeFormFactory;
import jetbrains.buildServer.controllers.admin.projects.EditableBuildTypeSettingsForm;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.settings.ProjectSettingsManager;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.pages.SharedResourcesBean;
import jetbrains.buildServer.sharedResources.settings.SharedResourcesProjectSettings;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.*;

/**
 * @author Oleg Rybak
 */
public class SharedResourcesPluginController extends BaseController {

  @NotNull
  private final PluginDescriptor myDescriptor;

  @NotNull
  private final EditBuildTypeFormFactory myFormFactory;

  @NotNull
  private ProjectSettingsManager myProjectSettingsManager;

  public SharedResourcesPluginController(
          @NotNull PluginDescriptor descriptor,
          @NotNull WebControllerManager web,
          @NotNull EditBuildTypeFormFactory formFactory,
          @NotNull ProjectSettingsManager projectSettingsManager
  ) {
    myDescriptor = descriptor;
    myFormFactory = formFactory;
    myProjectSettingsManager = projectSettingsManager;
    web.registerController(myDescriptor.getPluginResourcesPath(EDIT_FEATURE_PATH_HTML), this);

  }

  @Nullable
  @Override
  protected ModelAndView doHandle(@NotNull HttpServletRequest request,
                                  @NotNull HttpServletResponse response) throws Exception {
    final ModelAndView result = new ModelAndView(myDescriptor.getPluginResourcesPath(EDIT_FEATURE_PATH_JSP));
    final EditableBuildTypeSettingsForm form = myFormFactory.getOrCreateForm(request);
    final BuildFeaturesBean buildFeaturesBean = form.getBuildFeaturesBean();
    final String myLocksString = buildFeaturesBean.getPropertiesBean().getProperties().get(LOCKS_FEATURE_PARAM_KEY);
    final SProject project = form.getProject();

    final SharedResourcesProjectSettings settings = (SharedResourcesProjectSettings) myProjectSettingsManager.getSettings(project.getProjectId(), SERVICE_NAME);
    final SharedResourcesBean bean = new SharedResourcesBean(settings.getResources(), Collections.<String, Set<SBuildType>>emptyMap()); // todo: add constructor without usage map
    final List<Lock> locks = SharedResourcesUtils.getLocks(myLocksString);
    result.getModel().put("locks", locks);
    result.getModel().put("bean", bean);
    result.getModel().put("project", project);
    return result;
  }


}
