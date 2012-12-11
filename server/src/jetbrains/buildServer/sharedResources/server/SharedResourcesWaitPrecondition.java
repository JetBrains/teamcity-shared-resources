package jetbrains.buildServer.sharedResources.server;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.BuildAgent;
import jetbrains.buildServer.parameters.ParametersProvider;
import jetbrains.buildServer.serverSide.BuildPromotionEx;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.buildDistribution.*;
import jetbrains.buildServer.serverSide.settings.ProjectSettingsManager;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.settings.SharedResourcesProjectSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.SERVICE_NAME;
import static jetbrains.buildServer.sharedResources.server.SharedResourcesUtils.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class SharedResourcesWaitPrecondition implements StartBuildPrecondition {

  private static final Logger LOG = Logger.getInstance(SharedResourcesWaitPrecondition.class.getName());

  @NotNull
  private final ProjectSettingsManager myProjectSettingsManager;

  public SharedResourcesWaitPrecondition(@NotNull ProjectSettingsManager projectSettingsManager) {
    myProjectSettingsManager = projectSettingsManager;
  }

  @Nullable
  @Override
  public WaitReason canStart(@NotNull QueuedBuildInfo queuedBuild, @NotNull Map<QueuedBuildInfo, BuildAgent> canBeStarted, @NotNull BuildDistributorInput buildDistributorInput, boolean emulationMode) {
    WaitReason result = null;
    final BuildPromotionEx myPromotion = (BuildPromotionEx) queuedBuild.getBuildPromotionInfo();
    SBuildType buildType = myPromotion.getBuildType();
    if (buildType != null) {
      if (searchForFeature(buildType, false) != null) {
        final ParametersProvider pp = myPromotion.getParametersProvider();
        final Collection<Lock> locksToTake = extractLocksFromParams(pp.getAll());
        final String projectId = myPromotion.getProjectId();

        if (!locksToTake.isEmpty() && projectId != null) {
          // now deal only with builds that have same projectId as the current one

          final Collection<RunningBuildInfo> runningBuilds = buildDistributorInput.getRunningBuilds();
          final Collection<QueuedBuildInfo> distributedBuilds = canBeStarted.keySet();
          final Collection<BuildPromotionInfo> buildPromotions = getBuildPromotions(runningBuilds, distributedBuilds);
          // filter promotions by project id of current build
          filterPromotions(projectId, buildPromotions);
          final SharedResourcesProjectSettings settings = (SharedResourcesProjectSettings) myProjectSettingsManager.getSettings(projectId, SERVICE_NAME);
          final Map<String, Resource> resourceMap = settings.getResourceMap();
          final Collection<Lock> unavailableLocks = SharedResourcesUtils.getUnavailableLocks(locksToTake, buildPromotions, resourceMap);
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
