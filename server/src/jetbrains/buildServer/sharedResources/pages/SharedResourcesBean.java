package jetbrains.buildServer.sharedResources.pages;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class SharedResourcesBean {

  private List<String> mySharedResourcesNames = new ArrayList<String>();

  public SharedResourcesBean(List<String> sharedResourcesNames) {
    this.mySharedResourcesNames = sharedResourcesNames;
  }

  public List<String> getSharedResourcesNames() {
    return mySharedResourcesNames;
  }

}
