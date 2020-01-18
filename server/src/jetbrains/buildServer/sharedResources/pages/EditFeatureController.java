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

package jetbrains.buildServer.sharedResources.pages;

import com.intellij.openapi.util.text.StringUtil;
import com.sun.org.apache.xpath.internal.operations.Bool;
import java.lang.ref.Reference;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.controllers.admin.projects.BuildFeatureBean;
import jetbrains.buildServer.controllers.admin.projects.BuildFeaturesBean;
import jetbrains.buildServer.controllers.admin.projects.EditBuildTypeFormFactory;
import jetbrains.buildServer.controllers.admin.projects.EditableBuildTypeSettingsForm;
import jetbrains.buildServer.serverSide.BuildTypeSettings;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceType;
import jetbrains.buildServer.sharedResources.pages.beans.BeansFactory;
import jetbrains.buildServer.sharedResources.pages.beans.EditFeatureBean;
import jetbrains.buildServer.sharedResources.server.ConfigurationInspector;
import jetbrains.buildServer.sharedResources.server.SharedResourcesBuildFeature;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeature;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatureFactory;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.web.servlet.ModelAndView;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.EDIT_FEATURE_PATH_HTML;
import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.EDIT_FEATURE_PATH_JSP;
import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.FEATURE_TYPE;

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

  @NotNull
  private final ConfigurationInspector myInspector;

  @NotNull
  private final ProjectManager myProjectManager;

  @NotNull
  private final BeansFactory myBeansFactory;

  public EditFeatureController(@NotNull final PluginDescriptor descriptor,
                               @NotNull final WebControllerManager web,
                               @NotNull final EditBuildTypeFormFactory formFactory,
                               @NotNull final Resources resources,
                               @NotNull final SharedResourcesFeatureFactory factory,
                               @NotNull final ConfigurationInspector inspector,
                               @NotNull final ProjectManager projectManager,
                               @NotNull final BeansFactory beansFactory) {
    myDescriptor = descriptor;
    myFormFactory = formFactory;
    myResources = resources;
    myFactory = factory;
    myInspector = inspector;
    myProjectManager = projectManager;
    myBeansFactory = beansFactory;
    web.registerController(myDescriptor.getPluginResourcesPath(EDIT_FEATURE_PATH_HTML), this);
  }

  @Nullable
  @Override
  protected ModelAndView doHandle(@NotNull final HttpServletRequest request,
                                  @NotNull final HttpServletResponse response) {
    final ModelAndView result = new ModelAndView(myDescriptor.getPluginResourcesPath(EDIT_FEATURE_PATH_JSP));
    final Map<String, Object> model = result.getModel();

    final EditableBuildTypeSettingsForm form = myFormFactory.getOrCreateForm(request);
    assert form != null;

    final BuildTypeSettings buildTypeSettings = form.getSettings();
    assert buildTypeSettings != null;

    final SProject project = form.getProject();
    final BuildFeaturesBean buildFeaturesBean = form.getBuildFeaturesBean();

    final String buildFeatureId = request.getParameter("featureId");
    final Map<String, Lock> locks = new HashMap<>();
    // map of all visible resources from this project and its subtree
    final List<Resource> projectResources = myResources.getResources(project);
    final Set<String> available = projectResources.stream().map(Resource::getName).collect(Collectors.toSet());
    // resource names locked by other features defined in current build type
    final Set<String> lockedByOtherFeatures = new HashSet<>();

    final Collection<Lock> invalidLocks = new ArrayList<>();
    boolean inherited = false;
    for (BuildFeatureBean bfb: buildFeaturesBean.getBuildFeatureDescriptors()) {
      SBuildFeatureDescriptor descriptor = bfb.getDescriptor();
      if (SharedResourcesBuildFeature.FEATURE_TYPE.equals(descriptor.getType())) {
        // we have build feature of needed type
        SharedResourcesFeature f = myFactory.createFeature(descriptor);
        if (buildFeatureId.equals(descriptor.getId())) {
          // we have feature that we need to edit
          locks.putAll(f.getLockedResources());
          invalidLocks.addAll(myInspector.inspect(project, f).keySet());
          inherited =  bfb.isInherited();
          String originExternalId = bfb.getOriginExternalId();
          if (!StringUtil.isEmptyOrSpaces(originExternalId)) {
            if (inherited) {
              model.put("template", myProjectManager.findBuildTypeTemplateByExternalId(originExternalId));
            }
          }
        } else {
          // if feature belongs to current build type settings (not inherited, not enforced etc),
          // we must remove resources locked by this feature from available
          if (!bfb.isInherited()) {
            lockedByOtherFeatures.addAll(f.getLockedResources().keySet());
          }
        }
      }
    }
    if (!inherited) { // we are editing build feature descriptor in current build type
      available.removeAll(lockedByOtherFeatures);
    }

    if (buildTypeSettings.isCompositeBuildType()) { // custom resources are not available for composite build types yet
      projectResources.stream()
                      .filter(resource -> resource.getType() == ResourceType.CUSTOM)
                      .map(Resource::getName)
                      .forEach(available::remove);
    }
    final EditFeatureBean bean = myBeansFactory.createEditFeatureBean(project, available);
    final Map<String, Lock> invalidLocksMap = new HashMap<>();
    for (Lock lock: invalidLocks) {
      invalidLocksMap.put(lock.getName(), lock);
    }
    model.put("inherited", inherited);
    model.put("invalidLocks", invalidLocksMap);
    model.put("locks", locks);
    model.put("bean", bean);
    return result;
  }
}
