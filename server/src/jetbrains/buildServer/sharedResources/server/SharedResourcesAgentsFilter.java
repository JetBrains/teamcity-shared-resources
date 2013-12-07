package jetbrains.buildServer.sharedResources.server;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.buildDistribution.*;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.TakenLock;
import jetbrains.buildServer.sharedResources.server.feature.Locks;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeature;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.sharedResources.server.runtime.TakenLocks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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

  public SharedResourcesAgentsFilter(@NotNull final SharedResourcesFeatures features,
                                     @NotNull final Locks locks,
                                     @NotNull final TakenLocks takenLocks,
                                     @NotNull final RunningBuildsManager runningBuildsManager) {
    myFeatures = features;
    myLocks = locks;
    myTakenLocks = takenLocks;
    myRunningBuildsManager = runningBuildsManager;
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
    final String projectId = myPromotion.getProjectId();
    final SBuildType buildType = myPromotion.getBuildType();
    if (buildType != null && projectId != null) {
      final Collection<SharedResourcesFeature> features = myFeatures.searchForFeatures(buildType);
      if (!features.isEmpty()) {
        reason = checkForInvalidLocks(features, projectId, buildType);
        if (reason == null) {
          final Collection<Lock> locksToTake = myLocks.fromBuildFeaturesAsMap(features).values();
          if (!locksToTake.isEmpty()) {
            final Map<String, TakenLock> takenLocks = myTakenLocks.collectTakenLocks(projectId, myRunningBuildsManager.getRunningBuilds(), canBeStarted.keySet());
            if (!takenLocks.isEmpty()) {
              final Collection<Lock> unavailableLocks = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, projectId, featureContext);
              if (!unavailableLocks.isEmpty()) {
                reason = createWaitReason(takenLocks, unavailableLocks);
                if (LOG.isDebugEnabled()) {
                  LOG.debug("Firing precondition for queued build [" + queuedBuild + "] with reason: [" + reason.getDescription() + "]");
                }
              }
            }
          }
        }
      }
    }
    final AgentsFilterResult result = new AgentsFilterResult();
    if (reason != null) {
      result.setWaitReason(reason);
      result.setFilteredConnectedAgents(Collections.<SBuildAgent>emptyList());
    } else {
      result.setFilteredConnectedAgents(null);
    }
    return result;
  }

  @NotNull
  private WaitReason createWaitReason(@NotNull final Map<String, TakenLock> takenLocks,
                                      @NotNull final Collection<Lock> unavailableLocks) {
    final StringBuilder builder = new StringBuilder("Build is waiting for the following ");
    builder.append(unavailableLocks.size() > 1 ? "resources " : "resource ");
    builder.append("to become available: ");
    for (Lock lock : unavailableLocks) {
      final Set<String> buildTypeNames = new HashSet<String>();
      for (BuildPromotionInfo promotion: takenLocks.get(lock.getName()).getReadLocks().keySet()) {
        BuildTypeEx bt = ((BuildPromotionEx) promotion).getBuildType();
        if (bt != null) {
          buildTypeNames.add(bt.getName());
        }
      }
      for (BuildPromotionInfo promotion: takenLocks.get(lock.getName()).getWriteLocks().keySet()) {
        BuildTypeEx bt = ((BuildPromotionEx) promotion).getBuildType();
        if (bt != null) {
          buildTypeNames.add(bt.getName());
        }
      }
      if (!buildTypeNames.isEmpty()) {
        builder.append(lock.getName()).append(" (locked by ");
        builder.append(StringUtil.join(buildTypeNames, ","));
        builder.append(")");
      }
      builder.append(", ");
    }
    final String reasonDescription = builder.substring(0, builder.length() - 2);
    return new SimpleWaitReason(reasonDescription);
  }

  @Nullable
  @SuppressWarnings("StringBufferReplaceableByString")
  private WaitReason checkForInvalidLocks(@NotNull final Collection<SharedResourcesFeature> features,
                                          @NotNull final String projectId,
                                          @NotNull final SBuildType buildType) {
    WaitReason result = null;
    final Map<Lock, String> invalidLocks = new HashMap<Lock, String>();
    for (SharedResourcesFeature feature : features) {
      invalidLocks.putAll(feature.getInvalidLocks(projectId));
    }
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
