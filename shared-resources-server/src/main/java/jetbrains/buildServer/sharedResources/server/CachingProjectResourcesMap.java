/*
 * Copyright 2000-2025 JetBrains s.r.o.
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

package jetbrains.buildServer.sharedResources.server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import org.jetbrains.annotations.NotNull;

public class CachingProjectResourcesMap {
  private final Map<String, Map<String, Resource>> myCache = new HashMap<>();
  private final Resources myResources;

  public CachingProjectResourcesMap(@NotNull final Resources resources) {
    myResources = resources;
  }

  @NotNull
  public Map<String, Resource> getResourcesMap(@NotNull final SProject project) {
    final Map<String, Resource> resourcesMap = myCache.get(project.getProjectId());
    if (resourcesMap != null) return resourcesMap;

    Map<String, Resource> result = new HashMap<>();
    final List<SProject> projectPath = project.getProjectPath();
    for (SProject p : projectPath) {
      Map<String, Resource> cached = myCache.get(project.getProjectId());
      if (cached != null) {
        result.putAll(cached);
        continue;
      }

      final List<Resource> ownResources = myResources.getOwnResources(p);
      for (Resource r : ownResources) {
        result.put(r.getName(), r);
      }

      // store a copy of our intermediate result to the cache for the current project
      myCache.computeIfAbsent(p.getProjectId(), id -> new HashMap<>(result));
    }

    return result;
  }
}
