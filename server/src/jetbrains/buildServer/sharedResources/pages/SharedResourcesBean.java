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

package jetbrains.buildServer.sharedResources.pages;

import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class SharedResourcesBean {

  @NotNull
  private Collection<Resource> myResources = new ArrayList<Resource>();

  @NotNull
  private Map<String, Map<SBuildType, LockType>> myUsageMap = new HashMap<String, Map<SBuildType, LockType>>();

  public SharedResourcesBean(@NotNull final Collection<Resource> resources) {
    this(resources, Collections.<String, Map<SBuildType, LockType>>emptyMap());
  }

  public SharedResourcesBean(@NotNull final Collection<Resource> resources, @NotNull final Map<String, Map<SBuildType, LockType>> usageMap) {
    myResources = resources;
    myUsageMap = usageMap;
  }

  @NotNull
  public Collection<Resource> getResources() {
    return Collections.unmodifiableCollection(myResources);
  }

  @NotNull
  public Map<String, Map<SBuildType, LockType>> getUsageMap() {
    return Collections.unmodifiableMap(myUsageMap);
  }
}
