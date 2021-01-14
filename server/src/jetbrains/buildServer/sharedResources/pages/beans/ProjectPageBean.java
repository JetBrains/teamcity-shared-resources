/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

package jetbrains.buildServer.sharedResources.pages.beans;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.RESOURCE_BY_NAME_COMPARATOR;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class ProjectPageBean {

  @NotNull
  private final SProject myProject;

  @NotNull
  private final List<Resource> myOwnResources;

  @NotNull
  private final Map<String, List<Resource>> myTreeResources;

  @NotNull
  private Map<String, Resource> myOverridesMap;

  ProjectPageBean(@NotNull final SProject project,
                  @NotNull final List<Resource> allOwnResources,
                  @NotNull final Map<String, List<Resource>> treeResources,
                  @NotNull final Map<String, Resource> overridesMap) {
    myProject = project;
    // _ALL_ own resources are supplied separately, as we display duplicates on the page.
    // tree resources ignores resources with non unique names
    myOwnResources = allOwnResources;
    myTreeResources = treeResources;
    myOverridesMap = overridesMap;
  }

  @NotNull
  public SProject getProject() {
    return myProject;
  }

  public Map<String, SProject> getProjects() {
    return myProject.getProjectPath().stream().collect(Collectors.toMap(SProject::getProjectId, Function.identity()));
  }
  @NotNull
  public List<SProject> getProjectPath() {
    final List<SProject> result = myProject.getProjectPath();
    Collections.reverse(result);
    return result;
  }

  @NotNull
  public List<Resource> getOwnResources() {
    return myOwnResources;
  }

  @NotNull
  public Map<String, List<Resource>> getInheritedResources() {
    return myTreeResources;
  }

  @NotNull
  public Map<String, Resource> getOverridesMap() {
    return myOverridesMap;
  }
}
