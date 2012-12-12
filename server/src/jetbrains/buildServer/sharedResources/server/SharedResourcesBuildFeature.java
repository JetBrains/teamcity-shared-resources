/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Oleg Rybak
 */
public class SharedResourcesBuildFeature extends BuildFeature {

  @NotNull
  private final PluginDescriptor myDescriptor;

  public SharedResourcesBuildFeature(@NotNull PluginDescriptor descriptor) {
    myDescriptor = descriptor;
  }

  @NotNull
  @Override
  public String getType() {
    return SharedResourcesPluginConstants.FEATURE_TYPE;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return "Shared Resources Management";
  }

  @Nullable
  @Override
  public String getEditParametersUrl() {
    return myDescriptor.getPluginResourcesPath(SharedResourcesPluginConstants.EDIT_FEATURE_PATH_HTML);
  }

  @Override
  public boolean isMultipleFeaturesPerBuildTypeAllowed() {
    return false;
  }

  @NotNull
  @Override
  public String describeParameters(@NotNull Map<String, String> params) {
    final String locksParam = params.get(SharedResourcesPluginConstants.LOCKS_FEATURE_PARAM_KEY);
    final StringBuilder sb = new StringBuilder();
    final List<List<String>> lockNames = SharedResourcesUtils.getLockNames(locksParam);
    final List<String> readLockNames = lockNames.get(0);
    final List<String> writeLockNames = lockNames.get(1);
    if (!readLockNames.isEmpty()) {
      sb.append("Read locks used: ");
      sb.append(StringUtil.join(readLockNames, ", "));
    }
    if (!writeLockNames.isEmpty()) {
      sb.append("Write locks used: ");
      sb.append(StringUtil.join(writeLockNames, ", "));
    }
    if (sb.length() == 0) {
      sb.append("No locks are currently used by this build configuration");
    }

    return sb.toString();
  }

  @Nullable
  @Override
  public Map<String, String> getDefaultParameters() {
    final Map<String, String> result = new HashMap<String, String>();
    result.put(SharedResourcesPluginConstants.LOCKS_FEATURE_PARAM_KEY, "");
    return result;
  }
}
