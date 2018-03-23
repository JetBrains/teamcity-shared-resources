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

import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.parameters.AbstractBuildParametersProvider;
import jetbrains.buildServer.serverSide.parameters.BuildParametersProvider;
import jetbrains.buildServer.sharedResources.server.feature.Locks;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeature;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.sharedResources.server.runtime.LocksStorage;
import org.jetbrains.annotations.NotNull;


/**
 * Class {@code BuildFeatureParametersProvider}
 *
 * Exposes {@code SharedResourcesBuildFeature} parameters to build
 *
 * @see jetbrains.buildServer.sharedResources.server.SharedResourcesBuildFeature
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class BuildFeatureParametersProvider extends AbstractBuildParametersProvider implements BuildParametersProvider {

  @NotNull
  private final SharedResourcesFeatures myFeatures;

  @NotNull
  private final LocksStorage myStorage;

  @NotNull
  private final Locks myLocks;

  public BuildFeatureParametersProvider(@NotNull final SharedResourcesFeatures features,
                                        @NotNull final Locks locks,
                                        @NotNull final LocksStorage storage) {
    myFeatures = features;
    myLocks = locks;
    myStorage = storage;
  }

  @NotNull
  @Override
  public Map<String, String> getParameters(@NotNull final SBuild build, boolean emulationMode) {
    if (emulationMode || !build.getBuildPromotion().isCompositeBuild()) {
      return getParametersFromFeatures(build);
    } else {
      return myLocks.asBuildParameters(myStorage.load(build.getBuildPromotion()).values());
    }
  }

  private Map<String, String> getParametersFromFeatures(@NotNull final SBuild build) {
    final Map<String, String> result = new HashMap<>();
    final SBuildType buildType = build.getBuildType();
    if (buildType != null) {
      myFeatures.searchForFeatures(buildType).stream()
                .map(SharedResourcesFeature::getBuildParameters)
                .forEach(result::putAll);
    }
    return result;
  }

}
