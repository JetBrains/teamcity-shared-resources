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

import java.util.*;
import java.util.stream.Collectors;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class SharedResourcesBean {

  @NotNull
  private final SProject myProject;

  @NotNull
  private final List<Resource> myOwnResources;

  @NotNull
  private Map<String, List<Resource>> myResourceMap;

  public SharedResourcesBean(@NotNull final SProject project,
                             @NotNull final Resources resources,
                             boolean forProjectPage) {
    this(project, resources, forProjectPage, Collections.emptySet());
  }

  public SharedResourcesBean(@NotNull final SProject project,
                             @NotNull final Resources resources,
                             boolean forProjectPage,
                             @NotNull final Set<String> available) {
    myProject = project;
    if (forProjectPage) {
      // no filtering of any kind
      myOwnResources = resources.getAllOwnResources(project);
      myResourceMap = resources.getResources(project).stream()
                               .collect(Collectors.groupingBy(Resource::getProjectId));
    } else {
      myOwnResources = resources.getOwnResources(project).stream()
                                .filter(resource -> available.contains(resource.getName()))
                                .collect(Collectors.toList());
      myResourceMap = resources.getResources(project).stream()
                               .filter(resource -> available.contains(resource.getName()))
                               .collect(Collectors.groupingBy(Resource::getProjectId));
    }
  }

  @NotNull
  public List<Resource> getOwnResources() {
    return myOwnResources;
  }

  public Map<String, List<Resource>> getInheritedResources() {
    final Map<String, List<Resource>> result = new LinkedHashMap<>(myResourceMap);
    result.remove(myProject.getProjectId());
    return result;
  }

  @NotNull
  public SProject getProject() {
    return myProject;
  }

  @NotNull
  public List<SProject> getProjectPath() {
    List<SProject> result = myProject.getProjectPath();
    Collections.reverse(result);
    return result;
  }

  @NotNull
  public Collection<Resource> getAllResources() {
    return myResourceMap.values().stream()
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());
  }

}
