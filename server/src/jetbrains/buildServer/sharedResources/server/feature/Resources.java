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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public interface Resources {

  /**
   * Gets all resources for project with given {@code projectId} and all its ancestors
   *
   * {@link #getResources(SProject)} cannot be used in runtime, as it requires project to be present,
   * which makes this method a convenient single point, where we acquire project by project id
   *
   * @param projectId id oof the current project
   * @return map of resources in format {@code resource_name -> resource}
   */
  @NotNull
  Map<String, Resource> getResourcesMap(@NotNull final String projectId);

  /**
   * Returns all valid resources, defined in current project
   * Duplicates are not excluded.
   * Not to be used in runtime for resource locking
   *
   * @param project project to get resources for
   * @return all valid resources, defined in current project
   */
  @NotNull
  List<Resource> getAllOwnResources(@NotNull final SProject project);

  /**
   * Gets project own resources
   * Excludes duplicates
   *
   * @param project project to get resources for
   * @return own resources of the project
   */
  @NotNull
  List<Resource> getOwnResources(@NotNull final SProject project);

  /**
   * Gets all resources for project with given {@code projectId} and all its ancestors
   *
   * @param projectId id oof the current project
   * @return collection of all resources for project
   */
  @NotNull
  Collection<Resource> getResources(@NotNull final String projectId);

  /**
   * Gets resources for project with inheritance
   * Duplicates are excluded on every level of project hierarchy
   *
   * @param project project to get resources for
   * @return project's resources with inheritance
   */
  @NotNull
  List<Resource> getResources(@NotNull final SProject project);

  /**
   * Gets number of resources, visible for project with given project id
   * Counts only resources available at runtime. Ignores duplicates
   *
   * @param project to count resources in
   * @return number of visible resources
   */
  int getCount(@NotNull final SProject project);
}
