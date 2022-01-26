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

package jetbrains.buildServer.sharedResources.server.feature;

import java.util.Collection;
import jetbrains.buildServer.serverSide.BuildTypeSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Interface {@code SharedResourceFeatures}
 * <p/>
 * Detects and extracts {@code SharedResourcesFeatures} from build type
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public interface SharedResourcesFeatures {

  /**
   * Searches for features of type {@code SharedResourcesBuildFeature} in given build type
   *
   * @param settings build type or template
   * @return {@code Collection} of build features of type {@code SharedResourcesBuildFeature} of there are any,
   *         {@code empty list} if there are none.
   *         <p/>
   *         <b>Be aware, that parameters are not resolved here</b>
   * @see jetbrains.buildServer.sharedResources.server.SharedResourcesBuildFeature#FEATURE_TYPE
   *      <p/>
   */
  @NotNull
  Collection<SharedResourcesFeature> searchForFeatures(@NotNull final BuildTypeSettings settings);
}
