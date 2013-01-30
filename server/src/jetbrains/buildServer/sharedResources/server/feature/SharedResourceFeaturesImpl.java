package jetbrains.buildServer.sharedResources.server.feature;

import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SBuildType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static jetbrains.buildServer.sharedResources.server.SharedResourcesBuildFeature.FEATURE_TYPE;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public final class SharedResourceFeaturesImpl implements SharedResourceFeatures {

  @NotNull
  @Override
  public Collection<SBuildFeatureDescriptor> searchForFeatures(@NotNull final SBuildType buildType) {
    final List<SBuildFeatureDescriptor> result = new ArrayList<SBuildFeatureDescriptor>();
    for (SBuildFeatureDescriptor descriptor: buildType.getBuildFeatures()) {
      if (FEATURE_TYPE.equals(descriptor.getType())) {
        result.add(descriptor);
      }
    }
    return result;
  }

  @Override
  @NotNull
  public Collection<SBuildFeatureDescriptor> searchForResolvedFeatures(@NotNull final SBuildType buildType) {
    final List<SBuildFeatureDescriptor> result = new ArrayList<SBuildFeatureDescriptor>();
    for (SBuildFeatureDescriptor descriptor: buildType.getResolvedSettings().getBuildFeatures()) {
      if (FEATURE_TYPE.equals(descriptor.getType()) && buildType.isEnabled(descriptor.getId())) {
        result.add(descriptor);
      }
    }
    return result;
  }
}
