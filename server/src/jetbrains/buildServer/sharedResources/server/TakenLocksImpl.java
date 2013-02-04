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

import jetbrains.buildServer.serverSide.BuildPromotionEx;
import jetbrains.buildServer.serverSide.buildDistribution.BuildPromotionInfo;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.TakenLock;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.server.feature.Locks;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class TakenLocksImpl implements TakenLocks {

  @NotNull
  private final Locks myLocks;

  @NotNull
  private final Resources myResources;

  public TakenLocksImpl(@NotNull final Locks locks,
                        @NotNull final Resources resources) {
    myLocks = locks;
    myResources = resources;
  }

  @NotNull
  @Override
  public Map<String, TakenLock> collectTakenLocks(@NotNull final Collection<BuildPromotionInfo> buildPromotions) {
    final Map<String, TakenLock> result = new HashMap<String, TakenLock>();
    for (BuildPromotionInfo promo: buildPromotions) {
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
  @Override
  public Collection<Lock> getUnavailableLocks(@NotNull Collection<Lock> locksToTake,
                                              @NotNull Map<String, TakenLock> takenLocks,
                                              @NotNull String projectId) {
    final Map<String, Resource> resources = myResources.getAllResources(projectId);
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
