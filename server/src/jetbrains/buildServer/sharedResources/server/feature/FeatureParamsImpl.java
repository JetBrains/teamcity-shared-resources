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

package jetbrains.buildServer.sharedResources.server.feature;

import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.sharedResources.model.Lock;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class {@code FeatureParamsImpl}
 * <p/>
 * Default implementation of build feature parameters
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 * @see FeatureParams
 */
public final class FeatureParamsImpl implements FeatureParams {

  @NotNull
  private final Locks myLocks;

  @NotNull
  static final String NO_LOCKS_MESSAGE = "No locks are currently used by this build feature.";

  @NotNull
  static final String READ_LOCKS_MESSAGE = "Read locks used: ";

  @NotNull
  static final String WRITE_LOCKS_MESSAGE = "Write locks used: ";

  public FeatureParamsImpl(@NotNull final Locks locks) {
    myLocks = locks;
  }

  @NotNull
  @Override
  public String describeParams(@NotNull final Map<String, String> params) {
    final StringBuilder sb = new StringBuilder();
    final Map<String, Lock> locks = myLocks.fromFeatureParameters(params);
    final List<String> readLockNames = new ArrayList<String>();
    final List<String> writeLockNames = new ArrayList<String>();
    for (Lock lock : locks.values()) {
      switch (lock.getType()) {
        case READ:
          readLockNames.add(lock.getName());
          break;
        case WRITE:
          writeLockNames.add(lock.getName());
          break;
      }
    }
    if (!readLockNames.isEmpty()) {
      sb.append(READ_LOCKS_MESSAGE);
      sb.append(StringUtil.join(readLockNames, ", "));
      sb.append(". ");
    }
    if (!writeLockNames.isEmpty()) {
      sb.append(WRITE_LOCKS_MESSAGE);
      sb.append(StringUtil.join(writeLockNames, ", "));
      sb.append(". ");
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
    result.put(LOCKS_FEATURE_PARAM_KEY, "");
    return result;
  }
}
