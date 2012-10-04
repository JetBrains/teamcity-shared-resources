package jetbrains.buildServer.sharedResources.server;

import jetbrains.buildServer.BuildAgent;
import jetbrains.buildServer.serverSide.buildDistribution.*;
import jetbrains.buildServer.sharedResources.model.Lock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

import static jetbrains.buildServer.sharedResources.server.WaitPreconditionUtils.*;


/**
 *
 *
 * @author Oleg Rybak
 */
public class SharedResourcesWaitPrecondition implements StartBuildPrecondition {

  @Nullable
  public WaitReason canStart(@NotNull QueuedBuildInfo queuedBuild, @NotNull Map<QueuedBuildInfo, BuildAgent> canBeStarted, @NotNull BuildDistributorInput buildDistributorInput, boolean emulationMode) {
    if (emulationMode) return null;
    Collection<Lock> locksToTake = extractLocksFromPromotion(queuedBuild.getBuildPromotionInfo());
    Collection<BuildPromotionInfo> buildPromotions = getBuildPromotions(buildDistributorInput.getRunningBuilds(), canBeStarted.keySet());
    SimpleWaitReason result = null;
    boolean canLock = locksAvailable(locksToTake, buildPromotions);
    if (!canLock) {
      result = new SimpleWaitReason("Build is waiting for locks");
    }
    return result;
  }
}
