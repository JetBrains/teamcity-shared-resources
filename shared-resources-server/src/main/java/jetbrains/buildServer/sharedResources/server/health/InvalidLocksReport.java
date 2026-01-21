

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