/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.serverSide.BuildPromotionEx;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import org.jetbrains.annotations.NotNull;

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
  private final Map<BuildPromotionEx, String> myReadLocks = new HashMap<>();

  @NotNull
  private final Map<BuildPromotionEx, String> myWriteLocks = new HashMap<>();

  public TakenLock(@NotNull final Resource resource) {
    myResource = resource;
  }

  public TakenLock(@NotNull final Resource resource,
                   @NotNull final Map<BuildPromotionEx, String> readLocks,
                   @NotNull final Map<BuildPromotionEx, String> writeLocks) {
    myResource = resource;
    myReadLocks.putAll(readLocks);
    myWriteLocks.putAll(writeLocks);
  }

  public void addLock(@NotNull final BuildPromotionEx info, @NotNull final Lock lock) {
    switch (lock.getType()) {
      case READ:
        myReadLocks.put(info, lock.getValue());
        break;
      case WRITE:
        myWriteLocks.put(info, lock.getValue());
        break;
    }
  }

  @NotNull
  public Map<BuildPromotionEx, String> getReadLocks() {
    return Collections.unmodifiableMap(myReadLocks);
  }

  @NotNull
  public Map<BuildPromotionEx, String> getWriteLocks() {
    return Collections.unmodifiableMap(myWriteLocks);
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
    return myReadLocks.size() + myWriteLocks.size();
  }

  public boolean hasReadLocks() {
    return !myReadLocks.isEmpty();
  }

  public boolean hasWriteLocks() {
    return !myWriteLocks.isEmpty();
  }

}
