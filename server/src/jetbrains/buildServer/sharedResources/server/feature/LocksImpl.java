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

package jetbrains.buildServer.sharedResources.server.feature;

import java.util.*;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.sharedResources.server.feature.FeatureParams.LOCKS_FEATURE_PARAM_KEY;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public final class LocksImpl implements Locks {

  @NotNull
  @Override
  public Map<String, Lock> fromFeatureParameters(@NotNull final SBuildFeatureDescriptor descriptor) {
    return fromFeatureParametersInternal(descriptor.getParameters());
  }

  @NotNull
  @Override
  public Map<String, Lock> fromFeatureParameters(@NotNull final Map<String, String> parameters) {
    return fromFeatureParametersInternal(parameters);
  }

  @NotNull
  @Override
  public Map<String, String> asBuildParameters(@NotNull final Collection<Lock> locks) {
    final Map<String, String> buildParams = new HashMap<>();
    for (Lock lock: locks) {
      buildParams.put(asBuildParameterInternal(lock), lock.getValue());
    }
    return buildParams;
  }

  @NotNull
  @Override
  public Map<String, Lock> fromBuildFeaturesAsMap(@NotNull final Collection<SharedResourcesFeature> features) {
    final Map<String, Lock> result = new LinkedHashMap<>();       // enforced -> my -> template
    features.stream()
            .flatMap(f -> f.getLockedResources().entrySet().stream())
            .forEach(e -> result.putIfAbsent(e.getKey(), e.getValue()));
    return result;
  }

  @NotNull
  @Override
  public String asBuildParameter(@NotNull final Lock lock) {
    return asBuildParameterInternal(lock);
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
        builder.append(lock.getType()).append(" ");
        builder.append(lock.getValue()).append("\n");
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
  @SuppressWarnings("StringBufferReplaceableByString")
  @NotNull
  private String asBuildParameterInternal(@NotNull final Lock lock) {
    final StringBuilder sb = new StringBuilder(LOCK_PREFIX);
    sb.append(lock.getType());
    sb.append(".");
    sb.append(lock.getName());
    return sb.toString();
  }

  @NotNull
  private Map<String, Lock> fromFeatureParametersInternal(@NotNull final Map<String, String> featureParameters) {
    final String locksString = featureParameters.get(LOCKS_FEATURE_PARAM_KEY);
    final Map<String, Lock> result = new LinkedHashMap<>();
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
    // get location of Type
    int t = -1; // position of type
    LockType type = null;
    for (LockType lockType: LockType.values()) {
      t = str.lastIndexOf(lockType.getName());
      if (t > 0) {
        type = lockType;
        break;
      }
    }
    Lock result = null;
    if (type != null) {
      final String name = str.substring(0, t).trim();
      int m = str.indexOf(' ', t + 1);
      // lock is valid
      if (m > 0) {
        // values
        result = new Lock(name, type, str.substring(m + 1).trim());
      } else {
        // no values
        result = new Lock(name, type);
      }
    }
    return result;
  }
}
