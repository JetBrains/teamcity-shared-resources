

package jetbrains.buildServer.sharedResources.pages.beans;

import java.util.Collection;
import java.util.List;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class EditFeatureBean {

  @NotNull
  private final SProject myProject;

  @NotNull
  private final List<Resource> myAllResources;

  EditFeatureBean(@NotNull final SProject project,
                  @NotNull final List<Resource> allResources) {
    myProject = project;
    myAllResources = allResources;
  }

  @NotNull
  public SProject getProject() {
    return myProject;
  }

  @NotNull
  public Collection<Resource> getAllResources() {
    return myAllResources;
  }
}