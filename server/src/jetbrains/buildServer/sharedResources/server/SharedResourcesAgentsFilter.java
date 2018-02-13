package jetbrains.buildServer.sharedResources.server;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import java.util.*;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.buildDistribution.*;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.TakenLock;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.server.feature.Locks;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeature;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
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

  public SharedResourcesAgentsFilter(@NotNull final SharedResourcesFeatures features,
                                     @NotNull final Locks locks,
                                     @NotNull final TakenLocks takenLocks,
                                     @NotNull final RunningBuildsManager runningBuildsManager,
                                     @NotNull final ConfigurationInspector inspector) {
    myFeatures = features;
    myLocks = locks;
    myTakenLocks = takenLocks;
    myRunningBuildsManager = runningBuildsManager;
    myInspector = inspector;
  }

  @NotNull
  @Override
  public AgentsFilterResult filterAgents(@NotNull final AgentsFilterContext context) {
    // get custom data
    final Set<String> featureContext = getOrCreateFeatureData(context);
    // get or create our collection of resources
    WaitReason reason = null;
    QueuedBuildInfo queuedBuild = context.getStartingBuild();
    final Map<QueuedBuildInfo, SBuildAgent> canBeStarted = context.getDistributedBuilds();
    final BuildPromotionEx myPromotion = (BuildPromotionEx) queuedBuild.getBuildPromotionInfo();
    if (myPromotion.isPartOfBuildChain()) {
      LOG.info("Queued build is part of build chain");
      BuildPromotionEx top = myPromotion.getTopDependencyGraph().getTop();
      if (top.isCompositeBuild()) {
        // top is composite build and top is not us
        // check if top is running
        SBuild topBuild = top.getAssociatedBuild();
        if (topBuild != null && topBuild instanceof SRunningBuild) {
          LOG.info("Top composite build is running, need to resolve only our locks");
        }

        SQueuedBuild queuedTopBuild = top.getQueuedBuild();
        if (queuedTopBuild != null) {
          reason = getWaitReason(top, Collections.emptySet(), canBeStarted);
          LOG.info("Composite build is queued. Need to make sure it can be started");
        }
      }
    }
    if (reason == null) {
      reason = getWaitReason(myPromotion, featureContext, canBeStarted);
    }
    final AgentsFilterResult result = new AgentsFilterResult();
    result.setWaitReason(reason);
    return result;
  }

  private WaitReason getWaitReason(final BuildPromotionEx buildPromotion,
                                   final Set<String> featureContext,
                                   final Map<QueuedBuildInfo, SBuildAgent> canBeStarted) {
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
            // Resolved locks as multi-valued taken locks (for custom - multiple custom values, for quoted - number of quotes to take)
            final Map<Resource, TakenLock> takenLocks = myTakenLocks.collectTakenLocks(projectId, myRunningBuildsManager.getRunningBuilds(), canBeStarted.keySet());
            // Collection<Lock> --> Collection<ResolvedLock>. For quoted - number of insufficient quotes, for custom -> custom values
            final Map<Resource, Lock> unavailableLocks = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, projectId, featureContext);
            if (!unavailableLocks.isEmpty()) {
              reason = createWaitReason(takenLocks, unavailableLocks);
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
        descr.append(StringUtil.join(buildTypeNames, ", "));
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
