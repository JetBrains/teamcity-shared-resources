package jetbrains.buildServer.sharedResources.server;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
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

import java.util.*;
import java.util.stream.Collectors;

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
    final String projectId = myPromotion.getProjectId();
    final SBuildType buildType = myPromotion.getBuildType();
    if (buildType != null && projectId != null) {
      final Collection<SharedResourcesFeature> features = myFeatures.searchForFeatures(buildType);
      if (!features.isEmpty()) {
        reason = checkForInvalidResources(buildType.getProject());
        if (reason == null) {
          reason = checkForInvalidLocks(buildType);
        }
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
                LOG.debug("Firing precondition for queued build [" + queuedBuild + "] with reason: [" + reason.getDescription() + "]");
              }
            }
          }
        }
      }
    }
    final AgentsFilterResult result = new AgentsFilterResult();
    result.setWaitReason(reason);
    return result;
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

  @Nullable
  private WaitReason checkForInvalidResources(@NotNull final SProject project) {
    WaitReason result = null;
    final Map<String, List<String>> duplicates = myInspector.checkDuplicateResources(project);
    if (!duplicates.isEmpty()) {
      StringBuilder builder = new StringBuilder("Shared resources configuration is not valid. ");
      duplicates.entrySet().forEach(
              entry -> {
                builder.append("Project ").append(entry.getKey()).append(" has invalid resource");
                builder.append(entry.getValue().size() > 1 ? "s: " : ": ");
                builder.append(entry.getValue().stream().collect(Collectors.joining(", ")));
              }
      );
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
