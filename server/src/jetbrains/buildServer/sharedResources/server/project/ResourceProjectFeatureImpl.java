/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Oleg Rybak <oleg.rybak@jetbrains.com>
 */
public class ResourceProjectFeatureImpl implements ResourceProjectFeature {

  @NotNull
  private final SProjectFeatureDescriptor myDescriptor;

  @Nullable
  private final Resource myResource;

  public ResourceProjectFeatureImpl(@NotNull final SProjectFeatureDescriptor descriptor) {
    myDescriptor = descriptor;
    myResource = ResourceFactory.fromDescriptor(myDescriptor);
  }

  @Nullable
  @Override
  public Resource getResource() {
    return myResource;
  }

  @NotNull
  @Override
  public String getId() {
    return myDescriptor.getId();
  }
}
