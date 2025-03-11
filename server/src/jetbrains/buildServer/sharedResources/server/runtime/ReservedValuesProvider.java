

package jetbrains.buildServer.sharedResources.server.runtime;

import gnu.trove.TLongObjectHashMap;
import gnu.trove.TLongObjectIterator;
import gnu.trove.TLongObjectProcedure;
import java.util.*;
import javax.annotation.concurrent.NotThreadSafe;
import jetbrains.buildServer.serverSide.BuildPromotion;
import jetbrains.buildServer.serverSide.BuildPromotionEx;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import org.jetbrains.annotations.NotNull;

/**
 * Contains information about custom resource values reserved during the build distribution.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@NotThreadSafe
public class ReservedValuesProvider {

  /**
   * Storage for actual locked values associated with the build
   * One value per resource per build is currently supported
   */
  private final Map<String, TLongObjectHashMap<String>> myReservedValues = new HashMap<>();

  /**
   * Remembers values of the locks reserved for the current build promotion.
   *
   * @param promotion   promotion to whose reserved values to remember
   * @param reservedValues map ({@code resourceId -> value}) of requested resource values
   */
  public void rememberReservedValues(@NotNull final BuildPromotionEx promotion, @NotNull final Map<String, String> reservedValues) {
    final long promotionId = promotion.getId();
    reservedValues.forEach((resourceId, value) -> {
      // store the value
      myReservedValues.computeIfAbsent(resourceId, it -> new TLongObjectHashMap<>()).put(promotionId, value);
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
  public List<String> getValuesReservedByOtherBuilds(@NotNull final Resource resource,
                                                     @NotNull final BuildPromotion currentPromotion) {
    // every promotion can lock at most one value of the resource
    final TLongObjectHashMap<String> reservedValues = myReservedValues.get(resource.getId());
    if (reservedValues != null) {
      final long promotionId = currentPromotion.getId();
      // list, as we allow duplicate values in custom resources, multiple instances of a value can be locked by other builds
      final List<String> result = new ArrayList<>();
      reservedValues.forEachEntry((promoId, value) -> {
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
   * @param actualPromotionIds ids of promotions that participate in current distribution cycle
   */
  public void cleanupValuesReservedByObsoleteBuilds(@NotNull final Set<Long> actualPromotionIds) {
    // only retain values which belong to actual build promotions
    myReservedValues.forEach((resourceId, valuesMap) -> {
      valuesMap.retainEntries((id, val) -> actualPromotionIds.contains(id));
    });

    // cleanup empty maps
    myReservedValues.keySet().removeIf(resourceId -> myReservedValues.get(resourceId).isEmpty());
  }
}