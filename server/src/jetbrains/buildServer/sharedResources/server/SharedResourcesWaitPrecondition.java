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
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.TakenLock;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.server.feature.Locks;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
  private final Resources myResources;

  public SharedResourcesWaitPrecondition(@NotNull final SharedResourcesFeatures features,
                                         @NotNull final Locks locks,
                                         @NotNull final Resources resources) {
    myFeatures = features;
    myLocks = locks;
    myResources = resources;
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
      if (myFeatures.featuresPresent(buildType)) {
        final ParametersProvider pp = myPromotion.getParametersProvider();
        final Collection<Lock> locksToTake  = myLocks.fromBuildParameters(pp.getAll());

        if (!locksToTake.isEmpty()) {
          // now deal only with builds that have same projectId as the current one
          final Collection<BuildPromotionInfo> buildPromotions = getBuildPromotions(
                  buildDistributorInput.getRunningBuilds(), canBeStarted.keySet(), projectId);
          if (!buildPromotions.isEmpty()) {
            final Map<String, TakenLock> takenLocks = collectTakenLocks(buildPromotions);
            if (!takenLocks.isEmpty()) {
              final Map<String, Resource> resources = myResources.getAllResources(projectId);
              final Collection<Lock> unavailableLocks = getUnavailableLocks(locksToTake, takenLocks, resources);
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
      }
    }
    return result;
  }

  // the idea is that precondition has to deal with decision itself. Now what will we do, when we introduce custom values?


  /**
   * Extracts build promotions from build server state, represented by running and queued builds. Only build promotions
   * with the same id as provided are returned
   *
   * @param runningBuilds running builds
   * @param queuedBuilds  queued builds
   * @param projectId id of the current project
   * @return collection of build promotions, that correspond to given builds
   */
  @NotNull
  private Collection<BuildPromotionInfo> getBuildPromotions(@NotNull final Collection<RunningBuildInfo> runningBuilds,
                                                            @NotNull final Collection<QueuedBuildInfo> queuedBuilds,
                                                            @NotNull final String projectId) {
    final ArrayList<BuildPromotionInfo> result = new ArrayList<BuildPromotionInfo>();
    for (RunningBuildInfo runningBuildInfo : runningBuilds) {
      BuildPromotionEx buildPromotionEx = ((BuildPromotionEx)runningBuildInfo.getBuildPromotionInfo());
      if (projectId.equals(buildPromotionEx.getProjectId())) {
        result.add(runningBuildInfo.getBuildPromotionInfo());
      }
    }
    for (QueuedBuildInfo queuedBuildInfo : queuedBuilds) {
      BuildPromotionEx buildPromotionEx = (BuildPromotionEx)queuedBuildInfo.getBuildPromotionInfo();
      if (projectId.equals(buildPromotionEx.getProjectId())) {
        result.add(queuedBuildInfo.getBuildPromotionInfo());
      }
    }
    return result;
  }

  private Map<String, TakenLock> collectTakenLocks(@NotNull final Collection<BuildPromotionInfo> promotions) {
    final Map<String, TakenLock> result = new HashMap<String, TakenLock>();
    for (BuildPromotionInfo promo: promotions) {
      Collection<Lock> locks = myLocks.fromBuildParameters(((BuildPromotionEx)promo).getParametersProvider().getAll());
      for (Lock lock: locks) {
        TakenLock takenLock = result.get(lock.getName());
        if (takenLock == null) {
          takenLock = new TakenLock();
          result.put(lock.getName(), takenLock);
        }
        takenLock.addLock(promo, lock);
      }
    }
    return result;
  }

  @NotNull
  private Collection<Lock> getUnavailableLocks(@NotNull Collection<Lock> locksToTake,
                                               @NotNull Map<String, TakenLock> takenLocks,
                                               @NotNull Map<String, Resource> resources) {
    final Collection<Lock> result = new ArrayList<Lock>();
    for (Lock lock : locksToTake) {
      final TakenLock takenLock = takenLocks.get(lock.getName());
      if (takenLock != null) {
        switch (lock.getType())  {
          case READ:
            // 1) Check that no write lock exists
            if (takenLock.hasWriteLocks()) {
              result.add(lock);
            }
            // check against resource
            final Resource resource = resources.get(lock.getName());
            if (resource != null && !resource.isInfinite()) {
              // limited capacity resource
              if (takenLock.getReadLocks().size() >= resource.getQuota()) {
                result.add(lock);
              }
            }
            break;
          case WRITE:
            if (takenLock.hasReadLocks() || takenLock.hasWriteLocks()) { // if anyone is accessing the resource
              result.add(lock);
            }
        }
      }
    }
    return result;
  }




}

