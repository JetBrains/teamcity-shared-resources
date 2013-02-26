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
import jetbrains.buildServer.sharedResources.settings.PluginProjectSettings;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  public void addResource(@NotNull final String projectId, @NotNull final Resource resource) {
    getSettings(projectId).addResource(resource);
  }

  @Override
  public void deleteResource(@NotNull final String projectId, @NotNull final String resourceName) {
    getSettings(projectId).deleteResource(resourceName);
  }

  @Override
  public void editResource(@NotNull final String projectId, @NotNull final String name, @NotNull final Resource newResource) {
    getSettings(projectId).editResource(name, newResource);
  }

  @NotNull
  @Override
  public Map<String, Resource> asMap(@NotNull final String projectId) {
    final Map<String, Resource> result = new HashMap<String, Resource>();
    SProject project = myProjectManager.findProjectById(projectId);
    if (project != null) {
      String parentId = project.getParentProjectId();
      if (parentId != null) {
        result.putAll(asMap(parentId));
      }
      result.putAll(getSettings(projectId).getResourceMap());
    }
    return result;
  }

  @NotNull
  @Override
  public Map<SProject, Map<String, Resource>> asProjectResourceMap(@NotNull String projectId) {
    final Map<SProject, Map<String, Resource>> result = new HashMap<SProject, Map<String, Resource>>();
    SProject project = myProjectManager.findProjectById(projectId);
    if (project != null) {
      String parentId = project.getParentProjectId();
      if (parentId != null) {
        result.putAll(asProjectResourceMap(parentId));
      }
      result.put(project, getSettings(projectId).getResourceMap());
    }
    return result;
  }


  @NotNull
  @Override
  public Map<String, Resource> getAllResources() {
    final Map<String, Resource> result = new HashMap<String, Resource>();
    SProject root = myProjectManager.getRootProject();
    List<SProject> children = root.getAllSubProjects();
    children.add(root);
    for (SProject p : children) {
      result.putAll(getSettings(p.getProjectId()).getResourceMap());
    }
    return result;
  }

  @NotNull
  private PluginProjectSettings getSettings(@NotNull final String projectId) {
    return (PluginProjectSettings) myProjectSettingsManager.getSettings(projectId, SERVICE_NAME);
  }
}
