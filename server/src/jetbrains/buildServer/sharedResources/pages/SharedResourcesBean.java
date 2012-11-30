package jetbrains.buildServer.sharedResources.pages;

import jetbrains.buildServer.sharedResources.model.resources.Resource;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class SharedResourcesBean {

  private Collection<Resource> myResources = new ArrayList<Resource>();

  public SharedResourcesBean(Collection<Resource> resources) {
    myResources = resources;
  }

  public Collection<Resource> getResources() {
    return myResources;
  }
}
