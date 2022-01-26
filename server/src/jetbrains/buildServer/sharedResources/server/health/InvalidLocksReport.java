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

package jetbrains.buildServer.sharedResources.server.health;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.healthStatus.*;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.server.ConfigurationInspector;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.healthStatus.HealthStatusItemPageExtension;
import org.jetbrains.annotations.NotNull;


/**
 * Class {@code InvalidLocksReport}
 *
 * Reports problems in share resources plugin configuration and resource usage
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class InvalidLocksReport extends HealthStatusReport {

  @NotNull
  private static final String TYPE = "InvalidLocksReport";

  @NotNull
  private static final String CATEGORY_ID = "invalid_locks_on_shared_resources";

  @NotNull
  private static final String CATEGORY_NAME = "Invalid locks on shared resources";

  @NotNull
  private final ItemCategory myCategory;

  @NotNull
  private final ConfigurationInspector myInspector;

  public InvalidLocksReport(@NotNull final PluginDescriptor pluginDescriptor,
                            @NotNull final PagePlaces pagePlaces,
                            @NotNull final ConfigurationInspector inspector) {
    myInspector = inspector;
    myCategory = new ItemCategory(CATEGORY_ID, CATEGORY_NAME, ItemSeverity.WARN);
    final HealthStatusItemPageExtension myPEx = new HealthStatusItemPageExtension(TYPE, pagePlaces);
    myPEx.setIncludeUrl(pluginDescriptor.getPluginResourcesPath("health/invalidLocksReport.jsp"));
    myPEx.addCssFile("/css/admin/buildTypeForm.css");
    myPEx.setVisibleOutsideAdminArea(true);
    myPEx.register();
  }

  @Override
  @NotNull
  public String getType() {
    return TYPE;
  }

  @Override
  @NotNull
  public String getDisplayName() {
    return CATEGORY_NAME;
  }

  @Override
  @NotNull
  public Collection<ItemCategory> getCategories() {
    return Collections.singleton(myCategory);
  }

  @Override
  public boolean canReportItemsFor(@NotNull final HealthStatusScope scope) {
    return scope.isItemWithSeverityAccepted(myCategory.getSeverity());
  }

  @Override
  public void report(@NotNull final HealthStatusScope scope, @NotNull final HealthStatusItemConsumer resultConsumer) {
    for (final SBuildType type: scope.getBuildTypes()) {
      final Map<Lock, String> invalidLocks = myInspector.inspect(type);
      if (!invalidLocks.isEmpty()) {
        resultConsumer.consumeForBuildType(
                type,
                new HealthStatusItem(myCategory.getId() + "_" + type.getInternalId(), myCategory, new HashMap<String, Object>() {{
          put("invalid_locks", invalidLocks);
          put("build_type", type);
        }}));
      }
    }
  }
}
