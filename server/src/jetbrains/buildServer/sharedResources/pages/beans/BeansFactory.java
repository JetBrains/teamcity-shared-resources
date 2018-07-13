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

package jetbrains.buildServer.sharedResources.pages.beans;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.RESOURCE_BY_NAME_COMPARATOR;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class BeansFactory {

  @NotNull
  private Resources myResources;

  public BeansFactory(@NotNull final Resources resources) {
    myResources = resources;
  }

  @NotNull
  public EditFeatureBean createEditFeatureBean(@NotNull final SProject project,
                                               @NotNull final Set<String> availableNames) {
    final List<Resource> allResources = myResources.getResources(project).stream()
                                                   .filter(resource -> availableNames.contains(resource.getName()))
                                                   .sorted(RESOURCE_BY_NAME_COMPARATOR)
                                                   .collect(Collectors.toList());
    return new EditFeatureBean(project, allResources);
  }

  @NotNull
  public ProjectPageBean createProjectPageBean(@NotNull final SProject project) {
    final Map<String, List<Resource>> treeResources = new HashMap<>();
    final Map<String, Resource> overridesMap = new HashMap<>();
    final List<Resource> allOwnResources = myResources.getAllOwnResources(project).stream()
                                                      .sorted(RESOURCE_BY_NAME_COMPARATOR)
                                                      .collect(Collectors.toList());
    project.getProjectPath().forEach(p -> {
      final List<Resource> currentOwnResources = myResources.getAllOwnResources(p);
      // check that current resource overrides something
      currentOwnResources.forEach(resource -> {
        // check overrides
        checkOverrides(resource, treeResources, overridesMap);
      });
      if (!p.equals(project)) {
        currentOwnResources.sort(RESOURCE_BY_NAME_COMPARATOR);
        treeResources.put(p.getProjectId(), currentOwnResources);
      }
    });
    return new ProjectPageBean(project, allOwnResources, treeResources, overridesMap);

  }

  private void checkOverrides(final Resource resource,
                              final Map<String, List<Resource>> result,
                              final Map<String, Resource> overridesMap) {
    result.forEach((projectId, resources) -> {
      for (Resource rc: resources) {
        if (resource.getName().equals(rc.getName())) {
          overridesMap.put(rc.getId(), resource);
          break;
        }
      }
    });
  }
}
