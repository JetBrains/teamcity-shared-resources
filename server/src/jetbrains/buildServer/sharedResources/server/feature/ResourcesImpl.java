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

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SecurityContextEx;
import jetbrains.buildServer.serverSide.settings.ProjectSettingsManager;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.server.exceptions.DuplicateResourceException;
import jetbrains.buildServer.sharedResources.settings.PluginProjectSettings;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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
  private static final Logger LOG = Logger.getInstance(ResourcesImpl.class.getName());

  @NotNull
  private final ProjectSettingsManager myProjectSettingsManager;

  @NotNull
  private final ProjectManager myProjectManager;

  @NotNull
  private final SecurityContextEx mySecurityContextEx;

  public ResourcesImpl(@NotNull final ProjectSettingsManager projectSettingsManager,
                       @NotNull final ProjectManager projectManager, @NotNull SecurityContextEx securityContextEx) {
    myProjectSettingsManager = projectSettingsManager;
    myProjectManager = projectManager;
    mySecurityContextEx = securityContextEx;
  }

  @Override
  public void addResource(@NotNull final String projectId, @NotNull final Resource resource) throws DuplicateResourceException {
    final String name = resource.getName();
    checkNameDuplication(name);
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
      checkNameDuplication(newName);
    }
    getSettings(projectId).editResource(currentName, newResource);
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

  @Override
  public int getCount(@NotNull String projectId) {
    int result = 0;
    SProject project = myProjectManager.findProjectById(projectId);
    if (project != null) {
      String parentId = project.getParentProjectId();
      if (parentId != null) {
        result += getCount(parentId);
      }
      result += getSettings(projectId).getCount();
    }
    return  result;
  }

  @NotNull
  private Map<String, Resource> getAllResources() {
    final Map<String, Resource> result = new HashMap<String, Resource>();
    final SProject root = myProjectManager.getRootProject();
    final List<SProject> children = new ArrayList<SProject>();
    try {
      children.addAll(mySecurityContextEx.runAsSystem(new SecurityContextEx.RunAsActionWithResult<List<SProject>>() {
        @Override
        public List<SProject> run() throws Throwable {
          return root.getProjects();
        }
      }));
    } catch (Throwable t) {
      LOG.error(t);
    }
    children.add(root);
    for (SProject p : children) {
      result.putAll(getSettings(p.getProjectId()).getResourceMap());
    }
    return result;
  }

  private void checkNameDuplication(@NotNull final String name) throws DuplicateResourceException {
    final Map<String, Resource> resources = getAllResources();
    if (resources.containsKey(name)) {
      throw new DuplicateResourceException(name);
    }
  }

  @NotNull
  private PluginProjectSettings getSettings(@NotNull final String projectId) {
    return (PluginProjectSettings) myProjectSettingsManager.getSettings(projectId, SERVICE_NAME);
  }
}
