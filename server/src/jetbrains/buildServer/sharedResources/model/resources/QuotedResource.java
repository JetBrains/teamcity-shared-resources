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
public class QuotedResource extends AbstractResource {

  static final int QUOTA_INFINITE = -1;

  private final int myQuota;

  private QuotedResource(@NotNull String name, int quota) {
    super(name, ResourceType.QUOTED);
    myQuota = quota;
  }

  static QuotedResource newResource(@NotNull String name, int quota) {
    return new QuotedResource(name, quota);
  }

  static QuotedResource newInfiniteResource(@NotNull String name) {
    return new QuotedResource(name, QUOTA_INFINITE);
  }

  public boolean isInfinite() {
    return myQuota < 0;
  }

  public int getQuota() {
    return myQuota;
  }
}
