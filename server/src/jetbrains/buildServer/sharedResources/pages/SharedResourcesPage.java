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

import com.intellij.openapi.util.Pair;
import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import jetbrains.buildServer.controllers.admin.projects.EditProjectTab;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.auth.SecurityContext;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.pages.beans.BeansFactory;
import jetbrains.buildServer.sharedResources.server.ConfigurationInspector;
import jetbrains.buildServer.sharedResources.server.analysis.ResourceUsageAnalyzer;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class SharedResourcesPage extends EditProjectTab {

  @NotNull
  private static final String TITLE_PREFIX = "Shared Resources";

  @NotNull
  private final Resources myResources;

  @NotNull
  private final SecurityContext mySecurityContext;

  @NotNull
  private final ConfigurationInspector myInspector;

  @NotNull
  private final ResourceUsageAnalyzer myAnalyzer;
  @NotNull private final BeansFactory myBeansFactory;

  public SharedResourcesPage(@NotNull final PagePlaces pagePlaces,
                             @NotNull final PluginDescriptor descriptor,
                             @NotNull final Resources resources,
                             @NotNull final SecurityContext securityContext,
                             @NotNull final ConfigurationInspector inspector,
                             @NotNull final ResourceUsageAnalyzer analyzer,
                             @NotNull final BeansFactory beansFactory) {
    super(pagePlaces, SharedResourcesPluginConstants.PLUGIN_NAME, descriptor.getPluginResourcesPath("projectPage.jsp"), TITLE_PREFIX);
    myResources = resources;
    mySecurityContext = securityContext;
    myInspector = inspector;
    myAnalyzer = analyzer;
    myBeansFactory = beansFactory;
    addCssFile("/css/admin/buildTypeForm.css");
    addJsFile(descriptor.getPluginResourcesPath("js/ResourceDialog.js"));
  }

  @Override
  public void fillModel(@NotNull final Map<String, Object> model, @NotNull final HttpServletRequest request) {
    super.fillModel(model, request);
    final SProject project = getProject(request);
    if (project != null) {
      model.put("bean", myBeansFactory.createProjectPageBean(project));
      model.put("configurationErrors", getConfigurationErrors(project));
      model.put("usedResources", myAnalyzer.findUsedResources(project));
      model.put("duplicates", prepareDuplicates(project));
    }
  }

  @Override
  public boolean hasOwnSettings(@NotNull final SProject project) {
    return !myResources.getOwnResources(project).isEmpty();
  }

  @Override
  public boolean isAvailable(@NotNull final HttpServletRequest request) {
    final SProject project = getProject(request);
    final SUser user = (SUser) mySecurityContext.getAuthorityHolder().getAssociatedUser();
    return  user != null
            && project != null
            && (user.isPermissionGrantedForProject(project.getProjectId(), Permission.EDIT_PROJECT)
                || user.isPermissionGrantedForProject(project.getProjectId(), Permission.VIEW_BUILD_CONFIGURATION_SETTINGS));
  }

  @NotNull
  private Map<SBuildType, Map<Lock, String>> getConfigurationErrors(@NotNull final SProject project) {
    return project.getBuildTypes().stream()
                  .map(bt -> new Pair<>(bt, myInspector.inspect(bt)))
                  .filter(it -> !it.second.isEmpty())
                  .collect(Collectors.toMap(
                    p -> p.first,
                    p -> p.second
                  ));
  }

  private Map<String, Boolean> prepareDuplicates(@NotNull final SProject project) {
    final Map<String, Boolean> result = new HashMap<>();
    myInspector.getOwnDuplicateNames(project).forEach(dup -> result.put(dup, true));
    return result;
  }
}
