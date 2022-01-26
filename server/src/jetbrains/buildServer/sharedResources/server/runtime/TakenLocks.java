/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

package jetbrains.buildServer.sharedResources.server.runtime;

import java.util.Collection;
import java.util.Map;
import jetbrains.buildServer.serverSide.BuildPromotion;
import jetbrains.buildServer.serverSide.BuildPromotionEx;
import jetbrains.buildServer.serverSide.RunningBuildEx;
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
   * For given project collects taken locks using both artifacts and build promotions.
   * <p>
   * For running builds :
   * looking first into artifact
   * secondly, if no artifact exists, looking into promotion+buildType
   * <p>
   * For queued builds looking only in promotion+buildType
   *
   * @param runningBuilds running builds
   * @param queuedBuilds  queued builds
   * @return map of taken locks in format {@code <Resource, TakenLock>}
   */
  @NotNull
  Map<Resource, TakenLock> collectTakenLocks(@NotNull final Collection<RunningBuildEx> runningBuilds,
                                             @NotNull final Collection<QueuedBuildInfo> queuedBuilds);

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
