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
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.server.exceptions.DuplicateResourceException;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public interface ResourceProjectFeatures {

  @NotNull
  List<ResourceProjectFeature> getOwnFeatures(@NotNull final SProject project);

  @NotNull
  @Deprecated
  Map<SProject, Map<String, Resource>> asProjectResourceMap(@NotNull final SProject project);

  @NotNull
  @Deprecated
  Map<String, Resource> asMap(@NotNull final SProject project);

  void addResource(@NotNull final SProject project,
                   @NotNull final Map<String, String> resourceParameters) throws DuplicateResourceException;

  void editResource(@NotNull final SProject project,
                    @NotNull final String name,
                    @NotNull final Map<String, String> resourceParameters) throws DuplicateResourceException;

  void deleteResource(@NotNull final SProject project, @NotNull final String name);
}
