/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public enum ResourceType {

  /**
   * Resource with quota without custom values.
   * Quota can be infinite
   */
  QUOTED,

  /**
   * Resource that has custom value space
   */
  CUSTOM;

  @Nullable
  public static ResourceType fromString(@Nullable final String str) {
    if (str == null) {
      return null;
    } else {
      for (ResourceType type: values()) {
        if (type.toString().equalsIgnoreCase(str)) {
          return type;
        }
      }
    }
    return null;
  }

  public static List<String> getCorrectValues() {
    return Arrays.stream(ResourceType.values()).map(Enum::name).collect(Collectors.toList());
  }
}
