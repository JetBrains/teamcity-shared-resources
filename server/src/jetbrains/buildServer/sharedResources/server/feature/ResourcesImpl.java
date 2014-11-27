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

import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.settings.ProjectSettingsManager;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.server.exceptions.DuplicateResourceException;
import jetbrains.buildServer.sharedResources.settings.PluginProjectSettings;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.filters.Filter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.RESOURCE_NAMES_COMPARATOR;
import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.SERVICE_NAME;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public final class ResourcesImpl implements Resources {

  @NotNull
  private final ProjectSettingsManager myProjectSettingsManager;

  @NotNull
  private final ProjectManager myProjectManager;


  public ResourcesImpl(@NotNull final ProjectSettingsManager projectSettingsManager,
                       @NotNull final ProjectManager projectManager) {
    myProjectSettingsManager = projectSettingsManager;
    myProjectManager = projectManager;
  }

  @Override
  public void addResource(@NotNull final Resource resource) throws DuplicateResourceException {
    final String name = resource.getName();
    final String projectId = resource.getProjectId();
    checkNameDuplication(projectId, name);
    getSettings(projectId).addResource(resource);
  }

  @Override
  public void deleteResource(@NotNull final String projectId, @NotNull final String resourceName) {
    getSettings(projectId).deleteResource(resourceName);
  }

  @Override
  public void editResource(@NotNull final String projectId,
                           @NotNull final String currentName,
                           @NotNull final Resource newResource) throws DuplicateResourceException {
    final String newName = newResource.getName();
    if (!currentName.equals(newName)) {
      checkNameDuplication(projectId, newName);
    }
    getSettings(projectId).editResource(currentName, newResource);
  }

  @NotNull
  @Override
  public Map<String, Resource> asMap(@NotNull final String projectId) {
    return getResourcesWithInheritance(projectId);
  }

  @NotNull
  @Override
  public Map<SProject, Map<String, Resource>> asProjectResourceMap(@NotNull String projectId) {
    return asProjectResourcesMapWithInheritance(projectId);
  }

  @Override
  public int getCount(@NotNull final String projectId) {
    int result = 0;
    final Map<SProject, Map<String, Resource>> resources = asProjectResourcesMapWithInheritance(projectId);
    for (Map<String, Resource> map: resources.values()) {
      result += map.size();
    }
    return  result;
  }

  /**
   * Gets all resources for single project
   * @param projectId internal id of the project
   * @return map of all resources for the project with given id
   */
  @NotNull
  private Map<String, Resource> getResourcesForProject(@NotNull final String projectId) {
    final Map<String, Resource> result = new TreeMap<String, Resource>(RESOURCE_NAMES_COMPARATOR);
    result.putAll(getSettings(projectId).getResourceMap());
    return result;
  }

  private Map<String, Resource> getResourcesWithInheritance(@NotNull final String projectId) {
    final Map<String, Resource> result = new TreeMap<String, Resource>(RESOURCE_NAMES_COMPARATOR);
    final SProject currentProject = myProjectManager.findProjectById(projectId);
    if (currentProject != null) {
      // get project path
      final List<SProject> projects = currentProject.getProjectPath();
      final ListIterator<SProject> it = projects.listIterator(projects.size());
      while (it.hasPrevious()) {
        SProject p = it.previous();
        Map<String, Resource> currentResources = getResourcesForProject(p.getProjectId());
        // add only non-overridden resources (may be just add with overwriting??)
        result.putAll(CollectionsUtil.filterMapByKeys(currentResources, new Filter<String>() {
          @Override
          public boolean accept(@NotNull String data) {
            return !result.containsKey(data);
          }
        }));
      }
    }
    return result;
  }

  @NotNull
  private Map<SProject, Map<String, Resource>> asProjectResourcesMapWithInheritance(@NotNull final String projectId) {
    final Map<SProject, Map<String, Resource>> result = new LinkedHashMap<SProject, Map<String, Resource>>();
    // cache for already fetched resources
    final Map<String, Resource> treeResources = new HashMap<String, Resource>();
    final SProject currentProject = myProjectManager.findProjectById(projectId);
    if (currentProject != null) {
      final List<SProject> projects = currentProject.getProjectPath();
      final ListIterator<SProject> it = projects.listIterator(projects.size());
      while (it.hasPrevious()) {
        SProject p = it.previous();
        Map<String, Resource> currentResources = getResourcesForProject(p.getProjectId());
        final Map<String, Resource> value = CollectionsUtil.filterMapByKeys(currentResources, new Filter<String>() {
          @Override
          public boolean accept(@NotNull String data) {
            return !treeResources.containsKey(data);
          }
        });
        treeResources.putAll(value);
        result.put(p, value);
      }
    }
    return result;
  }

  private void checkNameDuplication(@NotNull final String projectId, @NotNull final String name) throws DuplicateResourceException {
    final Map<String, Resource> resources = getResourcesForProject(projectId);
    if (resources.containsKey(name)) {
      throw new DuplicateResourceException(name);
    }
  }

  @NotNull
  private PluginProjectSettings getSettings(@NotNull final String projectId) {
    return (PluginProjectSettings) myProjectSettingsManager.getSettings(projectId, SERVICE_NAME);
  }
}
