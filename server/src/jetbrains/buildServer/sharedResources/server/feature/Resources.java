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

import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public interface Resources {

  public void addResource(@NotNull final String projectId, @NotNull final Resource resource);

  public void deleteResource(@NotNull final String projectId, @NotNull final String resourceName);

  public void editResource(@NotNull final String projectId, @NotNull final String name, @NotNull final Resource newResource);


  /**
   * Gets all resources for project with given {@code projectId} and all its ancestors
   *
   * @param projectId id oof the current project
   * @return map of resources in format {@code resource_name -> resource}
   */
  @NotNull
  public Map<String, Resource> asMap(@NotNull final String projectId);

  /**
   * Gets all resources for project with given {@code projectId} and
   * all its ancestors
   *
   * @param projectId id of the current project
   * @return map of projects and resources in format {@code project -> {resource_name -> resource}}
   */
  @NotNull
  public Map<SProject, Map<String, Resource>> asProjectResourceMap(@NotNull final String projectId);

  /**
   * Gets all shared resources for all projects
   *
   * @return map of resources in format {@code resource_name -> resource}
   */
  @NotNull
  public Map<String, Resource> getAllResources();

}
