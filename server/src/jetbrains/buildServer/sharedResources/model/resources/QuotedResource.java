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

package jetbrains.buildServer.sharedResources.model.resources;

import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class QuotedResource extends AbstractResource {

  private static final int QUOTA_INFINITE = -1;

  private final int myQuota;

  private QuotedResource(@NotNull final String id,
                         @NotNull final String projectId,
                         @NotNull String name,
                         int quota,
                         boolean state) {
    super(id, projectId, name, ResourceType.QUOTED, state);
    myQuota = quota;
  }

  @NotNull
  static QuotedResource newResource(@NotNull final String id, @NotNull final String projectId, @NotNull String name, int quota, boolean state) {
    return new QuotedResource(id, projectId, name, quota, state);
  }

  @NotNull
  static QuotedResource newInfiniteResource(@NotNull final String id, @NotNull final String projectId, @NotNull String name, boolean state) {
    return new QuotedResource(id, projectId, name, QUOTA_INFINITE, state);
  }

  public boolean isInfinite() {
    return myQuota < 0;
  }

  public int getQuota() {
    return myQuota;
  }

  @NotNull
  @Override
  public Map<String, String> getParameters() {
    final Map<String, String> result =  super.getParameters();
    result.put("quota", Integer.toString(myQuota));
    return result;
  }
}
