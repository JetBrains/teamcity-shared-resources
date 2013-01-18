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

import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.parameters.AbstractBuildParametersProvider;
import jetbrains.buildServer.serverSide.parameters.BuildParametersProvider;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static jetbrains.buildServer.sharedResources.server.SharedResourcesUtils.searchForFeature;

/**
 * Class {@code BuildFeatureParametersProvider}
 *
 * Exposes {@code SharedResourcesBuildFeature} parameters to build
 *
 * @see SharedResourcesBuildFeature
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class BuildFeatureParametersProvider extends AbstractBuildParametersProvider implements BuildParametersProvider {

  @NotNull
  @Override
  public Map<String, String> getParameters(@NotNull SBuild build, boolean emulationMode) {
    final Map<String, String> result = new HashMap<String, String>();
    final SBuildType buildType = build.getBuildType();
    if (buildType != null) {
      SBuildFeatureDescriptor myFeatureDescriptor = searchForFeature(buildType, false);
      if (myFeatureDescriptor != null) {
        myFeatureDescriptor = searchForFeature(buildType, true); // resolving params here
        if (myFeatureDescriptor != null) {
          final String serializedBuildParams = myFeatureDescriptor.getParameters().get(FeatureParams.LOCKS_FEATURE_PARAM_KEY);
          result.putAll(SharedResourcesUtils.featureParamToBuildParams(serializedBuildParams));
        }
      }
    }
    return result;
  }
}
