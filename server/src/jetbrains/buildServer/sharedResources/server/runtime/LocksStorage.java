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

import jetbrains.buildServer.serverSide.BuildPromotion;
import jetbrains.buildServer.sharedResources.model.Lock;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Interface {@code LocksStorage}
 *
 * Contains method definition for storage of taken locks
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public interface LocksStorage {

  /**
   * Stores taken locks for given build
   *  @param buildPromotion build promotion to store locks for
   * @param takenLocks taken locks for given build with values
   */
  void store(@NotNull final BuildPromotion buildPromotion, @NotNull final Map<Lock, String> takenLocks);

  /**
   * Loads taken locks
   *
   * @param buildPromotion build promotion to load locks for
   * @return collection of taken locks. Values are restored inside locks
   */
  @NotNull
  Map<String, Lock> load(@NotNull final BuildPromotion buildPromotion);

  /**
   * Checks, whether locks has been already stored
   *
   * @param buildPromotion build promotion to check for
   * @return {@code true} if locks has been stored inside build artifact
   * {@code false} otherwise
   */
  boolean locksStored(@NotNull final BuildPromotion buildPromotion);

}
