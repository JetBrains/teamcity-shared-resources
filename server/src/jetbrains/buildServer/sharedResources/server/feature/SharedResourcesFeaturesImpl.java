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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import jetbrains.buildServer.serverSide.BuildTypeSettings;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.sharedResources.server.SharedResourcesBuildFeature.FEATURE_TYPE;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public final class SharedResourcesFeaturesImpl implements SharedResourcesFeatures {

  @NotNull
  private final SharedResourcesFeatureFactory myFactory;

  public SharedResourcesFeaturesImpl(@NotNull final SharedResourcesFeatureFactory factory) {
    myFactory = factory;
  }

  @NotNull
  @Override
  public Collection<SharedResourcesFeature> searchForFeatures(@NotNull final BuildTypeSettings settings) {
    return createFeatures(getEnabledUnresolvedFeatureDescriptors(settings));
  }

  @NotNull
  private Collection<SharedResourcesFeature> createFeatures(@NotNull final Collection<SBuildFeatureDescriptor> descriptors) {
    final List<SharedResourcesFeature> result = new ArrayList<>();
    for (SBuildFeatureDescriptor descriptor : descriptors) {
      result.add(myFactory.createFeature(descriptor));
    }
    return result;
  }

  @NotNull
  private Collection<SBuildFeatureDescriptor> getEnabledUnresolvedFeatureDescriptors(@NotNull final BuildTypeSettings settings) {
    final List<SBuildFeatureDescriptor> result = new ArrayList<>();
    for (SBuildFeatureDescriptor descriptor : settings.getBuildFeaturesOfType(FEATURE_TYPE)) {
      if (settings.isEnabled(descriptor.getId())) {
        result.add(descriptor);
      }
    }
    return result;
  }
}
