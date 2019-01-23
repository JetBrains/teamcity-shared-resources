/*
 * Copyright 2000-2019 JetBrains s.r.o.
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

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.serverSide.BuildPromotionEx;
import jetbrains.buildServer.serverSide.buildDistribution.DistributionCycleExtension;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.DistributionData;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.getReservedResourceAttributeKey;

public class ReservedValuesProvider {

  public ReservedValuesProvider(@NotNull final ExtensionHolder extensionHolder) {
    extensionHolder.registerExtension(DistributionCycleExtension.class, getClass().getName(),
                                      (build, agent, distributionContext, emulationMode) -> {
                                        final BuildPromotionEx promotion = (BuildPromotionEx)build.getBuildPromotionInfo();
                                        if (!promotion.isCompositeBuild()) {
                                          DistributionData data = (DistributionData)distributionContext.get(DistributionDataAccessor.DISTRIBUTION_DATA_KEY);
                                          if (data != null) {
                                            data.getResourceAffinity().getRequestedValues(promotion)
                                                .forEach((resourceId, value) -> promotion.setAttribute(getReservedResourceAttributeKey(resourceId), value));
                                          }
                                        }
                                        return true;
                                      });
  }
}
