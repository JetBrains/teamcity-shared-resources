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

package jetbrains.buildServer.sharedResources.server.runtime;

import java.util.List;
import java.util.Map;
import jetbrains.buildServer.serverSide.BuildPromotion;
import jetbrains.buildServer.serverSide.buildDistribution.AgentsFilterContext;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.DistributionData;
import org.jetbrains.annotations.NotNull;

public class DistributionDataAccessor {

  @NotNull
  static final String DISTRIBUTION_DATA_KEY = SharedResourcesPluginConstants.PLUGIN_NAME;

  private DistributionData myData;

  public DistributionDataAccessor(@NotNull AgentsFilterContext context) {
    myData = (DistributionData)context.getCustomData(DISTRIBUTION_DATA_KEY);
    if (myData == null) {
      myData = new DistributionData();
      context.setCustomData(DISTRIBUTION_DATA_KEY, myData);
    }
  }

  public Map<String, List<BuildPromotion>> getFairSet() {
    return myData.getFairSet();
  }

  public ResourceAffinity getResourceAffinity() {
    return myData.getResourceAffinity();
  }
}
