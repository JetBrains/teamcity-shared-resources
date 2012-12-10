package jetbrains.buildServer.sharedResources.pages;

import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.sharedResources.model.resources.Resource;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class SharedResourcesBean {

  private Collection<Resource> myResources = new ArrayList<Resource>();

  private Map<String, Set<SBuildType>> myUsageMap = new HashMap<String, Set<SBuildType>>();

  public SharedResourcesBean(Collection<Resource> resources, Map<String, Set<SBuildType>> usageMap) {
    myResources = resources;
    myUsageMap = usageMap;
  }

  public Collection<Resource> getResources() {
    return myResources;                          // todo: unmodifiable?
  }

  public Map<String, Set<SBuildType>> getUsageMap() {
    return myUsageMap; // todo: unmodifiable?
  }
}
