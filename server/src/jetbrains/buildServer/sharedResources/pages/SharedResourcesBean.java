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
import jetbrains.buildServer.sharedResources.model.Lock;
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
  private final SProject myProject;

  @NotNull
  private Map<SProject, Map<String, Resource>> myProjectResources = new HashMap<SProject, Map<String, Resource>>();

  @NotNull
  private final Map<SBuildType, Map<Lock, String>> myConfigurationErrors;

  public SharedResourcesBean(@NotNull final SProject project,
                             @NotNull final Map<SProject, Map<String, Resource>> projectResources,
                             @NotNull final Map<SBuildType, Map<Lock, String>> configurationErrors) {
    myProject = project;
    myProjectResources = projectResources;
    myConfigurationErrors = configurationErrors;
  }

  public SharedResourcesBean(@NotNull final SProject project, @NotNull final Map<SProject, Map<String, Resource>> projectResources) {
    this(project, projectResources, Collections.<SBuildType, Map<Lock, String>>emptyMap());
  }

  @NotNull
  public SProject getProject() {
    return myProject;
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
    final Map<SProject, Map<String, Resource>> result = new LinkedHashMap<SProject, Map<String, Resource>>(myProjectResources);
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

  @NotNull
  public Map<SBuildType, Map<Lock, String>> getConfigurationErrors() {
    return Collections.unmodifiableMap(myConfigurationErrors);
  }
}
