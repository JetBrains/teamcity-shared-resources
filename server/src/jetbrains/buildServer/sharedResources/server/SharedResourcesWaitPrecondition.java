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
import jetbrains.buildServer.serverSide.BuildPromotionEx;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.buildDistribution.*;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.TakenLock;
import jetbrains.buildServer.sharedResources.server.feature.Locks;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeature;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.sharedResources.server.runtime.TakenLocks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class SharedResourcesWaitPrecondition implements StartBuildPrecondition {

  private static final Logger LOG = Logger.getInstance(SharedResourcesWaitPrecondition.class.getName());

  @NotNull
  private final SharedResourcesFeatures myFeatures;

  @NotNull
  private final Locks myLocks;

  @NotNull
  private final TakenLocks myTakenLocks;

  public SharedResourcesWaitPrecondition(@NotNull final SharedResourcesFeatures features,
                                         @NotNull final Locks locks,
                                         @NotNull final TakenLocks takenLocks) {
    myFeatures = features;
    myLocks = locks;
    myTakenLocks = takenLocks;
  }

  @Nullable
  @Override
  public WaitReason canStart(@NotNull final QueuedBuildInfo queuedBuild,
                             @NotNull final Map<QueuedBuildInfo, BuildAgent> canBeStarted,
                             @NotNull final BuildDistributorInput buildDistributorInput, boolean emulationMode) {
    if (emulationMode) return null; // not supporting snapshot running builds passed by estimator
    WaitReason result = null;
    final BuildPromotionEx myPromotion = (BuildPromotionEx) queuedBuild.getBuildPromotionInfo();
    final String projectId = myPromotion.getProjectId();
    final SBuildType buildType = myPromotion.getBuildType();
    if (buildType != null && projectId != null) {
      final Collection<SharedResourcesFeature> features = myFeatures.searchForFeatures(buildType);
      if (!features.isEmpty()) {
        result = checkForInvalidLocks(features, projectId, buildType);
        if (result == null) {
          final Collection<Lock> locksToTake = myLocks.fromBuildPromotion(myPromotion);
          if (!locksToTake.isEmpty()) {
            final Map<String, TakenLock> takenLocks = myTakenLocks.collectTakenLocks(projectId, buildDistributorInput.getRunningBuilds(), canBeStarted.keySet());
            if (!takenLocks.isEmpty()) {
              final Collection<Lock> unavailableLocks = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, projectId);
              if (!unavailableLocks.isEmpty()) {
                result = createWaitReason(unavailableLocks);
              }
            }
          }
        }
      }
    }
    return result;
  }

  @NotNull
  private WaitReason createWaitReason(@NotNull final Collection<Lock> unavailableLocks) {
    final StringBuilder builder = new StringBuilder("Build is waiting for ");
    builder.append(unavailableLocks.size() > 1 ? "locks: " : "lock: ");
    for (Lock lock : unavailableLocks) {
      builder.append(lock.getName()).append(", ");
    }
    final String reasonDescription = builder.substring(0, builder.length() - 2);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Got wait reason: [" + reasonDescription + "]");
    }
    return new SimpleWaitReason(reasonDescription);
  }

  @Nullable
  private WaitReason checkForInvalidLocks(@NotNull final Collection<SharedResourcesFeature> features,
                                          @NotNull final String projectId,
                                          @NotNull final SBuildType buildType) {
    WaitReason result = null;
    final Collection<Lock> invalidLocks = new ArrayList<Lock>();
    for (SharedResourcesFeature feature : features) {
      invalidLocks.addAll(feature.getInvalidLocks(projectId));
    }
    if (!invalidLocks.isEmpty()) {
      StringBuilder builder = new StringBuilder("Build type {");
      builder.append(buildType).append("} has invalid locks: ");
      for (Lock lock : invalidLocks) {
        builder.append(lock.getName()).append(", ");
      }
      result = new SimpleWaitReason(builder.substring(0, builder.length() - 2));
    }
    return result;
  }


}

