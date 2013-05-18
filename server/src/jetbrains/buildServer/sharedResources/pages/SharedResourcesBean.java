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
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class SharedResourcesBean {

  @Nullable
  private SProject myProject;

  @NotNull
  private Map<SProject, Map<String, Resource>> myProjectResources = new HashMap<SProject, Map<String, Resource>>();

  @NotNull
  private Map<String, Map<SBuildType, LockType>> myUsageMap = new HashMap<String, Map<SBuildType, LockType>>();


  public SharedResourcesBean(@Nullable final SProject project,
                             @NotNull final Map<SProject, Map<String, Resource>> projectResources,
                             @NotNull final Map<String, Map<SBuildType, LockType>> usageMap) {
    myProject = project;
    myProjectResources = projectResources;
    myUsageMap = usageMap;
  }

  public SharedResourcesBean(@Nullable final SProject project, @NotNull final Map<SProject, Map<String, Resource>> projectResources) {
    this(project, projectResources, Collections.<String, Map<SBuildType, LockType>>emptyMap());
  }

  public SharedResourcesBean(@Nullable final SProject project) {
    this(project, Collections.<SProject, Map<String, Resource>>emptyMap(), Collections.<String, Map<SBuildType, LockType>>emptyMap());
  }

  @NotNull
  public Map<String, Map<SBuildType, LockType>> getUsageMap() {
    return Collections.unmodifiableMap(myUsageMap);
  }

  @NotNull
  public Map<String, Resource> getMyResources() {
    Map<String, Resource> result = myProjectResources.get(myProject);
    if (result == null) {
      result = Collections.emptyMap();
    }
    return result;
  }

  @NotNull
  public Map<SProject, Map<String, Resource>> getInheritedResources() {
    final Map<SProject, Map<String, Resource>> result = new HashMap<SProject, Map<String, Resource>>(myProjectResources);
    result.remove(myProject);
    return result;
  }

  @NotNull
  public Collection<Resource> getAllResources() {
    final Collection<Resource> result = new HashSet<Resource>();
    for (Map<String, Resource> map : myProjectResources.values()) {
      result.addAll(map.values());
    }
    return result;
  }
}
