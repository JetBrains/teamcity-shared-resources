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
import jetbrains.buildServer.sharedResources.server.exceptions.DuplicateResourceException;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public interface Resources {

  /**
   * Adds given resource to project resources
   *
   * @param project project to add resource to
   * @param resource resource to add
   * @throws DuplicateResourceException if resource with the same name as {@code resource} in parameters exists
   */
  void addResource(@NotNull final SProject project, @NotNull final Resource resource) throws DuplicateResourceException;

  /**
   * Deletes given resource from the project
   *
   * @param project project to delete resource from
   * @param resourceName name of the resource to delete
   */
  void deleteResource(@NotNull final SProject project, @NotNull final String resourceName);

  /**
   * Edits given resource
   *
   * @param project to edit resource in
   * @param currentName currentName of the resource
   * @param resource resource to replace existing one
   * @throws DuplicateResourceException if resource with given name exists
   */
  void editResource(@NotNull final SProject project,
                    @NotNull final String currentName,
                    @NotNull final Resource resource) throws DuplicateResourceException;

  /**
   * Gets all resources for project with given {@code projectId} and all its ancestors
   *
   * @param projectId id oof the current project
   * @return map of resources in format {@code resource_name -> resource}
   */
  @NotNull
  Map<String, Resource> asMap(@NotNull final String projectId);

  /**
   * Gets all resources for project with given {@code projectId} and
   * all its ancestors
   *
   * @param projectId id of the current project
   * @return map of projects and resources in format {@code project -> {resource_name -> resource}}
   */
  @NotNull
  Map<SProject, Map<String, Resource>> asProjectResourceMap(@NotNull final String projectId);

  /**
   * Gets number of resources, visible for project with given project id
   *
   * @param project to count resources in
   * @return number of visible resources
   */
  int getCount(@NotNull final SProject project);
}
