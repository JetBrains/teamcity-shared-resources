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

package jetbrains.buildServer.sharedResources.server.analysis;

import java.util.List;
import java.util.Map;
import jetbrains.buildServer.serverSide.BuildTypeTemplate;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.sharedResources.model.Lock;
import org.jetbrains.annotations.NotNull;

public class FindUsagesResult {

  @NotNull
  private final Map<SBuildType, List<Lock>> myBuildTypes;

  @NotNull
  private final Map<BuildTypeTemplate, List<Lock>> myTemplates;

  FindUsagesResult(@NotNull final Map<SBuildType, List<Lock>> buildTypes,
                   @NotNull final Map<BuildTypeTemplate, List<Lock>> templates) {
    myBuildTypes = buildTypes;
    myTemplates = templates;
  }

  public int getTotal() {
    return myBuildTypes.size() + myTemplates.size();
  }

  @NotNull
  public Map<SBuildType, List<Lock>> getBuildTypes() {
    return myBuildTypes;
  }

  @NotNull
  public Map<BuildTypeTemplate, List<Lock>> getTemplates() {
    return myTemplates;
  }
}
