

package jetbrains.buildServer.sharedResources.server.runtime;

import java.util.*;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.buildDistribution.QueuedBuildInfo;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
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
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.getReservedResourceAttributeKey;

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
  public Map<Resource, TakenLock> collectTakenLocks(@NotNull final Collection<RunningBuildEx> runningBuilds,
                                                    @NotNull final Collection<QueuedBuildInfo> queuedBuilds) {
    final Map<Resource, TakenLock> result = new HashMap<>();
    final Map<String, Map<String, Resource>> cachedResources = new HashMap<>();
    for (RunningBuildEx build : runningBuilds) {
      final SBuildType buildType = build.getBuildType();
      if (buildType != null) {
        final Collection<SharedResourcesFeature> features = myFeatures.searchForFeatures(build.getBuildPromotion());
        if (features.isEmpty()) continue;
        // at this point we have features
        final BuildPromotionEx bpEx = (BuildPromotionEx)build.getBuildPromotionInfo();
        Map<String, Lock> locks;
        if (myLocksStorage.locksStored(bpEx)) { // lock values are already resolved
          locks = myLocksStorage.load(bpEx);
        } else {
          locks = myLocks.fromBuildFeaturesAsMap(features); // in the future: <String, Set<Lock>>
        }
        if (locks.isEmpty()) continue;
        // get resources defined in project tree, respecting inheritance
        final Map<String, Resource> resources = getResources(buildType.getProjectId(), cachedResources);
        // resolve locks against resources defined in project tree
        locks.forEach((name, lock) -> {
          // collection, promotion, resource, lock
          final Resource resource = resources.get(name);
          if (resource != null) {
            if (resource instanceof CustomResource
                && lock.getType() == LockType.READ
                && lock.getValue().equals("")) { // ANY LOCK
              String reservedValue = (String)bpEx.getAttribute(getReservedResourceAttributeKey(resource.getId()));
              if (reservedValue != null) {
                addLockToTaken(result, bpEx, resource, Lock.createFrom(lock, reservedValue));
              } else {
                addLockToTaken(result, bpEx, resource, lock);
              }
            } else {
              addLockToTaken(result, bpEx, resource, lock);
            }
          }
        });
      }
    }

    for (QueuedBuildInfo build : queuedBuilds) {
      BuildPromotionEx bpEx = (BuildPromotionEx)build.getBuildPromotionInfo();
      final BuildTypeEx buildType = bpEx.getBuildType();
      if (buildType != null) {
        final Collection<SharedResourcesFeature> features = myFeatures.searchForFeatures(bpEx);
        if (features.isEmpty()) continue;
        Map<String, Lock> locks = myLocks.fromBuildFeaturesAsMap(features); // in the future: <String, Set<Lock>>
        if (locks.isEmpty()) continue;
        // get resources defined in project tree, respecting inheritance
        final Map<String, Resource> resources = getResources(buildType.getProjectId(), cachedResources);
        for (Map.Entry<String, Lock> entry : locks.entrySet()) {
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

  @Override
  public Map<Resource, String> getUnavailableLocks(@NotNull final Collection<Lock> locksToTake,
                                                   @NotNull final Map<Resource, TakenLock> takenLocks,
                                                   @NotNull final String projectId,
                                                   @NotNull final DistributionDataAccessor distributionDataAccessor,
                                                   @NotNull final BuildPromotion promotion) {
    final Map<String, Resource> resources = myResources.getResourcesMap(projectId);
    final Map<Resource, String> result = new HashMap<>();
    locksToTake.forEach(lock -> {
      final Resource resource = resources.get(lock.getName());
      if (resource != null) {
        if (!resource.isEnabled()) {
          result.put(resource, "resource is disabled");
        } else {
          checkAgainstResource(lock, takenLocks, resource, distributionDataAccessor, promotion, result);
        }
      }
    });
    return result;
  }

  @Override
  public Map<Resource, String> getUnavailableLocks(@NotNull final Map<String, Lock> locksToTake,
                                                   @NotNull final Map<Resource, TakenLock> takenLocks,
                                                   @NotNull final DistributionDataAccessor distributionDataAccessor,
                                                   @NotNull final Map<String, Resource> chainNodeResources,
                                                   @NotNull final Map<Resource, Map<BuildPromotionEx, Lock>> chainLocks,
                                                   @NotNull final BuildPromotion promotion) {
    final Map<Resource, String> result = new HashMap<>();
    Map<Resource, TakenLock> chainTakenLocks = purifyTakenLocks(takenLocks, chainLocks);
    locksToTake.forEach((name, lock) -> {
      final Resource resource = chainNodeResources.get(name);
      if (resource != null) {
        if (!resource.isEnabled()) {
          result.put(resource, "resource is disabled");
        } else {
          checkAgainstResource(lock, chainTakenLocks, resource, distributionDataAccessor, promotion, result);
        }
      }
    });
    return result;
  }

  @NotNull
  private Map<String, Resource> getResources(@NotNull final String btProjectId,
                                             @NotNull final Map<String, Map<String, Resource>> cachedResources) {
    return cachedResources.computeIfAbsent(btProjectId, myResources::getResourcesMap);
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

  private void checkAgainstResource(@NotNull final Lock lock,
                                    @NotNull final Map<Resource, TakenLock> takenLocks,
                                    @NotNull final Resource resource,
                                    @NotNull final DistributionDataAccessor distributionDataAccessor,
                                    @NotNull final BuildPromotion buildPromotion,
                                    @NotNull final Map<Resource, String> result) {
    if (ResourceType.QUOTED.equals(resource.getType())) {
      checkAgainstQuotedResource(lock, takenLocks, (QuotedResource)resource, distributionDataAccessor, buildPromotion, result);
    } else if (ResourceType.CUSTOM.equals(resource.getType())) {
      checkAgainstCustomResource(lock, takenLocks, (CustomResource)resource, distributionDataAccessor, buildPromotion, result);
    }
  }

  private void checkAgainstCustomResource(@NotNull final Lock lock,
                                          @NotNull final Map<Resource, TakenLock> takenLocks,
                                          @NotNull final CustomResource resource,
                                          @NotNull final DistributionDataAccessor distributionDataAccessor,
                                          @NotNull final BuildPromotion buildPromotion,
                                          @NotNull final Map<Resource, String> result) {
    // what type of lock do we have:
    // write            -> all
    // read with value  -> specific
    // read             -> any
    final TakenLock takenLock = getOrCreateTakenLock(takenLocks, resource);
    switch (lock.getType()) {
      case READ:   // check at least one value is available
        // check for unique writeLocks
        List<BuildPromotion> promosInFairSet = distributionDataAccessor.getFairSet().get(resource.getId());
        if (promosInFairSet != null && !promosInFairSet.isEmpty()) {
          String description = describeLockingPromotions(promosInFairSet);
          result.put(resource, "(write lock requested by " + description + ")");
          break;
        }
        // check for write locks
        if (takenLock.hasWriteLocks()) {
          // write lock can be in chain head, read locks can be in chain parts
          String description = describeLockingPromotions(takenLock.getReadLocks().keySet(), takenLock.getWriteLocks().keySet());
          result.put(resource, "(locked by " + description + ")");
          break;
        }
        // 2) SPECIFIC case
        if (!"".equals(lock.getValue())) { // we have custom lock
          final String requiredValue = lock.getValue();
          final Set<String> takenValues = new HashSet<>();
          takenValues.addAll(takenLock.getReadLocks().values());
          takenValues.addAll(takenLock.getWriteLocks().values());
          // get resource value affinity with other builds
          takenValues.addAll(distributionDataAccessor.getResourceAffinity().getOtherAssignedValues(resource, buildPromotion));
          if (takenValues.contains(requiredValue)) {
            StringBuilder builder = new StringBuilder("(required value '" + requiredValue + "' is occupied");
            BuildPromotionEx occupyingPromo = occupyingPromo(takenLock, requiredValue);
            if (occupyingPromo != null) {
              String description = describeLockingPromotions(Collections.singleton(occupyingPromo));
              builder.append(" by ");
              builder.append(description);
            }
            builder.append(")");
            result.put(resource, builder.toString());
            break;
          }
        }
        // 3) check for any unoccupied value
        if (resource.getValues().size() <= takenLock.getLocksCount()) {
          String description = describeLockingPromotions(takenLock.getReadLocks().keySet());
          result.put(resource, "(all available values are occupied by " + description + ")");
          break;
          // quota exceeded
        }
        break;
      case WRITE:
        // 'ALL' case
        if (takenLock.hasReadLocks() || takenLock.hasWriteLocks()) {
          addToFairSet(distributionDataAccessor, resource, buildPromotion);
          String description = describeLockingPromotions(takenLock.getReadLocks().keySet(), takenLock.getWriteLocks().keySet());
          result.put(resource, "(locked by " + description + ")");
          break;
        }
        break;
    }
  }

  private void checkAgainstQuotedResource(@NotNull final Lock lock,
                                          @NotNull final Map<Resource, TakenLock> takenLocks,
                                          @NotNull final QuotedResource resource,
                                          @NotNull final DistributionDataAccessor distributionDataAccessor,
                                          @NotNull final BuildPromotion buildPromotion,
                                          @NotNull final Map<Resource, String> result) {
    final TakenLock takenLock = getOrCreateTakenLock(takenLocks, resource);
    switch (lock.getType()) {
      case READ:
        // some build requested write lock on the current resource before us
        List<BuildPromotion> promosInFairSet = distributionDataAccessor.getFairSet().get(resource.getId());
        if (promosInFairSet != null && !promosInFairSet.isEmpty()) {
          String description = describeLockingPromotions(promosInFairSet);
          result.put(resource, "(write lock requested by " + description + ")");
          break;
        }
        // Check that no WriteLocks exist
        if (takenLock.hasWriteLocks()) {
          String description = describeLockingPromotions(takenLock.getReadLocks().keySet(), takenLock.getWriteLocks().keySet());
          result.put(resource, "(locked by " + description + ")");
          break;
        }
        if (isOverQuota(takenLock, resource)) {
          if (resource.getQuota() == 0) {
            result.put(resource, "(has zero quota available)");
          } else {
            String description = describeLockingPromotions(takenLock.getReadLocks().keySet());
            result.put(resource, "(locked by " + description + ")");
          }
          break;
        }
        break;
      case WRITE:
        // if anyone is accessing the resource
        if (takenLock.hasReadLocks() || takenLock.hasWriteLocks() || isOverQuota(takenLock, resource)) {
          addToFairSet(distributionDataAccessor, resource, buildPromotion);
          if (resource.getQuota() == 0) {
            result.put(resource, "(has zero quota available)");
          } else {
            String description = describeLockingPromotions(takenLock.getReadLocks().keySet(), takenLock.getWriteLocks().keySet());
            result.put(resource, "(locked by " + description + ")");
          }
        }
    }
  }

  @SafeVarargs
  @NotNull
  private final String describeLockingPromotions(@NotNull final Collection<? extends BuildPromotion>... promotions) {
    List<String> parts = new ArrayList<>();
    for (Collection<? extends BuildPromotion> collection : promotions) {
      collection.stream()
                .map(BuildPromotion::getBuildType)
                .filter(Objects::nonNull)
                .map(SBuildType::getExtendedFullName)
                .sorted()
                .forEach(parts::add);
    }
    return parts.isEmpty() ? "unknown" : String.join(", ", parts);
  }

  private boolean isOverQuota(@NotNull final TakenLock takenLock, @NotNull final QuotedResource resource) {
    return !resource.isInfinite() && takenLock.getLocksCount() >= resource.getQuota();
  }

  private void addToFairSet(@NotNull final DistributionDataAccessor distributionDataAccessor,
                            @NotNull final Resource resource,
                            @NotNull final BuildPromotion buildPromotion) {
    distributionDataAccessor.getFairSet()
                            .computeIfAbsent(resource.getId(), it -> new ArrayList<>())
                            .add(buildPromotion);
  }

  @Nullable
  private BuildPromotionEx occupyingPromo(@NotNull final TakenLock takenLock,
                                          @NotNull final String value) {
    for (Map.Entry<BuildPromotionEx, String> entry : takenLock.getReadLocks().entrySet()) {
      if (value.equals(entry.getValue())) {
        return entry.getKey();
      }
    }

    for (Map.Entry<BuildPromotionEx, String> entry : takenLock.getWriteLocks().entrySet()) {
      if (entry.getValue().contains(value)) {
        return entry.getKey();
      }
    }
    return null;
  }
}