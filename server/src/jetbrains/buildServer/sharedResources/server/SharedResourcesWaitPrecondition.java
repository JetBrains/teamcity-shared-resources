package jetbrains.buildServer.sharedResources.server;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.BuildAgent;
import jetbrains.buildServer.parameters.ParametersProvider;
import jetbrains.buildServer.serverSide.BuildPromotionEx;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.buildDistribution.*;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.Lock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

import static jetbrains.buildServer.sharedResources.server.SharedResourcesUtils.extractLocksFromParams;
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
    final BuildPromotionEx myPromotion = (BuildPromotionEx) queuedBuild.getBuildPromotionInfo();
    SBuildType buildType = myPromotion.getBuildType();
    if (buildType != null) {
      boolean featureFound = false;
      final Collection<SBuildFeatureDescriptor> features = buildType.getBuildFeatures();
      for (SBuildFeatureDescriptor descriptor: features) {
        if (SharedResourcesPluginConstants.FEATURE_TYPE.equals(descriptor.getType())) {
          featureFound = true;
          break;
        }
      }
      if (featureFound) {
        final ParametersProvider pp = myPromotion.getParametersProvider();
        final Collection<Lock> locksToTake = extractLocksFromParams(pp.getAll());
        if (!locksToTake.isEmpty()) {
          final Collection<RunningBuildInfo> runningBuilds = buildDistributorInput.getRunningBuilds();
          final Collection<QueuedBuildInfo> distributedBuilds = canBeStarted.keySet();
          final Collection<BuildPromotionInfo> buildPromotions = getBuildPromotions(runningBuilds, distributedBuilds);
          final Collection<Lock> unavailableLocks = SharedResourcesUtils.getUnavailableLocks(locksToTake, buildPromotions);
          if (!unavailableLocks.isEmpty()) {
            final StringBuilder builder = new StringBuilder("Build is waiting for ");
            builder.append(unavailableLocks.size() > 1 ? "locks: " : "lock: ");
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
    }
    return result;
  }
}
