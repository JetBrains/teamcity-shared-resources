

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
                                                         "Invalid shared resources definitions",
                                                         ItemSeverity.ERROR);

  @NotNull
  private final ConfigurationInspector myInspector;

  public InvalidResourcesHealthReport(@NotNull final PluginDescriptor pluginDescriptor,
                                      @NotNull final PagePlaces pagePlaces,
                                      @NotNull final ConfigurationInspector inspector) {
    myInspector = inspector;
    final HealthStatusItemPageExtension myPEx = new HealthStatusItemPageExtension(TYPE, pagePlaces);
    myPEx.setIncludeUrl(pluginDescriptor.getPluginResourcesPath("health/invalidResourcesReport.jsp"));
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

  private HealthStatusItem createDefinitionErrorsItem(@NotNull final SProject project,
                                                      @NotNull final Map<String, List<String>> definitionErrors) {
    final Map<String, Object> data = new HashMap<>();
    data.put("invalidResources", definitionErrors);
    data.put("project", project);
    return new HealthStatusItem(CATEGORY.getName() + "_" + project.getProjectId(), CATEGORY, data);
  }
}