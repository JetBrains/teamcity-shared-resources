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

package jetbrains.buildServer.sharedResources.model.resources;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Class {@code CustomResource}
 *
 * Represents resource with custom value space
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class CustomResource extends AbstractResource {

  @NotNull
  private final List<String> myValues;

  private CustomResource(@NotNull final String id,
                         @NotNull final String projectId,
                         @NotNull final String name,
                         @NotNull final List<String> values,
                         boolean state) {
    super(id, projectId, name, ResourceType.CUSTOM, state);
    myValues = new ArrayList<>(values);
  }

  @NotNull
  static CustomResource newCustomResource(@NotNull final String id,
                                          @NotNull final String projectId,
                                          @NotNull final String name,
                                          @NotNull final List<String> values,
                                          boolean state) {
    return new CustomResource(id, projectId, name, values, state);
  }

  @NotNull
  public List<String> getValues() {
    return Collections.unmodifiableList(myValues);
  }

  @NotNull
  @Override
  public Map<String, String> getParameters() {
    final Map<String, String> result = super.getParameters();
    result.put("values", myValues.stream().collect(Collectors.joining("\n")));
    return result;
  }
}
