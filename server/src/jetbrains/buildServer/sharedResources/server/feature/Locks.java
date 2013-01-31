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

import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.sharedResources.model.Lock;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public interface Locks {

  /**
   * Parses build feature parameters for taken locks
   * @param descriptor build feature descriptor
   * @return map of locks, taken by the build
   */
  @NotNull
  public Map<String, Lock> getLocksFromFeatureParameters(@NotNull final SBuildFeatureDescriptor descriptor);

  public Map<String, Lock> getLocksFromFeatureParameters(@NotNull final Map<String, String> parameters);


  /**
   * Converts given collection of locks to build parameters
   * @param locks locks to convert
   * @return {@code Collection} of build parameters that represent given locks
   */
  @NotNull
  public Map<String, String> asBuildParameters(@NotNull final Collection<Lock> locks);


}
