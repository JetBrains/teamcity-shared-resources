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

package jetbrains.buildServer.sharedResources.server.runtime;

import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.buildDistribution.QueuedBuildInfo;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.TakenLock;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

/**
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public interface TakenLocks {


  /**
   * For given project collects taken locks using both artifacts and build promotions.
   *
   * For running builds :
   *    looking first into artifact
   *    secondly, if no artifact exists, looking into promotion
   *
   * For queued builds looking only in promotion
   *
   *
   * @param projectId id of the project to filter by
   * @param runningBuilds running builds
   * @param queuedBuilds queued builds
   * @return map of taken locks in format {@code <lockName, TakenLock>}
   */
  @NotNull
  public Map<String, TakenLock> collectTakenLocks(@NotNull final String projectId,
                                                  @NotNull final Collection<SRunningBuild> runningBuilds,
                                                  @NotNull final Collection<QueuedBuildInfo> queuedBuilds);

  /**
   * Decides, whether required locks can be acquired by the build
   * @param locksToTake required locks
   * @param takenLocks taken locks
   * @return empty collection, if locks can be acquired, collection, that contains unavailable locks otherwise
   */
  @NotNull
  public Collection<Lock> getUnavailableLocks(@NotNull final Collection<Lock> locksToTake,
                                              @NotNull final Map<String, TakenLock> takenLocks,
                                              @NotNull final String projectId);

}
