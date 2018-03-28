package jetbrains.buildServer.sharedResources.server;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.buildDistribution.*;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.TakenLock;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.server.feature.Locks;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeature;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.sharedResources.server.runtime.LocksStorage;
import jetbrains.buildServer.sharedResources.server.runtime.TakenLocks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class SharedResourcesAgentsFilter implements StartingBuildAgentsFilter {

  @NotNull
  private static final Logger LOG = Logger.getInstance(SharedResourcesAgentsFilter.class.getName());

  @NotNull
  static final String CUSTOM_DATA_KEY = SharedResourcesPluginConstants.PLUGIN_NAME;

  @NotNull
  private final SharedResourcesFeatures myFeatures;

  @NotNull
  private final Locks myLocks;

  @NotNull
  private final TakenLocks myTakenLocks;

  @NotNull
  private final RunningBuildsManager myRunningBuildsManager;

  @NotNull
  private final ConfigurationInspector myInspector;

  @NotNull
  private final LocksStorage myLocksStorage;

  @NotNull
  private final Resources myResources;

  public SharedResourcesAgentsFilter(@NotNull final SharedResourcesFeatures features,
                                     @NotNull final Locks locks,
                                     @NotNull final TakenLocks takenLocks,
                                     @NotNull final RunningBuildsManager runningBuildsManager,
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
    // get custom data
    final Set<String> featureContext = getOrCreateFeatureData(context);
    final AtomicReference<List<SRunningBuild>> runningBuilds = new AtomicReference<>();
    final AtomicReference<Map<Resource,TakenLock>> takenLocks = new AtomicReference<>();
    // get or create our collection of resources
    WaitReason reason = null;
    final QueuedBuildInfo queuedBuild = context.getStartingBuild();
    final Map<QueuedBuildInfo, SBuildAgent> canBeStarted = context.getDistributedBuilds();
    final BuildPromotionEx myPromotion = (BuildPromotionEx) queuedBuild.getBuildPromotionInfo();

    if (TeamCityProperties.getBooleanOrTrue(SharedResourcesPluginConstants.RESOURCES_IN_CHAINS_ENABLED) && myPromotion.isPartOfBuildChain()) {
      LOG.debug("Queued build is part of build chain");
      final List<BuildPromotionEx> depPromos = myPromotion.getDependentCompositePromotions();
      if (depPromos.isEmpty()) {
        LOG.debug("Queued build does not have dependent composite promotions");
        reason = processSingleBuild(myPromotion, featureContext, runningBuilds, canBeStarted, takenLocks);
      } else {
        LOG.debug("Queued build does have " + depPromos.size() + " dependent composite " + StringUtil.pluralize("promotion", depPromos.size()));
        // contains resources and locks that are INSIDE of the build chain
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
          final Collection<SharedResourcesFeature> features = myFeatures.searchForFeatures(compositeQueuedBuild.getBuildType());
          if (!features.isEmpty()) {
            final Map<String, Lock> locksToTake = myLocks.fromBuildFeaturesAsMap(features);
            if (!locksToTake.isEmpty()) {
              // resolve locks that build wants to take against actual resources
              chainResources.computeIfAbsent(compositeQueuedBuild.getBuildType().getProjectId(), myResources::getResourcesMap);
              reason = processBuildInChain(featureContext, runningBuilds, canBeStarted, takenLocks,
                                           chainResources.get(compositeQueuedBuild.getBuildType().getProjectId()), chainLocks,locksToTake);
              if (reason != null) {
                if (LOG.isDebugEnabled()) {
                  LOG.debug("Firing precondition for queued build [" + compositeQueuedBuild + "] with reason: [" + reason.getDescription() + "]");
                  LOG.debug("Found blocked composite build on the path: " + compositeQueuedBuild);
                }
                break;
              }
            }
          }
        }
        // process build itself
        if (reason == null) {
          if (myPromotion.getBuildType() != null) {
            final String projectId = myPromotion.getBuildType().getProjectId();
            chainResources.computeIfAbsent(projectId, myResources::getResourcesMap);
            final Collection<SharedResourcesFeature> features = myFeatures.searchForFeatures(myPromotion.getBuildType());
            if (!features.isEmpty()) {
              reason = checkForInvalidLocks(myPromotion.getBuildType());
            }
            final Map<String, Lock> locksToTake = myLocks.fromBuildFeaturesAsMap(features);
            if (!locksToTake.isEmpty()) {
              reason = processBuildInChain(featureContext, runningBuilds, canBeStarted, takenLocks, chainResources.get(projectId), chainLocks, locksToTake);
            }
          }
        }
      }
    } else {
      reason = processSingleBuild(myPromotion, featureContext, runningBuilds, canBeStarted, takenLocks);
    }
    final AgentsFilterResult result = new AgentsFilterResult();
    result.setWaitReason(reason);
    return result;
  }

  private WaitReason processBuildInChain(@NotNull final Set<String> featureContext,
                                         @NotNull final AtomicReference<List<SRunningBuild>> runningBuilds,
                                         @NotNull final Map<QueuedBuildInfo, SBuildAgent> canBeStarted,
                                         @NotNull final AtomicReference<Map<Resource, TakenLock>> takenLocks,
                                         @NotNull final Map<String, Resource> chainNodeResources,
                                         @NotNull final Map<Resource, Map<BuildPromotionEx, Lock>> chainLocks,
                                         @NotNull final Map<String, Lock> locksToTake) {
    WaitReason reason = null;

    if (runningBuilds.get() == null) {
      runningBuilds.set(myRunningBuildsManager.getRunningBuilds());
    }
    if (takenLocks.get() == null) {
      takenLocks.set(myTakenLocks.collectTakenLocks(runningBuilds.get(), canBeStarted.keySet()));
    }
    final Map<Resource, Lock> unavailableLocks = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks.get(), featureContext, chainNodeResources, chainLocks);
    if (!unavailableLocks.isEmpty()) {
      reason = createWaitReason(takenLocks.get(), unavailableLocks);
    }
    return reason;
  }

  private WaitReason processSingleBuild(@NotNull final BuildPromotionEx buildPromotion,
                                        @NotNull final Set<String> featureContext,
                                        @NotNull final AtomicReference<List<SRunningBuild>> runningBuilds,
                                        @NotNull final Map<QueuedBuildInfo, SBuildAgent> canBeStarted,
                                        @NotNull final AtomicReference<Map<Resource, TakenLock>> takenLocks) {
    final String projectId = buildPromotion.getProjectId();
    final SBuildType buildType = buildPromotion.getBuildType();
    WaitReason reason = null;
    if (buildType != null && projectId != null) {
      final Collection<SharedResourcesFeature> features = myFeatures.searchForFeatures(buildType);
      if (!features.isEmpty()) {
        reason = checkForInvalidLocks(buildType);
        if (reason == null) {
          // Collection<Lock> ---> Collection<ResolvedLock> (i.e. lock against resolved resource. With project and so on)
          final Collection<Lock> locksToTake = myLocks.fromBuildFeaturesAsMap(features).values();
          if (!locksToTake.isEmpty()) {
            if (runningBuilds.get() == null) {
              runningBuilds.set(myRunningBuildsManager.getRunningBuilds());
            }
            if (takenLocks.get() == null) {
              takenLocks.set(myTakenLocks.collectTakenLocks(runningBuilds.get(), canBeStarted.keySet()));
            }
            // Collection<Lock> --> Collection<ResolvedLock>. For quoted - number of insufficient quotes, for custom -> custom values
            final Map<Resource, Lock> unavailableLocks = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks.get(), projectId, featureContext);
            if (!unavailableLocks.isEmpty()) {
              reason = createWaitReason(takenLocks.get(), unavailableLocks);
              if (LOG.isDebugEnabled()) {
                LOG.debug("Firing precondition for queued build [" + buildPromotion.getQueuedBuild() + "] with reason: [" + reason.getDescription() + "]");
              }
            }
          }
        }
      }
    }
    return reason;
  }

  /**
   * Resolves lock names into resources for given node of the build chain
   *
   * @param chainLocks locks already obtained by the build chain
   * @param nodeResources actual resources for the project of current node
   * @param promo build promotion of the current node
   * @param nodeLocks locks requested by the current node in the build chain
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
  private WaitReason createWaitReason(@NotNull final Map<Resource, TakenLock> takenLocks,
                                      @NotNull final Map<Resource, Lock> unavailableLocks) {
    final StringBuilder builder = new StringBuilder("Build is waiting for the following ");
    builder.append(unavailableLocks.size() > 1 ? "resources " : "resource ");
    builder.append("to become available: ");
    final Set<String> lockDescriptions = new HashSet<>();
    for (Map.Entry<Resource, Lock> entry : unavailableLocks.entrySet()) {
      final StringBuilder descr = new StringBuilder();
      final Set<String> buildTypeNames = new HashSet<>();
      final TakenLock takenLock = takenLocks.get(entry.getKey());
      if (takenLock != null) {
        for (BuildPromotionInfo promotion : takenLock.getReadLocks().keySet()) {
          BuildTypeEx bt = ((BuildPromotionEx) promotion).getBuildType();
          if (bt != null) {
            buildTypeNames.add(bt.getExtendedFullName());
          }
        }
        for (BuildPromotionInfo promotion : takenLock.getWriteLocks().keySet()) {
          BuildTypeEx bt = ((BuildPromotionEx) promotion).getBuildType();
          if (bt != null) {
            buildTypeNames.add(bt.getExtendedFullName());
          }
        }
      }
      descr.append(entry.getValue().getName());
      if (!buildTypeNames.isEmpty()) {
        descr.append(" (locked by ");
        descr.append(buildTypeNames.stream().sorted().collect(Collectors.joining(", ")));
        descr.append(")");
      }
      lockDescriptions.add(descr.toString());
    }
    builder.append(StringUtil.join(lockDescriptions, ", "));
    final String reasonDescription = builder.toString();
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

  @SuppressWarnings("unchecked")
  @NotNull
  private Set<String> getOrCreateFeatureData(@NotNull final AgentsFilterContext context) {
    Object o = context.getCustomData(CUSTOM_DATA_KEY);
    if (o == null) {
      o = new HashSet<String>();
      context.setCustomData(CUSTOM_DATA_KEY, o);
    }
    return (Set<String>)o;
  }
}
