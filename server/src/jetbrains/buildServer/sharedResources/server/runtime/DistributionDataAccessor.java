

package jetbrains.buildServer.sharedResources.server.runtime;

import java.util.List;
import java.util.Map;
import jetbrains.buildServer.serverSide.BuildPromotion;
import jetbrains.buildServer.serverSide.buildDistribution.AgentsFilterContext;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.DistributionData;
import org.jetbrains.annotations.NotNull;

public class DistributionDataAccessor {

  @NotNull
  static final String DISTRIBUTION_DATA_KEY = SharedResourcesPluginConstants.PLUGIN_NAME;

  private DistributionData myData;

  public DistributionDataAccessor(@NotNull AgentsFilterContext context) {
    myData = (DistributionData)context.getCustomData(DISTRIBUTION_DATA_KEY);
    if (myData == null) {
      myData = new DistributionData();
      context.setCustomData(DISTRIBUTION_DATA_KEY, myData);
    }
  }

  public Map<String, List<BuildPromotion>> getFairSet() {
    return myData.getFairSet();
  }

  public ResourceAffinity getResourceAffinity() {
    return myData.getResourceAffinity();
  }
}