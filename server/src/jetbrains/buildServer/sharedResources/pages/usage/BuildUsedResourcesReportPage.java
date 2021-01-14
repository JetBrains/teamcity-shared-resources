/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

import com.intellij.openapi.util.Pair;
import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.AccessDeniedException;
import jetbrains.buildServer.sharedResources.server.feature.Locks;
import jetbrains.buildServer.sharedResources.server.report.BuildUsedResourcesReport;
import jetbrains.buildServer.sharedResources.server.report.UsedResource;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.ViewLogTab;
import org.jetbrains.annotations.NotNull;

/**
 * Report page with used shared resources for single build
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class BuildUsedResourcesReportPage extends ViewLogTab {

  @NotNull
  private final BuildUsedResourcesReport myReport;

  @NotNull
  private final Locks myLocks;

  public BuildUsedResourcesReportPage(@NotNull final PagePlaces pagePlaces,
                                      @NotNull final SBuildServer server,
                                      @NotNull final PluginDescriptor descriptor,
                                      @NotNull final BuildUsedResourcesReport report,
                                      @NotNull final Locks locks) {
    super("Shared Resources", "buildUsedResources", pagePlaces, server);
    myReport = report;
    myLocks = locks;
    setIncludeUrl(descriptor.getPluginResourcesPath("report/buildUsedResources.jsp"));
  }

  @Override
  protected void fillModel(@NotNull final Map<String, Object> model, @NotNull final HttpServletRequest request, @NotNull final SBuild build) {
    final List<UsedResource> usedResources = myReport.load(build);
    usedResources.sort((r1, r2) -> r1.getResource().getName().compareToIgnoreCase(r2.getResource().getName()));
    final Map<String, SProject> projects = new HashMap<>();
    final Set<String> failed = new HashSet<>();
    final ProjectManager pm = myServer.getProjectManager();
    final Map<String, SProject> resourceOrigins = new HashMap<>();
    final List<Pair<String, String>> parameters = new ArrayList<>();

    usedResources.forEach(ur -> {
      String id = ur.getResource().getProjectId();
      if (projects.containsKey(id)) {
        resourceOrigins.put(ur.getResource().getName(), projects.get(id));
      } else {
        if (!failed.contains(id)) {
          try {
            SProject project = pm.findProjectById(id);
            if (project != null) {
              projects.put(id, project);
              resourceOrigins.put(ur.getResource().getName(), project);
            }
          } catch (AccessDeniedException e) {
            failed.add(id);
          }
        }
      }
      parameters.addAll(myLocks.asBuildParameters(ur.getLocks()).entrySet().stream()
                               .map(e -> new Pair<>(e.getKey(), e.getValue()))
                               .collect(Collectors.toList()));
    });
    parameters.sort((p1, p2) -> p1.getFirst().compareToIgnoreCase(p2.getFirst()));
    model.put("resourceOrigins", resourceOrigins);
    model.put("usedResources", usedResources);
    model.put("parameters", parameters);
  }

  @Override
  protected boolean isAvailable(@NotNull final HttpServletRequest request,
                                @NotNull final SBuild build) {
    return super.isAvailable(request, build)
           && myReport.exists(build);
  }
}
