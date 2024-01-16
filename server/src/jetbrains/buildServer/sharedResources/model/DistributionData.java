

package jetbrains.buildServer.sharedResources.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.serverSide.BuildPromotion;
import jetbrains.buildServer.sharedResources.server.runtime.ResourceAffinity;

public class DistributionData {

  private final Map<String, List<BuildPromotion>> myFairSet = new HashMap<>();
  private final ResourceAffinity myResourceAffinity = new ResourceAffinity();

  public Map<String, List<BuildPromotion>> getFairSet() {
    return myFairSet;
  }

  public ResourceAffinity getResourceAffinity() {
    return myResourceAffinity;
  }
}