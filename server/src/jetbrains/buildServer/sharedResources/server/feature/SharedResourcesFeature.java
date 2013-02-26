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

import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.sharedResources.model.Lock;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@SuppressWarnings("UnusedShould")
public interface SharedResourcesFeature {


  /**
   * Gets locked resources from current build feature
   *
   * @return map of locked resources. Map format is {@code <LockName, Lock>}
   */
  @NotNull
  public Map<String, Lock> getLockedResources();

  /**
   * Updates lock inside build feature
   *
   * @param buildType build
   * @param oldName   name of the existing lock
   * @param newName   new lock name
   */
  public void updateLock(@NotNull final SBuildType buildType,
                         @NotNull final String oldName,
                         @NotNull final String newName);

  /**
   * Exposes locks as parameters
   *
   * @return map of locks, exposed as parameters
   */
  @NotNull
  public Map<String, String> getBuildParameters();


  /**
   * Checks that build feature is in valid state, i.e. can acquire all of its locks.
   * Main concern here are resources inherited from ancestor projects
   *
   * @param projectId project id to check feature in
   * @return Collection of invalid locks, i.e. locks that have either no resource or
   *         wrong type of resource (in case of custom valued lock backed by quoted resource)
   */
  @NotNull
  public Collection<Lock> getInvalidLocks(@NotNull final String projectId);

}
