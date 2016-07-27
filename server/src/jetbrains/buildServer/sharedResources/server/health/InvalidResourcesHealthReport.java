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
public class InvalidResourcesHealthReport extends HealthStatusReport {

  @NotNull
  private static final String TYPE = "InvalidResourcesReport";

  @NotNull
  private final ItemCategory CATEGORY = new ItemCategory("invalid_shared_resources_definitions",
                                                         "Invalid Shared Resources Definitions",
                                                         ItemSeverity.ERROR);

  @NotNull
  private final ConfigurationInspector myInspector;

  public InvalidResourcesHealthReport(@NotNull final PluginDescriptor pluginDescriptor,
                                      @NotNull final PagePlaces pagePlaces,
                                      @NotNull final ConfigurationInspector inspector) {
    myInspector = inspector;
    final HealthStatusItemPageExtension myPEx = new HealthStatusItemPageExtension(TYPE, pagePlaces);
    myPEx.setIncludeUrl(pluginDescriptor.getPluginResourcesPath("/health/invalidResourcesReport.jsp"));
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
    return "Detect errors in shared resources definitions";
  }

  @NotNull
  @Override
  public Collection<ItemCategory> getCategories() {
    return Collections.singleton(CATEGORY);
  }

  @Override
  public boolean canReportItemsFor(@NotNull final HealthStatusScope scope) {
    return !scope.getProjects().isEmpty();
  }

  @Override
  public void report(@NotNull final HealthStatusScope scope, @NotNull final HealthStatusItemConsumer resultConsumer) {
    scope.getProjects().forEach(p -> {
      final Map<String, List<String>> definitionErrors = myInspector.getOwnResourceDefinitionErrors(p);
      if (!definitionErrors.isEmpty()) {
        resultConsumer.consumeForProject(p, createDefinitionErrorsItem(p, definitionErrors));
      }
    });
  }

  private HealthStatusItem createDefinitionErrorsItem(@NotNull final SProject p,
                                                      @NotNull final Map<String, List<String>> definitionErrors) {
    final Map<String, Object> data = new HashMap<>();
    data.put("definitionErrors", definitionErrors);
    return new HealthStatusItem(CATEGORY.getName() + p.getProjectId(), CATEGORY, data);
  }
}
