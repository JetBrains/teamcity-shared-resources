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

package jetbrains.buildServer.sharedResources.server.project;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.FEATURE_TYPE;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class ResourceProjectFeaturesImpl implements ResourceProjectFeatures {

  @Override
  public SProjectFeatureDescriptor addFeature(@NotNull final SProject project,
                                              @NotNull final Map<String, String> featureParameters) {
    return project.addFeature(FEATURE_TYPE, featureParameters);
  }

  @Override
  @Nullable
  public SProjectFeatureDescriptor removeFeature(@NotNull final SProject project, @NotNull final String id) {
    final SProjectFeatureDescriptor descriptor = getFeatureById(project, id);
    if (descriptor != null) {
      project.removeFeature(descriptor.getId());
    }
    return descriptor;
  }

  public void updateFeature(@NotNull final SProject project,
                            @NotNull final String id,
                            @NotNull final Map<String, String> featureParameters) {
    final SProjectFeatureDescriptor descriptor = getFeatureById(project, id);
    if (descriptor != null) {
      project.updateFeature(id, FEATURE_TYPE, featureParameters);
    }
  }

  @NotNull
  @Override
  public List<ResourceProjectFeature> getOwnFeatures(@NotNull final SProject project) {
    return getResourceFeatures(project).stream()
                                       .map(ResourceProjectFeatureImpl::new)
                                       .collect(Collectors.toList());
  }

  @Nullable
  private SProjectFeatureDescriptor getFeatureById(@NotNull final SProject project, @NotNull final String id) {
    return getResourceFeatures(project).stream()
                                       .filter(fd -> id.equals(fd.getId()))
                                       .findFirst()
                                       .orElse(null);
  }

  @NotNull
  private Collection<SProjectFeatureDescriptor> getResourceFeatures(@NotNull final SProject project) {
    return project.getOwnFeaturesOfType(FEATURE_TYPE);
  }
}
