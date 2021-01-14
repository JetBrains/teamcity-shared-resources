/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

import java.util.List;
import java.util.Map;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public interface ResourceProjectFeatures {

  @NotNull
  List<ResourceProjectFeature> getOwnFeatures(@NotNull final SProject project);

  SProjectFeatureDescriptor addFeature(@NotNull final SProject project,
                  @NotNull final Map<String, String> featureParameters);

  void updateFeature(@NotNull final SProject project,
                     @NotNull final String id,
                     @NotNull final Map<String, String> featureParameters);

  @Nullable
  SProjectFeatureDescriptor removeFeature(@NotNull final SProject project, @NotNull final String id);
}
