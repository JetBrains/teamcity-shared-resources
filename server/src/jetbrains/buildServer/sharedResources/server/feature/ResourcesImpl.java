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

import jetbrains.buildServer.serverSide.settings.ProjectSettingsManager;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.settings.PluginProjectSettings;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public final class ResourcesImpl implements Resources {

  @NotNull
  private final ProjectSettingsManager myProjectSettingsManager;

  public ResourcesImpl(@NotNull final ProjectSettingsManager projectSettingsManager) {
    myProjectSettingsManager = projectSettingsManager;
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
    return getSettings(projectId).getResourceMap();
  }

  @NotNull
  @Override
  public Collection<Resource> asCollection(@NotNull String projectId) {
    return getSettings(projectId).getResources();
  }

  @NotNull
  private PluginProjectSettings getSettings(@NotNull final String projectId) {
    return (PluginProjectSettings)myProjectSettingsManager.getSettings(projectId, SharedResourcesPluginConstants.SERVICE_NAME);
  }
}
