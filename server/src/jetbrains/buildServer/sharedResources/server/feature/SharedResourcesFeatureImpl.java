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
import jetbrains.buildServer.sharedResources.model.resources.CustomResource;
import jetbrains.buildServer.sharedResources.model.resources.QuotedResource;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceType;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static jetbrains.buildServer.sharedResources.server.feature.FeatureParams.LOCKS_FEATURE_PARAM_KEY;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public final class SharedResourcesFeatureImpl implements SharedResourcesFeature {

  @NotNull
  private final Locks myLocks;

  @NotNull
  private final Resources myResources;

  @NotNull
  private final SBuildFeatureDescriptor myDescriptor;

  @NotNull
  private final Map<String, Lock> myLockedResources;


  public SharedResourcesFeatureImpl(@NotNull final Locks locks,
                                    @NotNull final Resources resources,
                                    @NotNull final SBuildFeatureDescriptor descriptor) {
    myLocks = locks;
    myResources = resources;
    myDescriptor = descriptor;
    myLockedResources = myLocks.fromFeatureParameters(myDescriptor);
  }

  @NotNull
  @Override
  public Map<String, Lock> getLockedResources() {
    return Collections.unmodifiableMap(myLockedResources);
  }

  @Override
  public boolean updateLock(@NotNull final SBuildType buildType,
                            @NotNull final String oldName,
                            @NotNull final String newName) {
    boolean result = false;
    final Lock lock = myLockedResources.remove(oldName);
    if (lock != null) {
      // save its type
      result = true;
      final LockType lockType = lock.getType();
      final String lockValue = lock.getValue();
      // add lock with new resource name and saved type
      myLockedResources.put(newName, new Lock(newName, lockType, lockValue));
      // serialize locks
      final String locksAsString = myLocks.asFeatureParameter(myLockedResources.values());
      // update build feature parameters
      final Map<String, String> newParams = new HashMap<>(myDescriptor.getParameters());
      newParams.put(LOCKS_FEATURE_PARAM_KEY, locksAsString);
      // update build feature
      boolean updated = buildType.updateBuildFeature(myDescriptor.getId(), myDescriptor.getType(), newParams);
      // feature belongs to template. That is why it was not updated
      if (!updated) {
        final BuildTypeTemplate template = buildType.getTemplate();
        if (template != null) {
          template.updateBuildFeature(myDescriptor.getId(), myDescriptor.getType(), newParams);
        }
      }
    }
    return result;
  }

  @NotNull
  @Override
  public Map<String, String> getBuildParameters() {
    return myLocks.asBuildParameters(myLockedResources.values());
  }


  @NotNull
  @Override
  public Map<Lock, String> getInvalidLocks(@NotNull final String projectId) {
    // Lock is mapped to description of its invalid state
    final Map<Lock, String> invalidLocks = new HashMap<>();
    // get visible resources
    if (!myLockedResources.isEmpty()) {
      final Map<String, Resource> resources = myResources.getResourcesMap(projectId);
      for (Map.Entry<String, Lock> entry : myLockedResources.entrySet()) {
        // check that resource exists
        Resource r = resources.get(entry.getKey());
        Lock lock = entry.getValue();
        if (r != null) {
          // if lock has value (SPECIFIC case), check that resource is custom and has the value in values collection
          if (!"".equals(lock.getValue())) {
            if (ResourceType.CUSTOM == r.getType()) {
              if (!((CustomResource) r).getValues().contains(lock.getValue())) {
                // values domain does not contain required value
                invalidLocks.put(lock, "Resource '" + lock.getName() + "' does not contain required value '" + lock.getValue() + "'.");
              }
            } else {
              // wrong resource type. Expected quoted / infinite, got custom
              invalidLocks.put(lock, "Resource '" + lock.getName() + "' has wrong type: expected 'custom' got " + (((QuotedResource) r).isInfinite() ? "'infinite'." : "'quoted'."));
            }
          }
        } else {// resource does not exist
          invalidLocks.put(lock, "Resource '" + lock.getName() + "' does not exist.");
        }
      }
    }
    return invalidLocks;
  }
}
