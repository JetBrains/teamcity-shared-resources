

package jetbrains.buildServer.sharedResources.server.runtime;

import java.util.List;
import java.util.Map;
import jetbrains.buildServer.serverSide.BuildPromotion;
import jetbrains.buildServer.serverSide.buildDistribution.BuildDistributorInput;
import jetbrains.buildServer.serverSide.impl.buildDistribution.BuildDistributorInputEx;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.DistributionData;
import org.jetbrains.annotations.NotNull;

public class DistributionDataAccessor {

  @NotNull
  static final String DISTRIBUTION_DATA_KEY = SharedResourcesPluginConstants.PLUGIN_NAME;

  private DistributionData myData;

  public DistributionDataAccessor(BuildDistributorInput input) {
    BuildDistributorInputEx inputEx = (BuildDistributorInputEx) input;
    myData = inputEx.getCustomData(DISTRIBUTION_DATA_KEY, DistributionData.class);
    if (myData == null) {
      myData = new DistributionData();
      inputEx.setCustomData(DISTRIBUTION_DATA_KEY, myData);
    }
  }

  public Map<String, List<BuildPromotion>> getFairSet() {
    return myData.getFairSet();
  }

  @NotNull
  public ReservedValuesProvider getReservedValuesProvider() {
    return myData.getReservedValuesProvider();
  }
}