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
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Class {@code TakenLock}.
 *
 * For each resource, instance of this class contains locks that are acquired
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class TakenLock {

  @NotNull
  private final Resource myResource;

  @NotNull
  private final Map<BuildPromotionInfo, String> readLocks = new HashMap<BuildPromotionInfo, String>();

  @NotNull
  private final Map<BuildPromotionInfo, String> writeLocks = new HashMap<BuildPromotionInfo, String>();

  public TakenLock(@NotNull final Resource resource) {
    myResource = resource;
  }

  public void addLock(@NotNull final BuildPromotionInfo info, @NotNull final Lock lock) {
    switch (lock.getType()) {
      case READ:
        readLocks.put(info, lock.getValue());
        break;
      case WRITE:
        writeLocks.put(info, lock.getValue());
        break;
    }
  }

  @NotNull
  public Map<BuildPromotionInfo, String> getReadLocks() {
    return Collections.unmodifiableMap(readLocks);
  }

  @NotNull
  public Map<BuildPromotionInfo, String> getWriteLocks() {
    return Collections.unmodifiableMap(writeLocks);
  }

  /**
   * Returns resource associated with current {@code TakenLock}
   *
   * @since 9.0
   * @return {@code Resource} of the {@code TakenLock}
   */
  @NotNull
  public Resource getResource() {
    return myResource;
  }

  /**
   * Gets overall locks count without differentiation by type
   * @return overall locks count
   */
  public int getLocksCount() {
    return readLocks.size() + writeLocks.size();
  }

  public boolean hasReadLocks() {
    return !readLocks.isEmpty();
  }

  public boolean hasWriteLocks() {
    return !writeLocks.isEmpty();
  }

}
