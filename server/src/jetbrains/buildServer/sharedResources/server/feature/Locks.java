/*
 * Copyright 2000-2018 JetBrains s.r.o.
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
 * Interface {@code Locks}
 *
 * Defines operations tha deal with locks
 *
 * @see jetbrains.buildServer.sharedResources.model.Lock
 * @see jetbrains.buildServer.sharedResources.model.LockType
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public interface Locks {

  /** Lock prefix, used in build parameters */
  String LOCK_PREFIX = "teamcity.locks.";

  /**
   * Parses build feature descriptor for parameters containing locks
   *
   * @param descriptor build feature descriptor
   * @return map of locks, taken by the build
   */
  @NotNull
  Map<String, Lock> fromFeatureParameters(@NotNull final SBuildFeatureDescriptor descriptor);


  /**
   * Parses given map of parameters for entries that contain locks
   *
   * @param parameters map of parameters
   * @return map of locks, taken by the build
   */
  @NotNull
  Map<String, Lock> fromFeatureParameters(@NotNull final Map<String, String> parameters);

  /**
   * Serializes given locks to build feature param
   *
   * @param locks collection of locks to serialize
   * @return {@code String} that represents given locks
   */
  @NotNull
  String asFeatureParameter(@NotNull final Collection<Lock> locks);

  /**
   * Converts given lock into build parameter
   * @param lock lock to convert
   * @return name of the build parameter, corresponding to given lock
   */
  @NotNull
  String asBuildParameter(@NotNull final Lock lock);

  /**
   * Converts given collection of locks to build parameters
   *
   * @param locks locks to convert
   * @return {@code Collection} of build parameters that represent given locks
   */
  @NotNull
  Map<String, String> asBuildParameters(@NotNull final Collection<Lock> locks);

  /**
   * Extracts locks from build features
   * @param features features to extract locks from
   * @return map of locks defined in build features
   */
  @NotNull
  Map<String, Lock> fromBuildFeaturesAsMap(@NotNull final Collection<SharedResourcesFeature> features);

}
