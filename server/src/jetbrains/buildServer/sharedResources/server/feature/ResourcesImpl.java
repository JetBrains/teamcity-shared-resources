/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

package jetbrains.buildServer.sharedResources.server.feature;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.server.project.ResourceProjectFeature;
import jetbrains.buildServer.sharedResources.server.project.ResourceProjectFeatures;
import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public final class ResourcesImpl implements Resources {

  @NotNull
  private final ResourceProjectFeatures myFeatures;

  @NotNull
  private final ProjectManager myProjectManager;


  public ResourcesImpl(@NotNull final ProjectManager projectManager,
                       @NotNull final ResourceProjectFeatures resourceProjectFeatures) {
    myProjectManager = projectManager;
    myFeatures = resourceProjectFeatures;
  }

  @NotNull
  @Override
  public Map<String, Resource> getResourcesMap(@NotNull final String projectId) {
    final SProject project = myProjectManager.findProjectById(projectId);
    if (project != null) {
      return getResources(project).stream()
                                  .collect(Collectors.toMap(Resource::getName, Function.identity()));
    } else {
      return Collections.emptyMap();
    }
  }

  @NotNull
  @Override
  public List<Resource> getAllOwnResources(@NotNull final SProject project) {
    return myFeatures.getOwnFeatures(project).stream()
                     .map(ResourceProjectFeature::getResource)
                     .filter(Objects::nonNull)
                     .collect(Collectors.toList());

  }

  @NotNull
  @Override
  public List<Resource> getOwnResources(@NotNull final SProject project) {
    return myFeatures.getOwnFeatures(project).stream()
                     .map(ResourceProjectFeature::getResource)
                     .filter(Objects::nonNull)
                     .collect(Collectors.groupingBy(Resource::getName)).values().stream() // collect by name
                     .filter(list -> list.size() == 1) // exclude duplicates
                     .map(list -> list.get(0))
                     .collect(Collectors.toList());
  }

  @NotNull
  @Override
  public Collection<Resource> getResources(@NotNull final String projectId) {
    final SProject project = myProjectManager.findProjectById(projectId);
    if (project != null) {
      return getResources(project);
    } else {
      return Collections.emptyList();
    }
  }

  @NotNull
  @Override
  public List<Resource> getResources(@NotNull final SProject project) {
    final Set<String> names = new HashSet<>();
    final Set<Resource> result = new HashSet<>();
    final List<SProject> path = project.getProjectPath();
    final ListIterator<SProject> it = path.listIterator(path.size());
    while (it.hasPrevious()) {
      final Set<Resource> filtered = getOwnResources(it.previous()).stream()
                                                                   .filter(data -> !names.contains(data.getName()))
                                                                   .collect(Collectors.toSet());
      result.addAll(filtered);
      names.addAll(filtered.stream().map(Resource::getName).collect(Collectors.toSet()));
    }
    return new ArrayList<>(result);
  }

  @Override
  public int getCount(@NotNull final SProject project) {
    return getResources(project).size();
  }
}
