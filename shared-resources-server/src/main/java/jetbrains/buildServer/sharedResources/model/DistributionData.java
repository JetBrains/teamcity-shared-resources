

package jetbrains.buildServer.sharedResources.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.serverSide.BuildPromotion;
import jetbrains.buildServer.sharedResources.server.runtime.ReservedValuesProvider;
import org.jetbrains.annotations.NotNull;

public class DistributionData {

  private final Map<String, List<BuildPromotion>> myFairSet = new HashMap<>();
  private final ReservedValuesProvider myReservedValuesProvider = new ReservedValuesProvider();

  public Map<String, List<BuildPromotion>> getFairSet() {
    return myFairSet;
  }

  @NotNull
  public ReservedValuesProvider getReservedValuesProvider() {
    return myReservedValuesProvider;
  }
}