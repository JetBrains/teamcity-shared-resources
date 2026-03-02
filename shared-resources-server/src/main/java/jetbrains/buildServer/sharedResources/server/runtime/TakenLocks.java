

package jetbrains.buildServer.sharedResources.server.runtime;

import java.util.Collection;
import java.util.Map;
import jetbrains.buildServer.serverSide.BuildPromotion;
import jetbrains.buildServer.serverSide.BuildPromotionEx;
import jetbrains.buildServer.serverSide.RunningBuildEx;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.buildDistribution.QueuedBuildInfo;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.TakenLock;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import org.jetbrains.annotations.NotNull;

/**
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public interface TakenLocks {

  /**
   * Returns all currently taken locks as well as locks which will be taken by the starting running builds or by the queued builds which are scheduled to start.
   * @param startingQueuedBuilds  queued builds which were scheduled to start
   * @return map of taken locks in format {@code <Resource, TakenLock>}
   */
  @NotNull
  Map<Resource, TakenLock> collectTakenLocks(@NotNull Collection<RunningBuildEx> runningBuilds,
                                             @NotNull final Collection<QueuedBuildInfo> startingQueuedBuilds);

  Map<Resource, String> getUnavailableLocks(@NotNull final Collection<Lock> locksToTake,
                                            @NotNull final Map<Resource, TakenLock> takenLocks,
                                            @NotNull final String projectId,
                                            @NotNull final DistributionDataAccessor distributionDataAccessor,
                                            @NotNull final BuildPromotion promotion);

  Map<Resource, String> getUnavailableLocks(@NotNull final Map<String, Lock> locksToTake,
                                            @NotNull final Map<Resource, TakenLock> takenLocks,
                                            @NotNull final DistributionDataAccessor distributionDataAccessor,
                                            @NotNull final Map<String, Resource> chainNodeResources,
                                            @NotNull final Map<Resource, Map<BuildPromotionEx, Lock>> chainLocks,
                                            @NotNull final BuildPromotion promotion);
}