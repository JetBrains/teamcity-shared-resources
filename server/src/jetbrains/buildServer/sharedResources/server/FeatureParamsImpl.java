/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class {@code FeatureParamsImpl}
 *
 * Default implementation of build feature parameters
 *
 * @see FeatureParams
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public final class FeatureParamsImpl implements FeatureParams {

  // messages
  static final String NO_LOCKS_MESSAGE = "No locks are currently used by this build configuration";
  static final String READ_LOCKS_MESSAGE = "Read locks used: ";
  static final String WRITE_LOCKS_MESSAGE = "Write locks used: ";


  @NotNull
  @Override
  public String describeParams(@NotNull Map<String, String> params) {
    final String locksParam = params.get(FeatureParams.LOCKS_FEATURE_PARAM_KEY);
    final StringBuilder sb = new StringBuilder();
    final List<List<String>> lockNames = SharedResourcesUtils.getLockNames(locksParam);
    final List<String> readLockNames = lockNames.get(0);
    final List<String> writeLockNames = lockNames.get(1);
    if (!readLockNames.isEmpty()) {
      sb.append(READ_LOCKS_MESSAGE);
      sb.append(StringUtil.join(readLockNames, ", "));
    }
    if (!writeLockNames.isEmpty()) {
      sb.append(WRITE_LOCKS_MESSAGE);
      sb.append(StringUtil.join(writeLockNames, ", "));
    }
    if (sb.length() == 0) {
      sb.append(NO_LOCKS_MESSAGE);
    }
    return sb.toString();
  }

  @NotNull
  @Override
  public Map<String, String> getDefault() {
    final Map<String, String> result = new HashMap<String, String>();
    result.put(FeatureParams.LOCKS_FEATURE_PARAM_KEY, "");
    return result;
  }
}
