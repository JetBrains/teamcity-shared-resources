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

import jetbrains.buildServer.serverSide.BuildTypeTemplate;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.sharedResources.server.SharedResourcesUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static jetbrains.buildServer.sharedResources.server.FeatureParams.LOCKS_FEATURE_PARAM_KEY;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public final class SharedResourcesFeatureImpl implements SharedResourcesFeature {

  @NotNull
  private final Locks myLocks;

  @NotNull
  private final SBuildFeatureDescriptor myDescriptor;

  @NotNull
  private final Map<String, Lock> myLockedResources;


  public SharedResourcesFeatureImpl(@NotNull final Locks locks,
                                    @NotNull final SBuildFeatureDescriptor descriptor) {
    myLocks = locks;
    myDescriptor = descriptor;
    myLockedResources = myLocks.getLocksFromFeatureParameters(myDescriptor);
  }

  @NotNull
  @Override
  public Map<String, Lock> getLockedResources() {
    // modification is supported only through this class
    return Collections.unmodifiableMap(myLockedResources);
  }

  // todo: add test
  @Override
  public void updateLock(@NotNull SBuildType buildType, @NotNull String oldName, @NotNull String newName) {
    final Lock lock = myLockedResources.remove(oldName);
    if (lock != null) {
      // 3) save its type
      final LockType lockType = lock.getType();
      // 5) add lock with new resource name and saved type
      myLockedResources.put(newName, new Lock(newName, lockType));
      // 6) serialize locks
      final String locksAsString = SharedResourcesUtils.locksAsString(myLockedResources.values());
      // 7) update build feature parameters
      Map<String, String> newParams = new HashMap<String, String>(myDescriptor.getParameters());
      newParams.put(LOCKS_FEATURE_PARAM_KEY, locksAsString);
      // 8) update build feature
      buildType.updateBuildFeature(myDescriptor.getId(), myDescriptor.getType(), newParams);
      // todo: remove workaround with templates
      final BuildTypeTemplate template = buildType.getTemplate();
      if (template != null) {
       template.updateBuildFeature(myDescriptor.getId(), myDescriptor.getType(), newParams);
      }
      // todo: need reload?
    }
  }

  @NotNull
  @Override
  public Map<String, String> getBuildParameters() {
    return myLocks.asBuildParameters(myLockedResources.values());
  }

}
