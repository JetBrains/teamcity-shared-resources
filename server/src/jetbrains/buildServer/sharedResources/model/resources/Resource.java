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

package jetbrains.buildServer.sharedResources.model.resources;

import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class Resource {

  @NotNull
  private final String myName;

  private final int myQuota;

  private Resource(@NotNull String name, int quota) {
    myName = name;
    myQuota = quota;
  }

  public boolean isInfinite() {
    return myQuota < 0;
  }

  @NotNull
  public String getName() {
    return myName;
  }

  public int getQuota() {
    return myQuota;
  }

  public static Resource newResource(@NotNull String name, int quota) {
    return new Resource(name, quota);
  }

  public static Resource newInfiniteResource(@NotNull String name) {
    return new Resource(name, -1);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Resource)) return false;
    Resource resource = (Resource) o;
    return myName.equals(resource.myName);
  }

  @Override
  public int hashCode() {
    return myName.hashCode();
  }
}
