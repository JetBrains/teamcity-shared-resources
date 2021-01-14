/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

package jetbrains.buildServer.sharedResources.server.analysis;

import java.util.*;
import jetbrains.buildServer.serverSide.BuildTypeSettings;
import jetbrains.buildServer.serverSide.BuildTypeTemplate;
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

  @NotNull
  public FindUsagesResult findUsages(@NotNull final SProject project,
                                     @NotNull final Resource resource) { // <- here we look at a single resource.
    // start at project, go down the tree, look for overrides

    final Map<SBuildType, List<Lock>> buildTypes = new HashMap<>();
    final Map<BuildTypeTemplate, List<Lock>> templates = new HashMap<>();

    final Map<String, Map<String, Resource>> treeResources = new HashMap<>();
    final List<BuildTypeSettings> lookupScope = getLookupScope(project);
    for (BuildTypeSettings btSettings: lookupScope) {
      Map<String, Resource> currentBtResources = treeResources.computeIfAbsent(btSettings.getProject().getProjectId(), myResources::getResourcesMap);
      // check that resource available for buildType
      if (currentBtResources.containsValue(resource)) {
        // if it is -> search for usages
        final Collection<SharedResourcesFeature> features = myFeatures.searchForFeatures(btSettings);
        for (SharedResourcesFeature feature: features) {
          // for each feature
          //  get locked resources
          final Map<String, Lock> lockedResources = feature.getLockedResources();
          //  collect usages of current resource
          if (!lockedResources.isEmpty() && lockedResources.containsKey(resource.getName())) { // <- for now assume single feature has _single_ lock on resource
            List<Lock> storedUsages;
            if (btSettings instanceof SBuildType) {
              storedUsages = buildTypes.computeIfAbsent((SBuildType)btSettings, k -> new ArrayList<>());
            } else {
              storedUsages = templates.computeIfAbsent((BuildTypeTemplate)btSettings, k-> new ArrayList<>());
            }
            storedUsages.add(lockedResources.get(resource.getName()));
          }
        }
      }
    }

    return new FindUsagesResult(buildTypes, templates);
  }

  public Map<String, Resource> findUsedResources(@NotNull final SProject project) {
    final Map<String, Resource> result = new HashMap<>();
    final String projectId = project.getProjectId();
    final Map<String, Resource> availableToMeResources = myResources.getResourcesMap(projectId);
    if (!availableToMeResources.isEmpty()) {
      final List<BuildTypeSettings> lookupScope = getLookupScope(project);
      for (final BuildTypeSettings btSettings: lookupScope) {
        final Collection<SharedResourcesFeature> features = myFeatures.searchForFeatures(btSettings);
        if (!features.isEmpty()) {
          Map<String, Resource> currentBtResources;
          final String btProjectId = btSettings.getProject().getProjectId();
          if (btProjectId.equals(projectId)) {
            currentBtResources = availableToMeResources;
          } else {
            currentBtResources = CollectionsUtil.filterMapByValues(
              myResources.getResourcesMap(btProjectId), availableToMeResources::containsValue);
          }
          if (!currentBtResources.isEmpty()) {
            for (SharedResourcesFeature feature : features) {
              final Map<String, Lock> lockedResources = feature.getLockedResources();
              if (!lockedResources.isEmpty()) {
                for (String resourceName : lockedResources.keySet()) {
                  if (currentBtResources.containsKey(resourceName)) {
                    result.put(resourceName, currentBtResources.get(resourceName));
                  }
                }
              }
            }
          }
        }
      }
    }
    return result;
  }

  @NotNull
  private List<BuildTypeSettings> getLookupScope(final @NotNull SProject project) {
    final List<BuildTypeSettings> lookupScope = new ArrayList<>();
    lookupScope.addAll(project.getBuildTypes());
    lookupScope.addAll(project.getBuildTypeTemplates());
    return lookupScope;
  }
}
