

package jetbrains.buildServer.sharedResources.pages.beans;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import org.jetbrains.annotations.NotNull;

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
  private final Map<String, Resource> myOverridesMap;

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