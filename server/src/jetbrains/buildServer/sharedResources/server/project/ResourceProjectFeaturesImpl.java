/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package jetbrains.buildServer.sharedResources.server.project;

import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceFactory;
import jetbrains.buildServer.sharedResources.server.exceptions.DuplicateResourceException;
import jetbrains.buildServer.util.CollectionsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.ProjectFeatureParameters.NAME;
import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.RESOURCE_NAMES_COMPARATOR;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class ResourceProjectFeaturesImpl implements ResourceProjectFeatures {

  @Override
  public void addResource(@NotNull final SProject project,
                          @NotNull final Map<String, String> resourceParameters) throws DuplicateResourceException {
    final String name = resourceParameters.get(NAME);
    if (getResourceNamesInProject(project).contains(name)) {
      throw new DuplicateResourceException(name);
    }
    project.addFeature(SharedResourcesPluginConstants.FEATURE_TYPE, resourceParameters);
  }

  @Override
  public void deleteResource(@NotNull final SProject project, @NotNull final String name) {
    final SProjectFeatureDescriptor descriptor = findDescriptorByResourceName(project, name);
    if (descriptor != null) {
      project.removeFeature(descriptor.getId());
    }
  }

  @Override
  public void editResource(@NotNull final SProject project,
                           @NotNull final String name,
                           @NotNull final Map<String, String> resourceParameters) throws DuplicateResourceException {
    final SProjectFeatureDescriptor descriptor = findDescriptorByResourceName(project, name);
    if (descriptor != null) {
      final String newName = resourceParameters.get(NAME);
      if (!name.equals(newName)) {
        if (getResourceNamesInProject(project).contains(newName)) {
          throw new DuplicateResourceException(newName);
        }
      }
      project.updateFeature(descriptor.getId(), SharedResourcesPluginConstants.FEATURE_TYPE, resourceParameters);
    }
  }


  @Nullable
  private SProjectFeatureDescriptor findDescriptorByResourceName(@NotNull final SProject project,
                                                                 @NotNull final String name) {
    final Optional<SProjectFeatureDescriptor> descriptor = getResourceFeatures(project)
            .stream()
            .filter(fd -> name.equals(fd.getParameters().get(NAME)))
            .findFirst();
    if (descriptor.isPresent()) {
      return descriptor.get();
    }
    return null;
  }

  @NotNull
  @Override
  public Map<SProject, Map<String, Resource>> asProjectResourceMap(@NotNull final SProject project) {
    final Map<SProject, Map<String, Resource>> result = new LinkedHashMap<>();
    final Map<String, Resource> treeResources = new HashMap<>(); // todo: we need only names here
    final List<SProject> path = project.getProjectPath();
    final ListIterator<SProject> it = path.listIterator(path.size());
    while (it.hasPrevious()) {
      SProject p = it.previous();
      Map<String, Resource> currentResources = getResourcesForProject(p);
      final Map<String, Resource> value = CollectionsUtil.filterMapByKeys(
              currentResources, data -> !treeResources.containsKey(data)
      );
      treeResources.putAll(value);
      result.put(p, value);
    }
    return result;
  }

  @Override
  @NotNull
  public Map<String, Resource> asMap(@NotNull final SProject project) {
    final Map<String, Resource> result = new TreeMap<>(RESOURCE_NAMES_COMPARATOR);
    final List<SProject> path = project.getProjectPath();
    final ListIterator<SProject> it = path.listIterator(path.size());
    while (it.hasPrevious()) {
      SProject p = it.previous();
      Map<String, Resource> currentResources = getResourcesForProject(p);
      result.putAll(CollectionsUtil.filterMapByKeys(currentResources, data -> !result.containsKey(data)));
    }
    return result;
  }

  @NotNull
  @Override
  public List<Resource> getOwnResources(@NotNull final SProject project) {
    return getResourceFeatures(project).stream()
            .map(ResourceFactory::fromProjectFeatureDescriptor)
            .collect(Collectors.toList());
  }

  @NotNull
  @Override
  public List<Resource> getResources(@NotNull final SProject project) {
    final Set<Resource> resources = new HashSet<>();
    final List<SProject> path = project.getProjectPath();
    final ListIterator<SProject> it = path.listIterator(path.size());
    while (it.hasPrevious()) {
      SProject p = it.previous();
      resources.addAll(CollectionsUtil.filterCollection(getOwnResources(p), data -> !resources.contains(data)));
    }
    return new ArrayList<>(resources);
  }

  @NotNull
  private Set<String> getResourceNamesInProject(@NotNull final SProject project) {
    return getResourceFeatures(project).stream()
            .map(fd -> fd.getParameters().get(NAME))
            .collect(Collectors.toSet());
  }

  @NotNull
  private Map<String, Resource> getResourcesForProject(@NotNull final SProject project) {
    final Map<String, Resource> result = new TreeMap<>(RESOURCE_NAMES_COMPARATOR);
    result.putAll(getResourceFeatures(project).stream().collect(
            Collectors.toMap(
                    rd -> rd.getParameters().get(NAME),
                    ResourceFactory::fromProjectFeatureDescriptor,
                    (r1, r2) -> r1)
    ));
    return result;
  }

  @NotNull
  private Collection<SProjectFeatureDescriptor> getResourceFeatures(@NotNull SProject project) {
    return project.getOwnFeaturesOfType(SharedResourcesPluginConstants.FEATURE_TYPE);
  }
}
