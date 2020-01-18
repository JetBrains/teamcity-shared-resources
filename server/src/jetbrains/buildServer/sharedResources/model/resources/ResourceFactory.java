/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

package jetbrains.buildServer.sharedResources.model.resources;

import java.util.List;
import java.util.Map;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.openapi.util.text.StringUtil.isEmptyOrSpaces;
import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.ProjectFeatureParameters.*;
import static jetbrains.buildServer.util.StringUtil.split;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class ResourceFactory {

  /**
   * Creates new quoted resource with infinite quota
   *
   * @param name name of the resource
   * @param state state of the resource
   * @return new infinite quoted resource
   */
  @NotNull
  public static Resource newInfiniteResource(@NotNull final String id, @NotNull final String projectId, @NotNull final String name, boolean state) {
    return QuotedResource.newInfiniteResource(id, projectId, name, state);
  }

  /**
   * Creates new quoted resource with limited quota
   *
   * @param name name of the resource
   * @param quota resource quota
   * @param state state of the resource
   * @return new quoted resource with limited quota
   */
  @NotNull
  public static Resource newQuotedResource(@NotNull final String id, @NotNull final String projectId, @NotNull final String name, final int quota, boolean state) {
    return QuotedResource.newResource(id, projectId, name, quota, state);
  }

  /**
   * Creates new custom resource with specified value space
   *
   * @param name name of the resource
   * @param values values
   * @param state state of the resource
   * @return new custom resource with specified value space
   */
  @NotNull
  public static Resource newCustomResource(@NotNull final String id, @NotNull final String projectId, @NotNull final String name, @NotNull final List<String> values, boolean state) {
    return CustomResource.newCustomResource(id, projectId, name, values, state);
  }

  @Nullable
  public static Resource fromDescriptor(@NotNull final SProjectFeatureDescriptor descriptor) {
    Resource result = null;
    final Map<String, String> parameters = descriptor.getParameters();
    ResourceType type = ResourceType.fromString(parameters.get(TYPE));
    final String enabledStr = parameters.get(ENABLED);
    final boolean resourceState = enabledStr == null || Boolean.parseBoolean(enabledStr);
    final String name = parameters.get(NAME);
    if (isEmptyOrSpaces(name)) {
      return null;
    }
    if (type == ResourceType.QUOTED) {
      final String quotaStr = parameters.get(QUOTA);
      if (!isEmptyOrSpaces(quotaStr)) {
        try {
          int quota = Integer.parseInt(quotaStr);
          result = QuotedResource.newResource(descriptor.getId(), descriptor.getProjectId(), name, quota, resourceState);
        } catch (NumberFormatException ignored) {}
      }
    } else {
      final String valuesStr = parameters.get(VALUES);
      if (!isEmptyOrSpaces(valuesStr)) {
        List<String> values = split(valuesStr, true, '\r', '\n');
        if (!values.isEmpty()) {
          result = CustomResource.newCustomResource(descriptor.getId(), descriptor.getProjectId(), name, values, resourceState);
        }
      }
    }
    return result;
  }

}
