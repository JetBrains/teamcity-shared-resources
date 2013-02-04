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

package jetbrains.buildServer.sharedResources.server.feature;

import jetbrains.buildServer.serverSide.buildDistribution.BuildPromotionInfo;
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
   * Collects locks that are already taken
   * @param buildPromotions build promotions (either running builds or builds that are distributed)
   * @return map of taken locks in format {@code <lockName, TakenLock>}
   */
  @NotNull
  public Map<String, TakenLock> collectTakenLocks(@NotNull final Collection<BuildPromotionInfo> buildPromotions);

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
