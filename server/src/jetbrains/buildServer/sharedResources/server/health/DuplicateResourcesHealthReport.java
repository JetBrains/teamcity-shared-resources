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

import java.util.*;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.healthStatus.*;
import jetbrains.buildServer.sharedResources.server.ConfigurationInspector;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.healthStatus.HealthStatusItemPageExtension;
import org.jetbrains.annotations.NotNull;

/**
 * @author Oleg Rybak <oleg.rybak@jetbrains.com>
 */
public class DuplicateResourcesHealthReport extends HealthStatusReport {

  @NotNull
  private static final String TYPE = "DuplicateResourcesReport";

  @NotNull
  private final ItemCategory CATEGORY = new ItemCategory("duplicate__shared_resources",
                                                         "Resources with duplicate names",
                                                         ItemSeverity.ERROR);

  @NotNull
  private final ConfigurationInspector myInspector;

  public DuplicateResourcesHealthReport(@NotNull final PluginDescriptor pluginDescriptor,
                                        @NotNull final PagePlaces pagePlaces,
                                        @NotNull final ConfigurationInspector inspector) {
    myInspector = inspector;
    final HealthStatusItemPageExtension myPEx = new HealthStatusItemPageExtension(TYPE, pagePlaces);
    myPEx.setIncludeUrl(pluginDescriptor.getPluginResourcesPath("health/duplicateResourcesReport.jsp"));
    myPEx.setVisibleOutsideAdminArea(true);
    myPEx.register();
  }

  @NotNull
  @Override
  public String getType() {
    return TYPE;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return "Detect duplicate resource definitions";
  }

  @NotNull
  @Override
  public Collection<ItemCategory> getCategories() {
    return Collections.singleton(CATEGORY);
  }

  @Override
  public boolean canReportItemsFor(@NotNull HealthStatusScope scope) {
    return !scope.getProjects().isEmpty();
  }

  @Override
  public void report(@NotNull HealthStatusScope scope, @NotNull HealthStatusItemConsumer resultConsumer) {
    scope.getProjects().forEach(p -> {
      final List<String> dups = myInspector.getOwnDuplicateNames(p);
      if (!dups.isEmpty()) {
        resultConsumer.consumeForProject(p, createDupsHealthItem(p, dups));
      }
    });
  }

  @NotNull
  private HealthStatusItem createDupsHealthItem(@NotNull final SProject project,
                                                @NotNull final List<String> dups) {
    final Map<String, Object> data = new HashMap<>();
    data.put("project", project);
    data.put("duplicates", dups);
    return new HealthStatusItem(CATEGORY.getName() + "_" + project.getProjectId(), CATEGORY, data);
  }
}
