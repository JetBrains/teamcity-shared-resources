/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import java.util.*;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeature;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.util.CollectionsUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Class {@code ResourceUsageAnalyzer}
 *
 * For given project, constructs a map of results usage
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class ResourceUsageAnalyzer {

  @NotNull
  private final Resources myResources;

  @NotNull
  private final SharedResourcesFeatures myFeatures;

  public ResourceUsageAnalyzer(@NotNull final Resources resources,
                               @NotNull final SharedResourcesFeatures features) {
    myResources = resources;
    myFeatures = features;
  }

  /**
   * Collects usages of resources, defined in or available (through inheritance) to current project
   * Deals only with subtree of current project
   *
   * @param project project
   * @return map of usages for each resource available
   */
  @NotNull
  public Map<Resource, Map<SBuildType, List<Lock>>> collectResourceUsages(@NotNull final SProject project) {
    final Map<Resource, Map<SBuildType, List<Lock>>> result = new HashMap<>();
    final String projectId = project.getProjectId();
    final Map<String, Resource> availableToMeResources = myResources.getResourcesMap(projectId);
    if (!availableToMeResources.isEmpty()) {
      for (final SBuildType bt : project.getBuildTypes()) {
        final Collection<SharedResourcesFeature> features = myFeatures.searchForFeatures(bt);
        for (SharedResourcesFeature feature : features) {
          // Filter the collection of resources, available to the build by the project id of the resource
          // We are only interested only in the resources, available to current project
          Map<String, Resource> currentBtResources;
          final String btProjectId = bt.getProjectId();
          if (btProjectId.equals(projectId)) {
            currentBtResources = availableToMeResources;
          } else {
            currentBtResources = CollectionsUtil.filterMapByValues(
              myResources.getResourcesMap(btProjectId), availableToMeResources::containsValue);
          }
          final Map<String, Lock> lockedResources = feature.getLockedResources();
          if (!lockedResources.isEmpty()) {
            final Map<Resource, List<Lock>> matchResult = findResourcesUsages(lockedResources, currentBtResources);
            for (Map.Entry<Resource, List<Lock>> matched : matchResult.entrySet()) {
              Map<SBuildType, List<Lock>> resultMatch = result.get(matched.getKey());
              if (resultMatch == null) {
                resultMatch = new HashMap<>();
                result.put(matched.getKey(), resultMatch);
              }
              List<Lock> locksList = resultMatch.get(bt);
              if (locksList == null) {
                locksList = new ArrayList<>();
                resultMatch.put(bt, locksList);
              }
              locksList.addAll(matched.getValue());
            }
          }
        }
      }
    }
    return result;
  }

  /**
   * Resolves locks against given set of resources
   *
   * @param locks locks to resolve
   * @param resources resources to use
   * @return a list of locks taken for each resource given
   */
  @NotNull
  private Map<Resource, List<Lock>> findResourcesUsages(@NotNull final Map<String, Lock> locks,
                                                        @NotNull final Map<String, Resource> resources) {
    final Map<Resource, List<Lock>> result = new HashMap<>();
    for (Map.Entry<String, Lock> e: locks.entrySet()) {
      final Resource r = resources.get(e.getKey());
      if (r != null) {
        List<Lock> matchedList = result.get(r);
        if (matchedList == null) {
          matchedList = new ArrayList<>();
          result.put(r, matchedList);
        }
        matchedList.add(e.getValue());
      }
    }
    return result;
  }
}
