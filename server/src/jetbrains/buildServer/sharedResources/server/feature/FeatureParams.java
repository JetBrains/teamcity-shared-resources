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

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Interface {@code FeatureParams}
 *
 * Contains methods for dealing with build feature parameters
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public interface FeatureParams {

  /**
   * Key in feature parameters collection, that contains all locks
   */
  @NotNull
  String LOCKS_FEATURE_PARAM_KEY = "locks-param";

  /**
   * Provides description for build feature parameters to be shown in UI
   * @param params build feature parameters
   * @return parameters description
   */
  @NotNull
  String describeParams(@NotNull final Map<String, String> params);

  /**
   * Provides default parameters for build feature
   * @return default parameters
   */
  @NotNull
  Map<String, String> getDefault();
}
