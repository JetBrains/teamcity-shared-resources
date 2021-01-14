/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

package jetbrains.buildServer.sharedResources.server;

import java.util.*;
import java.util.stream.Collectors;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.resources.CustomResource;
import jetbrains.buildServer.sharedResources.model.resources.QuotedResource;
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
    return getInvalidLocks(type.getProject(), myFeatures.searchForFeatures(type));
  }

  @NotNull
  public Map<Lock, String> inspect(@NotNull final SProject project, @NotNull final SharedResourcesFeature feature) {
    return getInvalidLocks(project, Collections.singleton(feature));
  }

  /**
   * Checks project path for duplicate resource definitions
   * @param project project to check path for
   * @return map of projects to the list of duplicate resources in the project
   */
  @NotNull
  Map<SProject, List<String>> getDuplicateResources(@NotNull final SProject project) {
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

  private static final String OK = "OK";

  private Map<Lock, String> getInvalidLocks(@NotNull final SProject project,
                                            @NotNull final Collection<SharedResourcesFeature> features) {
    final Map<Lock, String> result = new HashMap<>();
    final Map<String, Lock> locks = new HashMap<>();
    features.stream().map(SharedResourcesFeature::getLockedResources).forEach(locks::putAll);
    if (locks.isEmpty()) {
      return result;
    }
    final List<SProject> path = project.getProjectPath();
    final ListIterator<SProject> iterator = path.listIterator(path.size());
    while (iterator.hasPrevious() && !locks.isEmpty()) {
      SProject p = iterator.previous();
      // try to resolve against current project.
      // 1) if any of unresolved locks hit duplicates - add error
      Set<String> duplicates = new HashSet<>(getOwnDuplicateNames(p));
      if (!duplicates.isEmpty()) {
        // intersect
        duplicates.retainAll(locks.keySet());
        if (!duplicates.isEmpty()) {
          duplicates.forEach(dup -> {
            Lock lock = locks.remove(dup);
            if (lock != null) {
              result.put(lock, "Resource '" + lock.getName() + "' cannot be resolved due to duplicate name");
            }
          });
        }
      }
      if (locks.isEmpty()) {
        break;
      }
      // 2) resolve rest of the locks
      Map<String, String> resolutionResult = resolveStep(myResources.getOwnResources(p), locks);
      resolutionResult.forEach((name, res) -> {
        Lock lock = locks.remove(name);
        if (!OK.equals(res) && lock != null) { // we have error.
          result.put(lock, res);
        }
      });
    }
    // 3) after all iterations, only locks left are those without resources
    locks.values().forEach(lock -> result.put(lock, "Resource '" + lock.getName() + "' does not exist"));
    return result;
  }

  @NotNull
  private Map<String, String> resolveStep(@NotNull final List<Resource> resources,
                                          @NotNull final Map<String, Lock> locks) {
    Map<String, String> result = new HashMap<>();
    resources.forEach(rc -> {
      Lock lock = locks.get(rc.getName());
      if (lock != null) {
        // some lock is requesting this resource
        result.put(lock.getName(), tryMatch(rc, lock));
      }
    });
    return result;
  }

  @NotNull
  private String tryMatch(@NotNull final Resource r, @NotNull final Lock lock) {
    if (!"".equals(lock.getValue())) {
      if (ResourceType.CUSTOM == r.getType()) {
        if (!((CustomResource) r).getValues().contains(lock.getValue())) {
          // values domain does not contain required value
          return "Resource '" + lock.getName() + "' does not contain required value '" + lock.getValue() + "'";
        }
      } else {
        // wrong resource type. Expected quoted / infinite, got custom
        return "Resource '" + lock.getName() + "' has wrong type: expected 'custom' got " + (((QuotedResource) r).isInfinite() ? "'infinite'" : "'quoted'");
      }
    }
    return OK;
  }
}
