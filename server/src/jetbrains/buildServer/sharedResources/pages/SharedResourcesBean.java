package jetbrains.buildServer.sharedResources.pages;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class SharedResourcesBean {

  private Collection<String> mySharedResourcesNames = new ArrayList<String>();

  public SharedResourcesBean(Collection<String> sharedResourcesNames) {
    this.mySharedResourcesNames = sharedResourcesNames;
  }

  public Collection<String> getSharedResourcesNames() {
    return mySharedResourcesNames;
  }

}
