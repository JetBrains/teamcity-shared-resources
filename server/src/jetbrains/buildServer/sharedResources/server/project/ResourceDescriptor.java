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

import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class ResourceDescriptor {

  @NotNull
  private final SProjectFeatureDescriptor myFeatureDescriptor;

  @NotNull
  private final SProject myProject;

  public ResourceDescriptor(@NotNull final SProject owner, @NotNull final SProjectFeatureDescriptor featureDescriptor) {
    myFeatureDescriptor = featureDescriptor;
    myProject = owner;
  }

  /**
   * Return id of the resource, i.e. its name
   *
   * @return id (name) of the resource
   */
  @NotNull
  public String getId() {
    return myFeatureDescriptor.getId();
  }

  @NotNull
  public Map<String, String> getParameters() {
    return myFeatureDescriptor.getParameters();
  }

  @NotNull
  public SProject getOwner() {
    return myProject;
  }
}
