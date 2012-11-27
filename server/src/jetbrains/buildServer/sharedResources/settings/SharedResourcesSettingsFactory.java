package jetbrains.buildServer.sharedResources.settings;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.serverSide.settings.ProjectSettings;
import jetbrains.buildServer.serverSide.settings.ProjectSettingsFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Class {@code SharedResourcesSettingsFactory}
 *
 * @author Oleg Rybak
 */
public final class SharedResourcesSettingsFactory implements ProjectSettingsFactory {

  private static final Logger LOG = Logger.getInstance(SharedResourcesSettingsFactory.class.getName());

  @NotNull
  @Override
  public ProjectSettings createProjectSettings(String projectId) {
    return new SharedResourcesProjectSettings();
  }
}
