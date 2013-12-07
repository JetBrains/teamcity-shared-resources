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
public final class SharedResourcesFeaturesImpl implements SharedResourcesFeatures {

  @NotNull
  private final SharedResourcesFeatureFactory myFactory;

  public SharedResourcesFeaturesImpl(@NotNull final SharedResourcesFeatureFactory factory) {
    myFactory = factory;
  }

  @NotNull
  @Override
  public Collection<SharedResourcesFeature> searchForFeatures(@NotNull final SBuildType buildType) {
    return createFeatures(getEnabledUnresolvedFeatureDescriptors(buildType));
  }

  @NotNull
  private Collection<SharedResourcesFeature> createFeatures(@NotNull final Collection<SBuildFeatureDescriptor> descriptors) {
    final List<SharedResourcesFeature> result = new ArrayList<SharedResourcesFeature>();
    for (SBuildFeatureDescriptor descriptor : descriptors) {
      result.add(myFactory.createFeature(descriptor));
    }
    return result;
  }

  @NotNull
  private Collection<SBuildFeatureDescriptor> getEnabledUnresolvedFeatureDescriptors(@NotNull final SBuildType buildType) {
    final List<SBuildFeatureDescriptor> result = new ArrayList<SBuildFeatureDescriptor>();
    for (SBuildFeatureDescriptor descriptor : buildType.getBuildFeatures()) {
      if (FEATURE_TYPE.equals(descriptor.getType()) && buildType.isEnabled(descriptor.getId())) {
        result.add(descriptor);
      }
    }
    return result;
  }
}
