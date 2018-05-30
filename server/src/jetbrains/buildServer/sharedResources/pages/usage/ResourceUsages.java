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

package jetbrains.buildServer.sharedResources.pages.usage;

import com.intellij.openapi.util.text.StringUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import jetbrains.buildServer.controllers.admin.projects.UsagesReportPageExtension;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.auth.AccessDeniedException;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.server.analysis.ResourceUsageAnalyzer;
import jetbrains.buildServer.sharedResources.server.analysis.FindUsagesResult;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ResourceUsages extends UsagesReportPageExtension {

  @NotNull private final ProjectManager myProjectManager;
  @NotNull
  private final Resources myResources;

  @NotNull
  private final ResourceUsageAnalyzer myAnalyzer;

  public ResourceUsages(@NotNull final PagePlaces pagePlaces,
                        @NotNull final PluginDescriptor pluginDescriptor,
                        @NotNull final ProjectManager projectManager,
                        @NotNull final Resources resources,
                        @NotNull final ResourceUsageAnalyzer analyzer) {
    super("resourceUsages", pagePlaces);
    myProjectManager = projectManager;
    myResources = resources;
    myAnalyzer = analyzer;
    setIncludeUrl(pluginDescriptor.getPluginResourcesPath("report/sharedResourceUsages.jsp"));
    register();
  }

  @Override
  public boolean isAvailable(@NotNull final HttpServletRequest request) {
    return super.isAvailable(request) && request.getParameter("resourceId") != null;
  }

  @Override
  public void fillModel(@NotNull final Map<String, Object> model, @NotNull final HttpServletRequest request) {
    // resource can be inherited and acquired from another project. Search origin project. Display usages only in current subtree
    final String currentProjectId = request.getParameter("resourceProjectId");
    int totalUsagesNum = 0;
    final Map<SBuildType, List<Lock>> buildTypes = new HashMap<>();
    final Map<BuildTypeTemplate, List<Lock>> templates = new HashMap<>();
    final SProject project = findProject(currentProjectId);
    if (project != null) {
      final String resourceId = request.getParameter("resourceId");
      final Resource resource = getResource(project, resourceId);
      model.put("resourceId", resourceId);
      if (resource != null) {
        final FindUsagesResult usages = myAnalyzer.findUsages(project, resource);
        buildTypes.putAll(usages.getBuildTypes());
        templates.putAll(usages.getTemplates());
        totalUsagesNum += usages.getTotal();
        model.put("resource", resource);
      }
    }
    model.put("buildTypes", buildTypes);
    model.put("templates", templates);
    model.put("totalUsagesNum", totalUsagesNum);
  }

  private SProject findProject(@Nullable final String currentProjectId) {
    if (StringUtil.isEmptyOrSpaces(currentProjectId)) {
      return null;
    }
    try {
      return myProjectManager.findProjectByExternalId(currentProjectId);
    } catch (AccessDeniedException ignored) {}
    return null;
  }

  @Nullable
  private Resource getResource(@NotNull final SProject project, @NotNull final String resourceId) {
    return myResources.getOwnResources(project).stream()
                      .filter(resource -> resourceId.equals(resource.getId()))
                      .findFirst()
                      .orElse(null);
  }
}
