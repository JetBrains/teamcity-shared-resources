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

package jetbrains.buildServer.sharedResources.server.report;

import java.util.Collection;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import org.jetbrains.annotations.NotNull;

/**
 * Represents {@code Resource} used by the build
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class UsedResource {
  
  @NotNull
  private final Resource myResource;

  @NotNull
  private final Collection<Lock> myLocks;

  UsedResource(@NotNull final Resource resource,
                      @NotNull final Collection<Lock> locks) {
    myResource = resource;
    myLocks = locks;
  }

  @NotNull
  public Resource getResource() {
    return myResource;
  }

  @NotNull
  public Collection<Lock> getLocks() {
    return myLocks;
  }
}
