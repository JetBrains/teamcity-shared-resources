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

import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.LOCK_PREFIX;
import static jetbrains.buildServer.sharedResources.server.FeatureParams.LOCKS_FEATURE_PARAM_KEY;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public final class LocksImpl implements Locks {

  @NotNull
  @Override
  public Map<String, Lock> getLocksFromFeatureParameters(@NotNull final SBuildFeatureDescriptor descriptor) {
    return getLocksInternal(descriptor.getParameters());
  }

  @NotNull
  @Override
  public Map<String, Lock> getLocksFromFeatureParameters(@NotNull Map<String, String> parameters) {
    return getLocksInternal(parameters);
  }

  @NotNull
  private Map<String, Lock> getLocksInternal(@NotNull Map<String, String> parameters) {
    final String locksString = parameters.get(LOCKS_FEATURE_PARAM_KEY);
    final Map<String, Lock> result = new HashMap<String, Lock>();
    if (locksString != null && !"".equals(locksString)) {
      final List<String> serializedLocks = StringUtil.split(locksString, true, '\n');
      for (String str: serializedLocks) {
        final Lock lock = getSingleLockFromString(str);
        if (lock != null) {
          result.put(lock.getName(), lock);
        }
      }
    }
    return result;
  }

  @NotNull
  @Override
  public Map<String, String> asBuildParameters(@NotNull Collection<Lock> locks) {
    final Map<String, String> buildParams = new HashMap<String, String>();
    for (Lock lock: locks) {
      buildParams.put(lockAsBuildParam(lock), "");
    }
    return buildParams;
  }

  /**
   * Converts given locks to a {@code String} that is suitable to
   * exposure as a build parameter name
   *
   * @see jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants#LOCK_PREFIX
   * @param lock lock to convert
   * @return lock as {@code String}
   */
  @NotNull
  private String lockAsBuildParam(@NotNull Lock lock) {
    final StringBuilder sb = new StringBuilder(LOCK_PREFIX);
    sb.append(lock.getType());
    sb.append(".");
    sb.append(lock.getName());
    return sb.toString();
  }


  @Nullable
  private Lock getSingleLockFromString(@NotNull String str) {
    int n = str.lastIndexOf(' ');
    final LockType type = LockType.byName(str.substring(n + 1));
    Lock result = null;
    if (type != null) {
      result =  new Lock(str.substring(0, n), type);
    }
    return result;
  }
}
