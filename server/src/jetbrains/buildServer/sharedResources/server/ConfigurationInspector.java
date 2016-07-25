package jetbrains.buildServer.sharedResources.server;

import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeature;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

  @NotNull
  private final Resources myResources;

  public ConfigurationInspector(@NotNull final SharedResourcesFeatures features,
                                @NotNull final Resources resources) {
    myFeatures = features;
    myResources = resources;
  }

  @NotNull
  public Map<Lock, String> inspect(@NotNull final SBuildType type) {
    final Map<Lock, String> result = new HashMap<>();
    for (SharedResourcesFeature feature: myFeatures.searchForFeatures(type)) {
      result.putAll(feature.getInvalidLocks(type.getProjectId()));
    }
    return result;
  }

  /**
   * Checks project path for duplicate resource definitions
   * @param project project to check path for
   * @return map of projects to the list of duplicate resources in the project
   */
  @NotNull
  public Map<SProject, List<String>> getDuplicateResources(@NotNull final SProject project) {
    final Map<SProject, List<String>> result = new HashMap<>();
    project.getProjectPath().forEach(p -> {
      final Map<String, List<Resource>> res = myResources.getOwnResources(p).stream()
              .collect(Collectors.groupingBy(Resource::getName));
      final List<String> duplicateNames = res.values().stream()
              .filter(list -> list.size() > 1)
              .map(dup -> dup.get(0).getName())
              .collect(Collectors.toList());
      if (!duplicateNames.isEmpty()) {
        result.put(p, duplicateNames);
      }
    });
    return result;
  }
}
