/*
 * Copyright 2000-2019 JetBrains s.r.o.
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

import gnu.trove.TLongObjectHashMap;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.buildDistribution.DistributionCycleExtension;
import jetbrains.buildServer.serverSide.buildDistribution.QueuedBuildInfo;
import jetbrains.buildServer.serverSide.buildDistribution.WaitReason;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Storage for custom resource requested values during build distribution
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class ResourceAffinity {

  /**
   * Storage for actual locked values associated with the build
   * One value per resource per build is currently supported
   */
  private final Map<String, TLongObjectHashMap<String>> myLockedValues = new HashMap<>();

  /**
   * Storage for custom resources locked by the build
   */
  private final TLongObjectHashMap<Set<String>> myBuildLockedResources = new TLongObjectHashMap<>();

  public ResourceAffinity(@NotNull final ExtensionHolder extensionHolder,
                          @NotNull final EventDispatcher<BuildServerListener> eventDispatcher) {
    extensionHolder.registerExtension(DistributionCycleExtension.class, getClass().getName(), new DistributionCycleExtension() {
      @Override
      public boolean buildDistributed(@NotNull final QueuedBuildInfo build, @Nullable final Object agent, boolean emulationMode) {
        return true;
      }

      @Override
      public boolean buildAwaits(@NotNull final QueuedBuildInfo build, @NotNull final WaitReason waitReason, boolean emulationMode) {
        // emulation mode must not touch real data
        if (!emulationMode) {
          release((BuildPromotionEx)build.getBuildPromotionInfo());
        }
        return true;
      }
    });
    eventDispatcher.addListener(new BuildServerAdapter() {
      @Override
      public void buildRemovedFromQueue(@NotNull final SQueuedBuild queued, final User user, final String comment) {
        if (user != null) {
          release(queued.getBuildPromotion());
        }
      }
    });
  }

  private final ReentrantReadWriteLock myLock = new ReentrantReadWriteLock();

  /**
   * Stores resource affinity
   *
   * @param promotion promotion to store resource affinity for
   * @param affinityMap map of requested resource values
   */
  public void store(@NotNull final BuildPromotion promotion,
                    @NotNull final Map<String, String> affinityMap) {
    myLock.writeLock().lock();
    try {
      final long promotionId = promotion.getId();
      affinityMap.forEach((resourceId, value) -> {
        // store the value
        myLockedValues.computeIfAbsent(resourceId, it -> new TLongObjectHashMap<>()).put(promotionId, value);
        Set<String> buildLockedResources = myBuildLockedResources.get(promotionId);
        if (buildLockedResources == null) {
          buildLockedResources = new HashSet<>();
          myBuildLockedResources.put(promotionId, buildLockedResources);
        }
        buildLockedResources.add(resourceId);
      });
    } finally {
      myLock.writeLock().unlock();
    }
  }

  /**
   * Releases all resource affinity entries for given promotion
   *
   * @param promotion build promotion to release affinity entries for
   */
  public void release(@NotNull final BuildPromotion promotion) {
    myLock.writeLock().lock();
    try {
      final long promotionId = promotion.getId();
      final Set<String> promotionResources = myBuildLockedResources.remove(promotionId);
      if (promotionResources != null) {
        promotionResources.forEach(resourceId -> { // for each resource remove value entry from second index
          final TLongObjectHashMap<String> lockedValues = myLockedValues.get(resourceId);
          if (lockedValues != null) {
            lockedValues.remove(promotionId);
          }
        });
      }
    } finally {
      myLock.writeLock().unlock();
    }
  }

  /**
   * Returns set of values assigned to other build promotions
   *
   * @param resource resource to compute for
   * @param currentPromotion promotion to compute the set for
   * @return set of values for the given resource,
   * assigned to promotions other that the given one
   */
  @NotNull
  public Set<String> getOtherAssignedValues(@NotNull final Resource resource,
                                     @NotNull final BuildPromotion currentPromotion) {
    myLock.readLock().lock();
    try {
      final TLongObjectHashMap<String> allValues = myLockedValues.get(resource.getId());
      if (allValues != null) {
        final long promotionId = currentPromotion.getId();
        final Set<String> result = new HashSet<>();
        allValues.forEachEntry((promoId, value) -> {
          if (promoId != promotionId) {
            result.add(value);
          }
          return true;
        });
        return result;
      }
      return Collections.emptySet();
    } finally {
      myLock.readLock().unlock();
    }
  }

  /**
   * Returns all requested resources with values for given promotion
   *
   * @param buildPromotion build promotion
   * @return map of resources with corresponding requested values
   */
  @NotNull
  public Map<String, String> getRequestedValues(@NotNull final BuildPromotion buildPromotion) {
    final Map<String, String> result = new HashMap<>();
    final long promotionId = buildPromotion.getId();
    final Set<String> buildLockedResources = myBuildLockedResources.get(promotionId);
    if (buildLockedResources != null) {
      buildLockedResources.forEach(resourceId -> {
        final TLongObjectHashMap<String> resourceLockedValues = myLockedValues.get(resourceId);
        if (resourceLockedValues != null) {
          String val = resourceLockedValues.get(promotionId);
          if (val != null) {
            result.put(resourceId, val);
          }
        }
      });
    }
    return result;
  }
}
