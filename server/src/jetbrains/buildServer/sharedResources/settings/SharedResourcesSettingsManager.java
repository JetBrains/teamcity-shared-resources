package jetbrains.buildServer.sharedResources.settings;

import jetbrains.buildServer.serverSide.settings.ProjectSettingsManager;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.SERVICE_NAME;

/**
 * Created with IntelliJ IDEA.
 * Date: 25.10.12
 * Time: 19:04
 *
 * @author Oleg Rybak
 */
public final class SharedResourcesSettingsManager {

  public SharedResourcesSettingsManager(@NotNull ProjectSettingsManager projectSettingsManager) {
    projectSettingsManager.registerSettingsFactory(SERVICE_NAME, new SharedResourcesSettingsFactory());
  }
}
