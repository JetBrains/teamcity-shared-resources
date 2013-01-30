/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import jetbrains.buildServer.sharedResources.server.feature.SharedResourceFeatures;
import jetbrains.buildServer.sharedResources.settings.PluginProjectSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
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

  @NotNull
  private final SharedResourceFeatures myFeatures;

  public SharedResourcesWaitPrecondition(@NotNull final ProjectSettingsManager projectSettingsManager,
                                         @NotNull final SharedResourceFeatures features) {
    myProjectSettingsManager = projectSettingsManager;
    myFeatures = features;
  }

  @Nullable
  @Override
  public WaitReason canStart(@NotNull final QueuedBuildInfo queuedBuild,
                             @NotNull final Map<QueuedBuildInfo, BuildAgent> canBeStarted,
                             @NotNull final BuildDistributorInput buildDistributorInput, boolean emulationMode) {
    WaitReason result = null;
    final BuildPromotionEx myPromotion = (BuildPromotionEx) queuedBuild.getBuildPromotionInfo();
    final String projectId = myPromotion.getProjectId();
    final SBuildType buildType = myPromotion.getBuildType();
    if (buildType != null && projectId != null) {
      if (!myFeatures.searchForFeatures(buildType).isEmpty()) {
        final ParametersProvider pp = myPromotion.getParametersProvider();
        final Collection<Lock> locksToTake = extractLocksFromParams(pp.getAll());
        if (!locksToTake.isEmpty()) {
          // now deal only with builds that have same projectId as the current one
          final Collection<RunningBuildInfo> runningBuilds = buildDistributorInput.getRunningBuilds();
          final Collection<QueuedBuildInfo> distributedBuilds = canBeStarted.keySet();
          final Collection<BuildPromotionInfo> buildPromotions = getBuildPromotions(runningBuilds, distributedBuilds);
          // filter promotions by project id of current build
          filterPromotions(projectId, buildPromotions);
          final PluginProjectSettings settings = (PluginProjectSettings) myProjectSettingsManager.getSettings(projectId, SERVICE_NAME);
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

  // todo: make private, make tests for it
  void filterPromotions(String projectId, Collection<BuildPromotionInfo> buildPromotions) {
    final Iterator<BuildPromotionInfo> promotionInfoIterator = buildPromotions.iterator();
    while (promotionInfoIterator.hasNext()) {
      final BuildPromotionEx bpEx = (BuildPromotionEx)promotionInfoIterator.next();
      if (!projectId.equals(bpEx.getProjectId())) {
        promotionInfoIterator.remove();
      }
    }
  }

}
