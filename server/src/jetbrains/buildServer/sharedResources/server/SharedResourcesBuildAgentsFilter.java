package jetbrains.buildServer.sharedResources.server;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.buildDistribution.*;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.sharedResources.util.FeatureUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Class {SharedResourcesBuildAgentsFilter}
 *
 * Implements filtering of agents based on locks
 *
 * @author Oleg Rybak
 */
public class SharedResourcesBuildAgentsFilter implements StartingBuildAgentsFilter {

  private static final Logger LOG = Logger.getInstance(SharedResourcesBuildAgentsFilter.class.getName());

  private final static WaitReason WAIT_FOR_LOCK = new SimpleWaitReason("Build is waiting for lock");

  private final RunningBuildsManager myRunningBuildsManager;

  public SharedResourcesBuildAgentsFilter(RunningBuildsManager runningBuildsManager) {
    myRunningBuildsManager = runningBuildsManager;
  }

  @NotNull
  public AgentsFilterResult filterAgents(@NotNull AgentsFilterContext context) {
    LOG.debug("(SharedResourcesBuildAgentsFilter) start");

    final AgentsFilterResult result = new AgentsFilterResult();
    final QueuedBuildInfo queuedBuild = context.getStartingBuild();
    final BuildPromotionEx promo = (BuildPromotionEx)queuedBuild.getBuildPromotionInfo();
    final BuildTypeEx mybuild = promo.getBuildType();

    if (mybuild != null) {
      // get my locks into map
      final Map<LockType, Set<Lock>> myLocks = FeatureUtil.getLocksMap(FeatureUtil.extractLocks(mybuild));
      if (!mapEmpty(myLocks))  {
        // if we have some locks, check them against other running builds
        boolean locksAvailable = true;

        // for each running build
        final List<SRunningBuild> runningBuilds = myRunningBuildsManager.getRunningBuilds();
        for (SRunningBuild build: runningBuilds) {
          SBuildType type = build.getBuildType();
          if (type != null && locksAvailable) {
            final Map<LockType, Set<Lock>> otherLocks = FeatureUtil.getLocksMap(FeatureUtil.extractLocks(type));
            //    check simple vs simple
            if (!mapEmpty(otherLocks)) {
              Set<Lock> locks = myLocks.get(LockType.SIMPLE);
              if (!locks.isEmpty()) {
                if (FeatureUtil.lockSetsCrossing(locks, otherLocks.get(LockType.SIMPLE))) {
                  locksAvailable = false;
                }
              }
              //    check read vs write
              if (locksAvailable) {
                locks = myLocks.get(LockType.READ);
                if (!locks.isEmpty()) {
                  if (FeatureUtil.lockSetsCrossing(locks, otherLocks.get(LockType.WRITE))) {
                    locksAvailable = false;
                  }
                }
              }

              //    check read vs write
              if (locksAvailable) {
                locks = myLocks.get(LockType.WRITE);
                if (!locks.isEmpty()) {
                  if (FeatureUtil.lockSetsCrossing(locks, otherLocks.get(LockType.WRITE)) ||
                          FeatureUtil.lockSetsCrossing(locks, otherLocks.get(LockType.READ))) {
                    locksAvailable = false;
                  }
                }
              }
            }
          }
        }

        if (!locksAvailable) {
          result.setFilteredConnectedAgents(Collections.<SBuildAgent>emptyList());
          result.setWaitReason(WAIT_FOR_LOCK);
          LOG.debug("(SharedResourcesBuildAgentsFilter) putting build on wait");
        }
      }
    }


    LOG.debug("(SharedResourcesBuildAgentsFilter) finish");
    return result;
  }

  private static <K, V>boolean mapEmpty(Map<K, Set<V>> map) {
    boolean result = true;
    for (Set<V> val: map.values()) {
      if (!val.isEmpty()) {
        result = false;
        break;
      }
    }
    return result;
  }
}
