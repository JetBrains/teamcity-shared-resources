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

package jetbrains.buildServer.sharedResources.pages;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.controllers.admin.projects.BuildFeatureBean;
import jetbrains.buildServer.controllers.admin.projects.BuildFeaturesBean;
import jetbrains.buildServer.controllers.admin.projects.EditBuildTypeFormFactory;
import jetbrains.buildServer.controllers.admin.projects.EditableBuildTypeSettingsForm;
import jetbrains.buildServer.serverSide.BuildTypeSettings;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.server.SharedResourcesBuildFeature;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeature;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatureFactory;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.EDIT_FEATURE_PATH_HTML;
import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.EDIT_FEATURE_PATH_JSP;

/**
 * @author Oleg Rybak
 */
public class EditFeatureController extends BaseController {

  @NotNull
  private final PluginDescriptor myDescriptor;

  @NotNull
  private final EditBuildTypeFormFactory myFormFactory;

  @NotNull
  private final Resources myResources;

  @NotNull
  private final SharedResourcesFeatureFactory myFactory;


  public EditFeatureController(@NotNull final PluginDescriptor descriptor,
                               @NotNull final WebControllerManager web,
                               @NotNull final EditBuildTypeFormFactory formFactory,
                               @NotNull final Resources resources,
                               @NotNull final SharedResourcesFeatureFactory factory) {
    myDescriptor = descriptor;
    myFormFactory = formFactory;
    myResources = resources;
    myFactory = factory;
    web.registerController(myDescriptor.getPluginResourcesPath(EDIT_FEATURE_PATH_HTML), this);

  }

  @Nullable
  @Override
  protected ModelAndView doHandle(@NotNull final HttpServletRequest request,
                                  @NotNull final HttpServletResponse response) throws Exception {
    final ModelAndView result = new ModelAndView(myDescriptor.getPluginResourcesPath(EDIT_FEATURE_PATH_JSP));
    final EditableBuildTypeSettingsForm form = myFormFactory.getOrCreateForm(request);
    assert form != null;
    final BuildTypeSettings buildTypeSettings = form.getSettings();
    assert buildTypeSettings != null;
    final SProject project = form.getProject();
    final BuildFeaturesBean buildFeaturesBean = form.getBuildFeaturesBean();
    final String buildFeatureId = request.getParameter("featureId");
    final Map<String, Lock> locks = new HashMap<String, Lock>();
    final String projectId = project.getProjectId();
    // map of all visible resources from this project and its subtree
    final Map<String, Resource> resources = new HashMap<String, Resource>(myResources.asMap(projectId));
    final Map<String, Object> model = result.getModel();
    final Collection<Lock> invalidLocks = new ArrayList<Lock>();
    boolean inherited = false;
    for (BuildFeatureBean bfb : buildFeaturesBean.getBuildFeatureDescriptors()) {
      SBuildFeatureDescriptor descriptor = bfb.getDescriptor();
      if (SharedResourcesBuildFeature.FEATURE_TYPE.equals(descriptor.getType())) {
        // we have build feature of needed type
        SharedResourcesFeature f = myFactory.createFeature(descriptor);
        if (buildFeatureId.equals(descriptor.getId())) {
          // we have feature that we need to edit
          locks.putAll(f.getLockedResources());
          invalidLocks.addAll(f.getInvalidLocks(projectId).keySet());
          inherited =  bfb.isInherited();
          if (inherited) {
            model.put("template", buildTypeSettings.getTemplate());
          }
        } else {
          // we have feature, that is not current feature under edit. must remove resources used by other features
          for (String name : f.getLockedResources().keySet()) {
            resources.remove(name);
          }
        }
      }
    }
    final Map<SProject, Map<String, Resource>> rcs = new HashMap<SProject, Map<String, Resource>>();
    rcs.put(project, resources);
    final SharedResourcesBean bean = new SharedResourcesBean(project, rcs);
    final Map<String, Lock> invalidLocksMap = new HashMap<String, Lock>();
    for (Lock lock: invalidLocks) {
      invalidLocksMap.put(lock.getName(), lock);
    }
    model.put("inherited", inherited);
    model.put("invalidLocks", invalidLocksMap);
    model.put("locks", locks);
    model.put("bean", bean);
    model.put("project", project);
    return result;
  }


}
