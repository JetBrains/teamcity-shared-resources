

package jetbrains.buildServer.sharedResources.server.feature;

import java.util.*;
import jetbrains.buildServer.serverSide.BuildPromotion;
import jetbrains.buildServer.serverSide.BuildTypeSettings;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import org.jetbrains.annotations.NotNull;

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
  public Collection<SharedResourcesFeature> searchForFeatures(@NotNull final BuildTypeSettings settings) {
    return createFeatures(getEnabledUnresolvedFeatureDescriptors(settings));
  }

  @NotNull
  @Override
  public Collection<SharedResourcesFeature> searchForFeatures(@NotNull final BuildPromotion promotion) {
    return createFeatures(promotion.getBuildFeaturesOfType(FEATURE_TYPE));
  }

  @NotNull
  private Collection<SharedResourcesFeature> createFeatures(@NotNull final Collection<SBuildFeatureDescriptor> descriptors) {
    if (descriptors.isEmpty()) return Collections.emptyList();

    final List<SharedResourcesFeature> result = new ArrayList<>();
    for (SBuildFeatureDescriptor descriptor : descriptors) {
      result.add(myFactory.createFeature(descriptor));
    }
    return result;
  }

  @NotNull
  private Collection<SBuildFeatureDescriptor> getEnabledUnresolvedFeatureDescriptors(@NotNull final BuildTypeSettings settings) {
    final List<SBuildFeatureDescriptor> result = new ArrayList<>();
    for (SBuildFeatureDescriptor descriptor : settings.getBuildFeaturesOfType(FEATURE_TYPE)) {
      if (settings.isEnabled(descriptor.getId())) {
        result.add(descriptor);
      }
    }
    return result;
  }
}