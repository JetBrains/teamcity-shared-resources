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

import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.buildDistribution.BuildPromotionInfo;
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
import org.jetbrains.annotations.NotNull;

import java.util.*;

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
  public Map<String, TakenLock> collectTakenLocks(@NotNull final String projectId,
                                                  @NotNull final Collection<SRunningBuild> runningBuilds,
                                                  @NotNull final Collection<QueuedBuildInfo> queuedBuilds) {
    final Map<String, TakenLock> result = new HashMap<String, TakenLock>();
    for (SRunningBuild build : runningBuilds) {
      final SBuildType buildType = build.getBuildType();
      if (buildType != null) {
        final Collection<SharedResourcesFeature> features = myFeatures.searchForFeatures(buildType);
        if (features.isEmpty()) continue;
        BuildPromotionEx bpEx = (BuildPromotionEx) ((RunningBuildEx) build).getBuildPromotionInfo();
        Collection<Lock> locks;
        RunningBuildEx rbEx = (RunningBuildEx) build;
        if (myLocksStorage.locksStored(rbEx)) { // lock values are already resolved
          locks = myLocksStorage.load(rbEx).values();
        } else {
          locks = myLocks.fromBuildFeaturesAsMap(features).values();
        }
        addToTakenLocks(result, bpEx, locks);
      }
    }

    for (QueuedBuildInfo build : queuedBuilds) {
      BuildPromotionEx bpEx = (BuildPromotionEx) build.getBuildPromotionInfo();
      final BuildTypeEx buildType = bpEx.getBuildType();
      if (buildType != null) {
        final Collection<SharedResourcesFeature> features = myFeatures.searchForFeatures(buildType);
        if (features.isEmpty()) continue;
        final Collection<Lock> locks = myLocks.fromBuildFeaturesAsMap(features).values();
        addToTakenLocks(result, bpEx, locks);
      }
    }
    return result;
  }

  private void addToTakenLocks(@NotNull final Map<String, TakenLock> takenLocks,
                               @NotNull final BuildPromotionInfo bpInfo,
                               @NotNull final Collection<Lock> locks) {
    for (Lock lock : locks) {
      TakenLock takenLock = takenLocks.get(lock.getName());
      if (takenLock == null) {
        takenLock = new TakenLock();
        takenLocks.put(lock.getName(), takenLock);
      }
      takenLock.addLock(bpInfo, lock);
    }
  }

  @NotNull
  @Override
  public Collection<Lock> getUnavailableLocks(@NotNull Collection<Lock> locksToTake,
                                              @NotNull Map<String, TakenLock> takenLocks,
                                              @NotNull String projectId,
                                              @NotNull final Set<String> fairSet) {
    final Map<String, Resource> resources = myResources.asMap(projectId);
    final Collection<Lock> result = new ArrayList<Lock>();
    for (Lock lock : locksToTake) {
      final TakenLock takenLock = takenLocks.get(lock.getName());
      if (takenLock != null) {
        final Resource resource = resources.get(lock.getName());
        if (resource != null && resource.isEnabled() && !checkAgainstResource(lock, takenLocks, resource, fairSet)) {
          result.add(lock);
        }
      }
    }
    return result;
  }

  private boolean checkAgainstResource(@NotNull final Lock lock,
                                       @NotNull final Map<String, TakenLock> takenLocks,
                                       @NotNull final Resource resource,
                                       @NotNull final Set<String> fairSet) {
    boolean result = true;
    if (ResourceType.QUOTED.equals(resource.getType())) {
      result = checkAgainstQuotedResource(lock, takenLocks, (QuotedResource) resource, fairSet);
    } else if (ResourceType.CUSTOM.equals(resource.getType())) {
      result = checkAgainstCustomResource(lock, takenLocks, (CustomResource) resource, fairSet);
    }
    return result;
  }

  private boolean checkAgainstCustomResource(@NotNull final Lock lock,
                                             @NotNull final Map<String, TakenLock> takenLocks,
                                             @NotNull final CustomResource resource,
                                             @NotNull final Set<String> fairSet) {
    boolean result = true;
    // what type of lock do we have
    // write            -> all
    // read with value  -> specific
    // read             -> any
    final TakenLock takenLock = takenLocks.get(lock.getName());
    switch (lock.getType()) {
      case READ:   // check at least one value is available
        // check for unique writeLocks
        if (fairSet.contains(lock.getName())) {
          result = false;
          break;
        }

        // check for write locks
        if (takenLock.hasWriteLocks()) { // ALL values are locked
          result = false;
          break;
        }
        // 2) check for quota (read + write)
        if (resource.getValues().size() <= takenLock.getReadLocks().size() + takenLock.getWriteLocks().size()) {
          // quota exceeded
          result = false;
          break;
        }
        // 3) SPECIFIC case
        if (!"".equals(lock.getValue())) { // we have custom lock
          final String requiredValue = lock.getValue();
          final Set<String> takenValues = new HashSet<String>();
          takenValues.addAll(takenLock.getReadLocks().values());
          takenValues.addAll(takenLock.getWriteLocks().values());
          if (takenValues.contains(requiredValue)) {
            // value was already taken
            result = false;
            break;
          }
        }
        break;
      case WRITE:
        // 'ALL' case
        if (takenLock.hasReadLocks() || takenLock.hasWriteLocks()) {
          fairSet.add(lock.getName());
          result = false;
          break;
        }
        break;
    }
    return result;
  }

  private boolean checkAgainstQuotedResource(@NotNull final Lock lock,
                                             @NotNull final Map<String, TakenLock> takenLocks,
                                             @NotNull final QuotedResource resource,
                                             @NotNull final Set<String> fairSet) {
    boolean result = true;
    final TakenLock takenLock = takenLocks.get(lock.getName());
    switch (lock.getType()) {
      case READ:
        if (fairSet.contains(lock.getName())) { // some build requested write lock before us
          result = false;
          break;
        }
        // Check that no write lock exists
        if (takenLock.hasWriteLocks()) {
          result = false;
          break;
        }
        if (!resource.isInfinite()) {
          if (takenLock.getReadLocks().size() >= resource.getQuota()) {
            result = false;
            break;
          }
        }
        break;
      case WRITE:
        if (takenLock.hasReadLocks() || takenLock.hasWriteLocks()) { // if anyone is accessing the resource
          fairSet.add(lock.getName()); // remember write access request
          result = false;
        }
    }
    return result;
  }
}
