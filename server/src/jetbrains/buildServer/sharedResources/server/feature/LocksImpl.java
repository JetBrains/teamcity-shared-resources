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

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static jetbrains.buildServer.sharedResources.server.FeatureParams.LOCKS_FEATURE_PARAM_KEY;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public final class LocksImpl implements Locks {

  private static final Logger LOG = Logger.getInstance(LocksImpl.class.getName());

  private static final int PREFIX_OFFSET = LOCK_PREFIX.length();

  // BEGIN interface Locks

  @NotNull
  @Override
  public Map<String, Lock> fromFeatureParameters(@NotNull final SBuildFeatureDescriptor descriptor) {
    return getLocksInternal(descriptor.getParameters());
  }

  @NotNull
  @Override
  public Map<String, Lock> fromFeatureParameters(@NotNull final Map<String, String> parameters) {
    return getLocksInternal(parameters);
  }

  @NotNull
  @Override
  public Map<String, String> asBuildParameters(@NotNull final Collection<Lock> locks) {
    final Map<String, String> buildParams = new HashMap<String, String>();
    for (Lock lock: locks) {
      buildParams.put(lockAsBuildParam(lock), "");
    }
    return buildParams;
  }

  @NotNull
  @Override
  public Collection<Lock> fromBuildParameters(@NotNull final Map<String, String> buildParams) {
    final List<Lock> result = new ArrayList<Lock>();
    for (String str: buildParams.keySet()) {
      Lock lock = getLockFromBuildParam(str);
      if (lock != null) {
        result.add(lock);
      }
    }
    return result;
  }

  @NotNull
  @Override
  public String asFeatureParameter(@NotNull final Collection<Lock> locks) {
    String result;
    if (locks.isEmpty()) {
      result = StringUtil.EMPTY;
    } else {
      final StringBuilder builder = new StringBuilder();
      for (Lock lock: locks) {
        builder.append(lock.getName()).append(" ");
        builder.append(lock.getType()).append("\n");
      }
      result = builder.substring(0, builder.length() - 1);
    }
    return result;
  }

  // END interface Locks

  // utility methods

  /**
   * Converts given locks to a {@code String} that is suitable to
   * exposure as a build parameter name
   *
   * @see Locks#LOCK_PREFIX
   * @param lock lock to convert
   * @return lock as {@code String}
   */
  @NotNull
  private String lockAsBuildParam(@NotNull final Lock lock) {
    final StringBuilder sb = new StringBuilder(LOCK_PREFIX);
    sb.append(lock.getType());
    sb.append(".");
    sb.append(lock.getName());
    return sb.toString();
  }

  @NotNull
  private Map<String, Lock> getLocksInternal(@NotNull final Map<String, String> parameters) {
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

  @Nullable
  private Lock getSingleLockFromString(@NotNull final String str) {
    int n = str.lastIndexOf(' ');
    final LockType type = LockType.byName(str.substring(n + 1));
    Lock result = null;
    if (type != null) {
      result =  new Lock(str.substring(0, n), type);
    }
    return result;
  }

  /**
   * Extracts lock from build parameter name
   *
   * @param paramName name of the build parameter
   * @return {@code Lock} of appropriate type, if parsing was successful, {@code null} otherwise
   */
  @Nullable
  public static Lock getLockFromBuildParam(@NotNull final String paramName) {
    Lock result = null;
    if (paramName.startsWith(LOCK_PREFIX)) {
      String lockString = paramName.substring(PREFIX_OFFSET);
      LockType lockType = null;
      for (LockType type : LockType.values()) {
        if (lockString.startsWith(type.getName())) {
          lockType = type;
          break;
        }
      }

      if (lockType == null) {
        LOG.warn("Error parsing lock type of '" + paramName + "'. Supported values are " + Arrays.toString(LockType.values()));
        return null;
      }

      try {
        String lockName = lockString.substring(lockType.getName().length() + 1);
        if (lockName.length() == 0) {
          LOG.warn("Error parsing lock name of '" + paramName + "'. Supported format is 'teamcity.locks.[read|write]Lock.<lock name>'");
          return null;
        }
        result = new Lock(lockName, lockType);
      } catch (IndexOutOfBoundsException e) {
        LOG.warn("Error parsing lock name of '" + paramName + "'. Supported format is 'teamcity.locks.[read|write]Lock.<lock name>'");
        return null;
      }
    }
    return result;
  }


}
