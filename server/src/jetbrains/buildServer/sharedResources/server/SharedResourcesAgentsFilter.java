

package jetbrains.buildServer.sharedResources.server;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.buildDistribution.*;
import jetbrains.buildServer.serverSide.impl.RunningBuildsManagerEx;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.sharedResources.model.TakenLock;
import jetbrains.buildServer.sharedResources.model.resources.CustomResource;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.server.feature.Locks;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeature;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.sharedResources.server.runtime.DistributionDataAccessor;
import jetbrains.buildServer.sharedResources.server.runtime.LocksStorage;
import jetbrains.buildServer.sharedResources.server.runtime.TakenLocks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.getReservedResourceAttributeKey;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class SharedResourcesAgentsFilter implements StartingBuildAgentsFilter {

  @NotNull
  private static final Logger LOG = Logger.getInstance(SharedResourcesAgentsFilter.class.getName());

  @NotNull
  private final SharedResourcesFeatures myFeatures;

  @NotNull
  private final Locks myLocks;

  @NotNull
  private final TakenLocks myTakenLocks;

  @NotNull
  private final RunningBuildsManagerEx myRunningBuildsManager;

  @NotNull
  private final ConfigurationInspector myInspector;

  @NotNull
  private final LocksStorage myLocksStorage;

  @NotNull
  private final Resources myResources;

  public SharedResourcesAgentsFilter(@NotNull final SharedResourcesFeatures features,
                                     @NotNull final Locks locks,
                                     @NotNull final TakenLocks takenLocks,
                                     @NotNull final RunningBuildsManagerEx runningBuildsManager,
                                     @NotNull final ConfigurationInspector inspector,
                                     @NotNull final LocksStorage locksStorage,
                                     @NotNull final Resources resources) {
    myFeatures = features;
    myLocks = locks;
    myTakenLocks = takenLocks;
    myRunningBuildsManager = runningBuildsManager;
    myInspector = inspector;
    myLocksStorage = locksStorage;
    myResources = resources;
  }

  @NotNull
  @Override
  public AgentsFilterResult filterAgents(@NotNull final AgentsFilterContext context) {
    final DistributionDataAccessor accessor = new DistributionDataAccessor(context);

    final QueuedBuildInfo queuedBuild = context.getStartingBuild();
    final Map<QueuedBuildInfo, SBuildAgent> canBeStarted = context.getDistributedBuilds();
    final List<RunningBuildEx> runningBuilds = myRunningBuildsManager.getRunningBuildsEx();

    final AtomicReference<Map<Resource, TakenLock>> takenLocks = new AtomicReference<>();
    // get or create our collection of resources
    WaitReason reason = null;
    final BuildPromotionEx myPromotion = (BuildPromotionEx)queuedBuild.getBuildPromotionInfo();
    accessor.getReservedValuesProvider().cleanupValuesReservedByObsoleteBuilds(canBeStarted.keySet().stream()
                                                                                           .map(QueuedBuildInfo::getBuildPromotionInfo)
                                                                                           .map(BuildPromotionInfo::getId)
                                                                                           .collect(Collectors.toSet()));

    if (TeamCityProperties.getBooleanOrTrue(SharedResourcesPluginConstants.RESOURCES_IN_CHAINS_ENABLED) && myPromotion.isPartOfBuildChain()) {
      LOG.debug("Queued build is part of build chain");
      final List<BuildPromotionEx> depPromos = myPromotion.getDependentCompositePromotions();
      if (depPromos.isEmpty()) {
        LOG.debug("Queued build does not have dependent composite promotions");
        reason = processSingleBuild(myPromotion, accessor, runningBuilds, canBeStarted, takenLocks, myPromotion, context.isEmulationMode());
      } else {
        LOG.debug("Queued build does have " + depPromos.size() + " dependent composite " + StringUtil.pluralize("promotion", depPromos.size()));
        // contains resources and locks that are INSIDE the build chain
        final Map<Resource, Map<BuildPromotionEx, Lock>> chainLocks = new HashMap<>(); // resource -> {promotion -> lock}
        final Map<String, Map<String, Resource>> chainResources = new HashMap<>(); // projectID -> {name, resource}
        // first - get top of the chain. Builds that are already running.
        // they have locks already taken
        depPromos.stream()
                 .filter(it -> it.getProjectId() != null)
                 .forEach(promo -> {
                   if (myLocksStorage.locksStored(promo)) {
                     LOG.debug("build promotion" + promo.getId() + " is running. Loading locks");
                     final Map<String, Lock> currentNodeLocks = myLocksStorage.load(promo);
                     if (!currentNodeLocks.isEmpty()) {
                       chainResources.computeIfAbsent(promo.getProjectId(), myResources::getResourcesMap);
                       // if there are locks - resolve locks against resources according to project hierarchy of composite build
                       resolve(chainLocks, chainResources.get(promo.getProjectId()), promo, currentNodeLocks);
                     }
                   }
                 });

        // rest are queued builds.
        // make sure queued builds can start.
        // builds inside composite build chain are not affected by the locks taken in the same chain
        final List<SQueuedBuild> queued = depPromos.stream()
                                                   .map(BuildPromotion::getQueuedBuild)
                                                   .filter(Objects::nonNull)
                                                   .collect(Collectors.toList());
        for (SQueuedBuild compositeQueuedBuild : queued) {
          final SBuildType compositeQueuedBuildType = getBuildTypeSafe(compositeQueuedBuild);
          if (compositeQueuedBuildType == null) continue;
          final Collection<SharedResourcesFeature> features = myFeatures.searchForFeatures(compositeQueuedBuildType);
          if (!features.isEmpty()) {
            final Map<String, Lock> locksToTake = myLocks.fromBuildFeaturesAsMap(features);
            if (!locksToTake.isEmpty()) {
              // resolve locks that build wants to take against actual resources
              chainResources.computeIfAbsent(compositeQueuedBuildType.getProjectId(), myResources::getResourcesMap);
              reason = processBuildInChain(accessor, runningBuilds, canBeStarted, takenLocks,
                                           chainResources.get(compositeQueuedBuildType.getProjectId()),
                                           chainLocks, locksToTake, compositeQueuedBuild.getBuildPromotion(), context.isEmulationMode());
              if (reason != null) {
                if (LOG.isDebugEnabled()) {
                  LOG.debug("Preventing start of the queued build [" + compositeQueuedBuild + "] with reason: [" + reason.getDescription() + "]");
                }
                break;
              }
            }
          }
        }
        // process build itself
        if (reason == null) {
          final BuildTypeEx promoBuildType = myPromotion.getBuildType();
          if (promoBuildType != null) {
            final String projectId = promoBuildType.getProjectId();
            chainResources.computeIfAbsent(projectId, myResources::getResourcesMap);
            final Collection<SharedResourcesFeature> features = myFeatures.searchForFeatures(myPromotion);

            if (!features.isEmpty()) {
              reason = checkForInvalidLocks(promoBuildType);
            }
            final Map<String, Lock> locksToTake = myLocks.fromBuildFeaturesAsMap(features);
            if (!locksToTake.isEmpty()) {
              reason = processBuildInChain(accessor, runningBuilds, canBeStarted, takenLocks, chainResources.get(projectId), chainLocks, locksToTake, myPromotion,
                                           context.isEmulationMode());
            }
          }
        }
      }
    } else {
      reason = processSingleBuild(myPromotion, accessor, runningBuilds, canBeStarted, takenLocks, myPromotion, context.isEmulationMode());
    }
    final AgentsFilterResult result = new AgentsFilterResult();
    result.setWaitReason(reason);
    return result;
  }

  @Nullable
  private SBuildType getBuildTypeSafe(@NotNull final SQueuedBuild queuedBuild) {
    try {
      return queuedBuild.getBuildType();
    } catch (BuildTypeNotFoundException ignored) {
    }
    return null;
  }

  @Nullable
  private WaitReason processBuildInChain(@NotNull final DistributionDataAccessor accessor,
                                         @NotNull final List<RunningBuildEx> runningBuilds,
                                         @NotNull final Map<QueuedBuildInfo, SBuildAgent> canBeStarted,
                                         @NotNull final AtomicReference<Map<Resource, TakenLock>> takenLocks,
                                         @NotNull final Map<String, Resource> chainNodeResources,
                                         @NotNull final Map<Resource, Map<BuildPromotionEx, Lock>> chainLocks,
                                         @NotNull final Map<String, Lock> locksToTake,
                                         @NotNull final BuildPromotion buildPromotion,
                                         boolean emulationMode) {
    final String projectId = buildPromotion.getProjectId();
    WaitReason reason = null;
    if (projectId != null) {
      takenLocks.compareAndSet(null, myTakenLocks.collectTakenLocks(runningBuilds, canBeStarted.keySet()));
      final Map<Resource, String> unavailableLocks = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks.get(), accessor, chainNodeResources, chainLocks, buildPromotion);
      if (!unavailableLocks.isEmpty()) {
        reason = createWaitReason(unavailableLocks);
      } else {
        storeResourcesAffinity((BuildPromotionEx)buildPromotion, projectId, takenLocks.get(), locksToTake.values(), accessor, emulationMode); // assign ANY locks here
        // if we are here, then the build will pass on to be started
      }
    }
    return reason;
  }

  private WaitReason processSingleBuild(@NotNull final BuildPromotionEx buildPromotion,
                                        @NotNull final DistributionDataAccessor accessor,
                                        @NotNull final List<RunningBuildEx> runningBuilds,
                                        @NotNull final Map<QueuedBuildInfo, SBuildAgent> canBeStarted,
                                        @NotNull final AtomicReference<Map<Resource, TakenLock>> takenLocks,
                                        @NotNull final BuildPromotion promotion,
                                        final boolean emulationMode) {
    final String projectId = buildPromotion.getProjectId();
    final SBuildType buildType = buildPromotion.getBuildType();
    WaitReason reason = null;
    if (buildType != null && projectId != null) {
      final Collection<SharedResourcesFeature> features = myFeatures.searchForFeatures(buildPromotion);
      if (!features.isEmpty()) {
        reason = checkForInvalidLocks(buildType);
        if (reason == null) {
          // Collection<Lock> ---> Collection<ResolvedLock> (i.e. lock against resolved resource. With project and so on)
          final Collection<Lock> locksToTake = myLocks.fromBuildFeaturesAsMap(features).values();
          if (!locksToTake.isEmpty()) {
            takenLocks.compareAndSet(null, myTakenLocks.collectTakenLocks(runningBuilds, canBeStarted.keySet()));
            // Collection<Lock> --> Collection<ResolvedLock>. For quoted - number of insufficient quotes, for custom -> custom values
            final Map<Resource, String> unavailableLocks = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks.get(), projectId, accessor, promotion);
            if (!unavailableLocks.isEmpty()) {
              reason = createWaitReason(unavailableLocks);
              if (LOG.isDebugEnabled()) {
                LOG.debug("Preventing start of the queued build [" + buildPromotion.getQueuedBuild() + "] with reason: [" + reason.getDescription() + "]");
              }
            } else {
              storeResourcesAffinity(buildPromotion, projectId, takenLocks.get(), locksToTake, accessor, emulationMode); // assign ANY locks here
            }
          }
        }
      }
    }
    return reason;
  }

  private void storeResourcesAffinity(@NotNull final BuildPromotionEx promotion,
                                      @NotNull final String projectId,
                                      @NotNull final Map<Resource, TakenLock> takenLocks,
                                      @NotNull final Collection<Lock> locksToTake,
                                      @NotNull final DistributionDataAccessor accessor,
                                      final boolean emulationMode) {
    if (emulationMode) return;

    final Map<String, Resource> resources = myResources.getResourcesMap(projectId);
    final Map<String, String> affinityMap = new HashMap<>();
    for (Lock lock: locksToTake) {
      if (lock.getType() != LockType.READ) continue;

      Resource r = resources.get(lock.getName());
      if (r instanceof CustomResource) {
        if (lock.isAnyValueLock()) {
          // if lock is ANY lock -> choose next available value
          final String nextVal = getNextAvailableValue((CustomResource)r, takenLocks, promotion, accessor);
          if (nextVal == null) {
            LOG.warn("Could not find a free shared resource value for promotion: " + promotion + ", resource: " + r);
          } else {
            affinityMap.put(r.getId(), nextVal);
          }
        } else {
          // if lock is SPECIFIC lock - choose lock value
          affinityMap.put(r.getId(), lock.getValue());
        }
      }
    }

    if (!affinityMap.isEmpty()) {
      // store assigned values in affinity set to be used by other builds inside current distribution cycle
      accessor.getReservedValuesProvider().rememberReservedValues(promotion, affinityMap);

      // store assigned value from resource affinity inside build promotion
      affinityMap.forEach((resourceId, value) -> promotion.setAttribute(getReservedResourceAttributeKey(resourceId), value));
    }
  }

  @Nullable
  private String getNextAvailableValue(@NotNull final CustomResource resource,
                                       @NotNull final Map<Resource, TakenLock> takenLocks,
                                       @NotNull final BuildPromotion promotion,
                                       @NotNull final DistributionDataAccessor accessor) {
    final List<String> values = new ArrayList<>(resource.getValues());
    // remove all values reserved by other builds in current distribution cycle
    accessor.getReservedValuesProvider().getValuesReservedByOtherBuilds(resource, promotion).forEach(values::remove);
    // remove values from taken locks
    final TakenLock takenLock = takenLocks.get(resource);
    if (takenLock != null) {
      takenLock.getReadLocks().values().forEach(values::remove);
    }
    return values.isEmpty() ? null : values.iterator().next();
  }

  /**
   * Resolves lock names into resources for given node of the build chain
   *
   * @param chainLocks    locks already obtained by the build chain
   * @param nodeResources actual resources for the project of current node
   * @param promo         build promotion of the current node
   * @param nodeLocks     locks requested by the current node in the build chain
   */
  private void resolve(@NotNull final Map<Resource, Map<BuildPromotionEx, Lock>> chainLocks,
                       @NotNull final Map<String, Resource> nodeResources,
                       @NotNull final BuildPromotionEx promo,
                       @NotNull final Map<String, Lock> nodeLocks) {
    nodeLocks.forEach((name, lock) -> {
      Resource resource = nodeResources.get(name);
      if (resource == null) {
        // todo: handle. this should not happen as configuration inspector should prevent this
        throw new RuntimeException("Invalid configuration!");
      }
      // here list instead of set as we need to know how much quota we should ignore
      chainLocks.computeIfAbsent(resource, k -> new HashMap<>()).put(promo, lock);
    });
  }

  @NotNull
  private WaitReason createWaitReason(@NotNull final Map<Resource, String> unavailableLocks) {
    final String description = unavailableLocks.entrySet().stream().map(e -> e.getKey().getName() + " " + e.getValue()).collect(Collectors.joining(", "));
    final String reasonDescription = "Build is waiting for the following "
                                     + StringUtil.pluralize("resource", unavailableLocks.size())
                                     + " to become available: "
                                     + description;
    return new SimpleWaitReason(reasonDescription);
  }

  @Nullable
  @SuppressWarnings("StringBufferReplaceableByString")
  private WaitReason checkForInvalidLocks(@NotNull final SBuildType buildType) {
    WaitReason result = null;
    final Map<Lock, String> invalidLocks = myInspector.inspect(buildType);
    if (!invalidLocks.isEmpty()) {
      final StringBuilder builder = new StringBuilder("Build configuration ");
      builder.append(buildType.getExtendedName()).append(" has shared resources configuration error");
      builder.append(invalidLocks.size() > 1 ? "s: " : ": ");
      builder.append(StringUtil.join(invalidLocks.values(), " "));
      result = new SimpleWaitReason(builder.toString());
    }
    return result;
  }
}