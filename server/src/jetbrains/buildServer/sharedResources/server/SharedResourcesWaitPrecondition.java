package jetbrains.buildServer.sharedResources.server;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.BuildAgent;
import jetbrains.buildServer.serverSide.buildDistribution.*;
import jetbrains.buildServer.sharedResources.model.Lock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

import static jetbrains.buildServer.sharedResources.server.SharedResourcesUtils.extractLocksFromPromotion;
import static jetbrains.buildServer.sharedResources.server.SharedResourcesUtils.getBuildPromotions;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class SharedResourcesWaitPrecondition implements StartBuildPrecondition {

  private static final Logger LOG = Logger.getInstance(SharedResourcesWaitPrecondition.class.getName());

  @Nullable
  @Override
  public WaitReason canStart(@NotNull QueuedBuildInfo queuedBuild, @NotNull Map<QueuedBuildInfo, BuildAgent> canBeStarted, @NotNull BuildDistributorInput buildDistributorInput, boolean emulationMode) {
    WaitReason result = null;
    if (!emulationMode) {
      final Collection<Lock> locksToTake = extractLocksFromPromotion(queuedBuild.getBuildPromotionInfo());
      if (!locksToTake.isEmpty()) {
        final Collection<RunningBuildInfo> runningBuilds = buildDistributorInput.getRunningBuilds();
        final Collection<QueuedBuildInfo> distributedBuilds = canBeStarted.keySet();
        final Collection<BuildPromotionInfo> buildPromotions = getBuildPromotions(runningBuilds, distributedBuilds);
        final Collection<Lock> unavailableLocks = SharedResourcesUtils.getUnavailableLocks(locksToTake, buildPromotions);
        if (!unavailableLocks.isEmpty()) {
          final StringBuilder builder = new StringBuilder("Build is waiting for lock(s): \n");
          for (Lock lock : unavailableLocks) {
            builder.append(lock.getName()).append(", ");
          }
          final String reasonDescription = builder.substring(0, builder.length() - 2);
          if (LOG.isDebugEnabled()) {
            LOG.debug("Got wait reason: [" + reasonDescription + "]");
          }
          result = new SimpleWaitReason(reasonDescription);
        }
      }
    }
    return result;
  }


}
