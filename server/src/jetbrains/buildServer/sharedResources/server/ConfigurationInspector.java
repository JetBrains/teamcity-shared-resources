package jetbrains.buildServer.sharedResources.server;

import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeature;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Class {@code ConfigurationInspector}
 *
 * Inspects build configuration settings and reports errors
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class ConfigurationInspector {

  @NotNull
  private final SharedResourcesFeatures myFeatures;

  public ConfigurationInspector(@NotNull final SharedResourcesFeatures features) {
    myFeatures = features;
  }

  public Map<Lock, String> inspect(@NotNull final SBuildType type) {
    final Map<Lock, String> result = new HashMap<Lock, String>();
    for (SharedResourcesFeature feature: myFeatures.searchForFeatures(type)) {
      result.putAll(feature.getInvalidLocks(type.getProjectId()));
    }
    return result;
  }

}
