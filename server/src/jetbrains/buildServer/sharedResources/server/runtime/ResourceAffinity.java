/*
 * Copyright 2000-2022 JetBrains s.r.o.
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
import gnu.trove.TLongObjectIterator;
import java.util.*;
import javax.annotation.concurrent.NotThreadSafe;
import jetbrains.buildServer.serverSide.BuildPromotion;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import org.jetbrains.annotations.NotNull;

/**
 * Storage for custom resource requested values during build distribution
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@NotThreadSafe
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

  /**
   * Stores resource affinity
   *
   * @param promotion   promotion to store resource affinity for
   * @param affinityMap map ({@code resourceId -> value}) of requested resource values
   */
  public void store(@NotNull final BuildPromotion promotion,
                    @NotNull final Map<String, String> affinityMap) {
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

  }

  /**
   * Returns set of values assigned to other build promotions
   *
   * @param resource         resource to compute for
   * @param currentPromotion promotion to compute the set for
   * @return list of values for the given resource,
   * assigned to promotions other that the given one
   */
  @NotNull
  public List<String> getOtherAssignedValues(@NotNull final Resource resource,
                                             @NotNull final BuildPromotion currentPromotion) {
    // every promotion can lock at most one value of the resource
    final TLongObjectHashMap<String> allLockedValues = myLockedValues.get(resource.getId());
    if (allLockedValues != null) {
      final long promotionId = currentPromotion.getId();
      // list, as we allow duplicate values in custom resources, multiple instances of a value can be locked by other builds
      final List<String> result = new ArrayList<>();
      allLockedValues.forEachEntry((promoId, value) -> {
        if (promoId != promotionId) {
          result.add(value);
        }
        return true;
      });
      return result;
    }
    return Collections.emptyList();
  }

  /**
   * Clears stored resource affinity for builds that do not participate in current distribution cycle
   * Such builds appear when build is removed from the distribution cycle by other extensions
   * after SharedResources plugin has already processed the build
   *
   * @param currentPromotionIds ids of promotions that participate in current distribution cycle
   */
  public void actualize(@NotNull final Set<Long> currentPromotionIds) {
    final TLongObjectIterator<Set<String>> iterator = myBuildLockedResources.iterator();
    while (iterator.hasNext()) {
      iterator.advance();
      if (!currentPromotionIds.contains(iterator.key())) {
        Optional.ofNullable(iterator.value()).ifPresent(it -> it.forEach(resourceId -> myLockedValues.get(resourceId).remove(iterator.key())));
        iterator.remove();
      }
    }
  }
}
