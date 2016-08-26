package jetbrains.buildServer.sharedResources.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceType;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeature;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.util.text.StringUtil.isEmptyOrSpaces;
import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.ProjectFeatureParameters.*;

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
      final List<String> duplicateNames = getOwnDuplicateNames(p);
      if (!duplicateNames.isEmpty()) {
        result.put(p, duplicateNames);
      }
    });
    return result;
  }

  /**
   * Checks for duplicate resources in given project
   * @param project project to check
   * @return list of duplicate resource names
   */
  @NotNull
  public List<String> getOwnDuplicateNames(@NotNull final SProject project) {
    return myResources.getAllOwnResources(project).stream()
                      .collect(Collectors.groupingBy(Resource::getName))
                      .values().stream()
                      .filter(list -> list.size() > 1)
                      .map(dup -> dup.get(0).getName())
                      .collect(Collectors.toList());
  }

  /**
   * Inspects project features for resource definition errors
   * @param project project to inspect
   * @return {@code Map<FEATURE_ID, ErrorDetails>}
   */
  @NotNull
  public Map<String, List<String>> getOwnResourceDefinitionErrors(@NotNull final SProject project) {
    final Map<String, List<String>> result = new HashMap<>();
    project.getOwnFeaturesOfType(SharedResourcesPluginConstants.FEATURE_TYPE).forEach(
      fd -> {
        final List<String> errors = new ArrayList<>();
        final Map<String, String> parameters = fd.getParameters();
        final String name = parameters.get(NAME);
        if (isEmptyOrSpaces(name)) {
          errors.add("Required parameter 'name' is missing");
        }
        final String type = parameters.get(TYPE);
        if (isEmptyOrSpaces(type)) {
          errors.add("Required parameter 'type' is missing");
        } else {
          ResourceType resourceType = ResourceType.fromString(type);
          if (resourceType == null) {
            errors.add("Value of parameter 'type' (" + type + ") is incorrect. Correct values are: " +
                       ResourceType.getCorrectValues().stream().collect(Collectors.joining(", ")));
          } else {
            // we have correct type
            // check the parameters that this type requires
            if (resourceType == ResourceType.QUOTED) {
              String quota = parameters.get(QUOTA);
              if (isEmptyOrSpaces(quota)) {
                errors.add("Required parameter 'quota' is missing");
              } else {
                try {
                  int q = Integer.parseInt(quota);
                  if (q < -1) {
                    errors.add("Value of parameter 'quota' must be either positive, or -1 for infinite quota. Got '" + quota + "'");
                  }
                } catch (NumberFormatException e) {
                  errors.add("Value of parameter 'quota' must be a valid integer. Got '" + quota + "'");
                }
              }
            } else {
              // resource with custom values
              final String values = parameters.get(VALUES);
              if (isEmptyOrSpaces(values)) {
                errors.add("Required parameter 'values' is missing");
              } else {
                final List<String> vals = StringUtil.split(parameters.get(VALUES), true, '\r', '\n');
                if (vals.isEmpty()) {
                  errors.add("At least one value for the resource with custom values must be defined");
                }
              }
            }
          }
        }
        if (!errors.isEmpty()) {
          result.put(fd.getId(), errors);
        }
      });
    return result;
  }
}
