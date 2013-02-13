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

package jetbrains.buildServer.sharedResources.model;

import jetbrains.buildServer.serverSide.buildDistribution.BuildPromotionInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Class {@code TakenLock}.
 *
 * For each resource, instance of this class contains locks that are acquired
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@SuppressWarnings("UnusedShould")
public class TakenLock {

  // current implementation does not support values for locks

  //private final Map<BuildPromotionInfo, String> readLocks = new HashMap<BuildPromotionInfo, String>();

  //private final Map<BuildPromotionInfo, String> writeLocks = new HashMap<BuildPromotionInfo, String>();


  /**
   * Contains build promotions that have acquired read lock
   */
  @NotNull
  private final Set<BuildPromotionInfo> readLocks = new HashSet<BuildPromotionInfo>();

  /**
   * Contains build promotions that have acquired write lock
   */
  @NotNull
  private final Set<BuildPromotionInfo> writeLocks = new HashSet<BuildPromotionInfo>(); // do we need a set here? write lock is exclusive

  public void addLock(@NotNull final BuildPromotionInfo info, @NotNull final Lock lock) {
    switch (lock.getType()) {
      case READ:
        readLocks.add(info);
        break;
      case WRITE:
        writeLocks.add(info);
        break;
    }
  }

  @NotNull
  public Set<BuildPromotionInfo> getReadLocks() {
    return Collections.unmodifiableSet(readLocks);
  }

  public boolean hasReadLocks() {
    return !readLocks.isEmpty();
  }

  public boolean hasWriteLocks() {
    return !writeLocks.isEmpty();
  }
}
