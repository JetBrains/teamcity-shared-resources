

package jetbrains.buildServer.sharedResources.server.analysis;

import java.util.List;
import java.util.Map;
import jetbrains.buildServer.serverSide.BuildTypeTemplate;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.sharedResources.model.Lock;
import org.jetbrains.annotations.NotNull;

public class FindUsagesResult {

  @NotNull
  private final Map<SBuildType, List<Lock>> myBuildTypes;

  @NotNull
  private final Map<BuildTypeTemplate, List<Lock>> myTemplates;

  FindUsagesResult(@NotNull final Map<SBuildType, List<Lock>> buildTypes,
                   @NotNull final Map<BuildTypeTemplate, List<Lock>> templates) {
    myBuildTypes = buildTypes;
    myTemplates = templates;
  }

  public int getTotal() {
    return myBuildTypes.size() + myTemplates.size();
  }

  @NotNull
  public Map<SBuildType, List<Lock>> getBuildTypes() {
    return myBuildTypes;
  }

  @NotNull
  public Map<BuildTypeTemplate, List<Lock>> getTemplates() {
    return myTemplates;
  }
}