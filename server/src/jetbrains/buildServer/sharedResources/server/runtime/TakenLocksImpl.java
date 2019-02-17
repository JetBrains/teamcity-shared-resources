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

package jetbrains.buildServer.sharedResources.server.runtime;

import java.util.*;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.buildDistribution.QueuedBuildInfo;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.TakenLock;
import jetbrains.buildServer.sharedResources.model.resources.CustomResource;
import jetbrains.buildServer.sharedResources.model.resources.QuotedResource;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceType;
import jetbrains.buildServer.sharedResources.server.feature.Locks;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeature;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.util.CollectionsUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class TakenLocksImpl implements TakenLocks {

  @NotNull
  private final Locks myLocks;

  @NotNull
  private final Resources myResources;

  @NotNull
  private final LocksStorage myLocksStorage;

  @NotNull
  private final SharedResourcesFeatures myFeatures;

  public TakenLocksImpl(@NotNull final Locks locks,
                        @NotNull final Resources resources,
                        @NotNull final LocksStorage locksStorage,
                        @NotNull final SharedResourcesFeatures features) {
    myLocks = locks;
    myResources = resources;
    myLocksStorage = locksStorage;
    myFeatures = features;
  }

  @NotNull
  @Override
  public Map<Resource, TakenLock> collectTakenLocks(@NotNull final Collection<SRunningBuild> runningBuilds,
                                                    @NotNull final Collection<QueuedBuildInfo> queuedBuilds) {
    final Map<Resource, TakenLock> result = new HashMap<>();
    final Map<String, Map<String, Resource>> cachedResources = new HashMap<>();
    for (SRunningBuild build: runningBuilds) {
      final SBuildType buildType = build.getBuildType();
      if (buildType != null) {
        final Collection<SharedResourcesFeature> features = myFeatures.searchForFeatures(buildType);
        if (features.isEmpty()) continue;
        // at this point we have features
        final BuildPromotionEx bpEx = (BuildPromotionEx) ((RunningBuildEx) build).getBuildPromotionInfo();
        Map<String, Lock> locks;
        if (myLocksStorage.locksStored(bpEx)) { // lock values are already resolved
          locks = myLocksStorage.load(bpEx);
        } else {
          locks = myLocks.fromBuildFeaturesAsMap(features); // in future: <String, Set<Lock>>
          // here we need to look for values in build promotion. build is running -> we have values in build promotion parameters
        }
        if (locks.isEmpty()) continue;
        // get resources defined in project tree, respecting inheritance
        final Map<String, Resource> resources = getResources(buildType.getProjectId(), cachedResources);
        // resolve locks against resources defined in project tree
        for (Map.Entry<String, Lock> entry: locks.entrySet()) {
          // collection, promotion, resource, lock
          final Resource resource = resources.get(entry.getKey());
          if (resource != null) {
            addLockToTaken(result, bpEx, resource, entry.getValue());
          }
        }
      }
    }

    for (QueuedBuildInfo build : queuedBuilds) {
      BuildPromotionEx bpEx = (BuildPromotionEx) build.getBuildPromotionInfo();
      final BuildTypeEx buildType = bpEx.getBuildType();
      if (buildType != null) {
        final Collection<SharedResourcesFeature> features = myFeatures.searchForFeatures(buildType);
        if (features.isEmpty()) continue;
        Map<String, Lock> locks = myLocks.fromBuildFeaturesAsMap(features); // in future: <String, Set<Lock>>
        if (locks.isEmpty()) continue;
        // get resources defined in project tree, respecting inheritance
        final Map<String, Resource> resources = getResources(buildType.getProjectId(), cachedResources);
        for (Map.Entry<String, Lock> entry: locks.entrySet()) {
          // collection, promotion, resource, lock
          final Resource resource = resources.get(entry.getKey());
          if (resource != null) {
            addLockToTaken(result, bpEx, resource, entry.getValue());
          }
        }
      }
    }
    return result;
  }

  @NotNull
  private Map<String, Resource> getResources(@NotNull final String btProjectId,
                                             @NotNull final Map<String, Map<String, Resource>> cachedResources) {
    return cachedResources.computeIfAbsent(btProjectId, myResources::getResourcesMap);
  }

  @NotNull
  @Override // todo: support several locks on resource here too -> Map<Resource, Collection<Lock>>
  public Map<Resource, Lock> getUnavailableLocks(@NotNull Collection<Lock> locksToTake,
                                                 @NotNull Map<Resource, TakenLock> takenLocks,
                                                 @NotNull String projectId,
                                                 @NotNull final DistributionDataAccessor distributionDataAccessor,
                                                 @NotNull final BuildPromotion buildPromotion) {
    final Map<String, Resource> resources = myResources.getResourcesMap(projectId);
    final Map<Resource, Lock> result = new HashMap<>();
    for (Lock lock : locksToTake) {
      final Resource resource = resources.get(lock.getName());
      if (resource != null) {
        if (!resource.isEnabled() || !checkAgainstResource(lock, takenLocks, resource, distributionDataAccessor, buildPromotion)) {
          result.put(resource, lock);
        }
      }
    }
    return result;
  }

  @NotNull
  @Override
  public Map<Resource, Lock> getUnavailableLocks(@NotNull final Map<String, Lock> locksToTake,
                                                 @NotNull final Map<Resource, TakenLock> takenLocks,
                                                 @NotNull final DistributionDataAccessor distributionDataAccessor,
                                                 @NotNull final Map<String, Resource> chainNodeResources,
                                                 @NotNull final Map<Resource, Map<BuildPromotionEx, Lock>> chainLocks,
                                                 @NotNull final BuildPromotion buildPromotion) {
    final Map<Resource, Lock> result = new HashMap<>();
    Map<Resource, TakenLock> chainTakenLocks = purifyTakenLocks(takenLocks, chainLocks);
    locksToTake.forEach((name, lock) -> {
      final Resource resource = chainNodeResources.get(name);
      if (resource != null) {
        if (!resource.isEnabled() || !checkAgainstResource(lock, chainTakenLocks, resource, distributionDataAccessor, buildPromotion)) {
          result.put(resource, lock);
        }
      }
    });

    return result;
  }

  private Map<Resource, TakenLock> purifyTakenLocks(final Map<Resource, TakenLock> takenLocks,
                                                    final Map<Resource, Map<BuildPromotionEx, Lock>> chainLocks) {
    Map<Resource, TakenLock> result = new HashMap<>();
    takenLocks.forEach((rc, tl) -> {
      Map<BuildPromotionEx, Lock> chainTakenLock = chainLocks.get(rc);
      if (chainTakenLock != null) {
        result.put(rc, new TakenLock(rc,
                                     CollectionsUtil.filterMapByKeys(tl.getReadLocks(), key -> !chainTakenLock.containsKey(key)),
                                     CollectionsUtil.filterMapByKeys(tl.getWriteLocks(), key -> !chainTakenLock.containsKey(key))));
      } else {
        result.put(rc, tl);
      }
    });
    return result;
  }


  private void addLockToTaken(@NotNull final Map<Resource, TakenLock> takenLocks,
                              @NotNull final BuildPromotionEx bpEx,
                              @NotNull final Resource resource,
                              @NotNull final Lock lock) {
    final TakenLock takenLock = getOrCreateTakenLock(takenLocks, resource);
    takenLock.addLock(bpEx, lock);
  }

  private TakenLock getOrCreateTakenLock(@NotNull final Map<Resource, TakenLock> takenLocks,
                                         @NotNull final Resource resource) {
    return takenLocks.computeIfAbsent(resource, TakenLock::new);
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean checkAgainstResource(@NotNull final Lock lock,
                                       @NotNull final Map<Resource, TakenLock> takenLocks,
                                       @NotNull final Resource resource,
                                       @NotNull final DistributionDataAccessor distributionDataAccessor,
                                       @NotNull final BuildPromotion buildPromotion) {
    boolean result = true;
    if (ResourceType.QUOTED.equals(resource.getType())) {
      result = checkAgainstQuotedResource(lock, takenLocks, (QuotedResource) resource, distributionDataAccessor);
    } else if (ResourceType.CUSTOM.equals(resource.getType())) {
      result = checkAgainstCustomResource(lock, takenLocks, (CustomResource) resource, distributionDataAccessor, buildPromotion);
    }
    return result;
  }

  private boolean checkAgainstCustomResource(@NotNull final Lock lock,
                                             @NotNull final Map<Resource, TakenLock> takenLocks,
                                             @NotNull final CustomResource resource,
                                             @NotNull final DistributionDataAccessor distributionDataAccessor,
                                             @NotNull final BuildPromotion buildPromotion) {
    boolean result = true;
    // what type of lock do we have
    // write            -> all
    // read with value  -> specific
    // read             -> any
    final TakenLock takenLock = getOrCreateTakenLock(takenLocks, resource);
    switch (lock.getType()) {
      case READ:   // check at least one value is available
        // check for unique writeLocks
        if (distributionDataAccessor.getFairSet().contains(lock.getName())) {
          result = false;
          break;
        }

        // check for write locks
        if (takenLock.hasWriteLocks()) { // ALL values are locked
          result = false;
          break;
        }
        // 2) check for quota (read + write)
        if (resource.getValues().size() <= takenLock.getLocksCount()) {
          // quota exceeded
          result = false;
          break;
        }
        // 3) SPECIFIC case
        if (!"".equals(lock.getValue())) { // we have custom lock
          final String requiredValue = lock.getValue();
          final Set<String> takenValues = new HashSet<>();
          takenValues.addAll(takenLock.getReadLocks().values());
          takenValues.addAll(takenLock.getWriteLocks().values());
          // get resource value affinity with other builds
          takenValues.addAll(distributionDataAccessor.getResourceAffinity().getOtherAssignedValues(resource, buildPromotion));
          if (takenValues.contains(requiredValue)) {
            result = false;
            break;
          }
        }
        break;
      case WRITE:
        // 'ALL' case
        if (takenLock.hasReadLocks() || takenLock.hasWriteLocks()) {
          distributionDataAccessor.getFairSet().add(lock.getName());
          result = false;
          break;
        }
        break;
    }
    return result;
  }

  private boolean checkAgainstQuotedResource(@NotNull final Lock lock,
                                             @NotNull final Map<Resource, TakenLock> takenLocks,
                                             @NotNull final QuotedResource resource,
                                             @NotNull final DistributionDataAccessor distributionDataAccessor) {
    boolean result = true;
    final TakenLock takenLock = getOrCreateTakenLock(takenLocks, resource);
    switch (lock.getType()) {
      case READ:
        // some build requested write lock on the current resource before us
        if (distributionDataAccessor.getFairSet().contains(resource.getId())) {
          result = false;
          break;
        }
        // Check that no write lock exists
        if (takenLock.hasWriteLocks()) {
          result = false;
          break;
        }
        if (isOverQuota(takenLock, resource)) {
          result = false;
          break;
        }
        break;
      case WRITE:
        // if anyone is accessing the resource
        if (takenLock.hasReadLocks() || takenLock.hasWriteLocks() || isOverQuota(takenLock, resource)) {
          distributionDataAccessor.getFairSet().add(resource.getId()); // remember write access request on the current resource
          result = false;
        }
    }
    return result;
  }

  private boolean isOverQuota(@NotNull final TakenLock takenLock, @NotNull final QuotedResource resource) {
    return !resource.isInfinite() && takenLock.getLocksCount() >= resource.getQuota();
  }
}
