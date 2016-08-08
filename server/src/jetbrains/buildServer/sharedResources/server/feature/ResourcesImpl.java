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

package jetbrains.buildServer.sharedResources.server.feature;

import java.util.*;
import java.util.stream.Collectors;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.server.exceptions.DuplicateResourceException;
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

  @Override
  public void addResource(@NotNull final SProject project, @NotNull final Resource resource) throws DuplicateResourceException {
    myFeatures.addFeature(project, resource.getParameters());
  }

  @Override
  public void addResource(@NotNull final SProject project, @NotNull final Map<String, String> params) {
    myFeatures.addFeature(project, params);
  }

  @Override
  public void deleteResource(@NotNull final SProject project, @NotNull final String resourceId) {
    myFeatures.removeFeature(project, resourceId);
  }

  @Override
  public void editResource(@NotNull final SProject project,
                           @NotNull final String currentName,
                           @NotNull final Resource resource) throws DuplicateResourceException {
    myFeatures.updateFeature(project, currentName, resource.getParameters());
  }

  @Override
  public void editResource(@NotNull final SProject project, @NotNull final String id, @NotNull final Map<String, String> params) {
    myFeatures.updateFeature(project, id, params);
  }

  @NotNull
  @Override
  @Deprecated
  public Map<String, Resource> asMap(@NotNull final String projectId) {
    return myFeatures.asMap(myProjectManager.findProjectById(projectId)); //todo: projectId -> project
  }

  @NotNull
  @Override
  public Map<String, Resource> getAvailableResources(@NotNull final SProject project) {
    return myFeatures.asMap(project);
  }

  @NotNull
  @Override
  public List<Resource> getOwnResources(@NotNull final SProject project) {
    return myFeatures.getOwnFeatures(project).stream()
                     .map(ResourceProjectFeature::getResource)
                     .filter(Objects::nonNull)
                     .collect(Collectors.toList());
  }

  @NotNull
  @Override
  public List<Resource> getResources(@NotNull final SProject project) {
    final Set<Resource> result = new HashSet<>();
    final List<SProject> path = project.getProjectPath();
    final ListIterator<SProject> it = path.listIterator(path.size());
    while (it.hasPrevious()) {
      result.addAll(getOwnResources(it.previous()).stream().filter(data -> !result.contains(data)).collect(Collectors.toSet()));
    }
    return new ArrayList<>(result);
  }

  @Override
  public int getCount(@NotNull final SProject project) {
    return getResources(project).size();
  }
}
