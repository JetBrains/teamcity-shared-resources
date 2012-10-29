package jetbrains.buildServer.sharedResources.pages;

import jetbrains.buildServer.serverSide.SProject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class SharedResourcesBean {

  private List<String> mySharedResourcesNames = new ArrayList<String>();

  private final SProject myProject;

  public SharedResourcesBean(SProject project, List<String> sharedResourcesNames) {
    this.myProject = project;
    this.mySharedResourcesNames = sharedResourcesNames;
  }

  public List<String> getSharedResourcesNames() {
    return mySharedResourcesNames;
  }

  public SProject getProject() {
    return myProject;
  }
}
