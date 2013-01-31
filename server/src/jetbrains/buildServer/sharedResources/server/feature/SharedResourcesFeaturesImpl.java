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
    //todo:  why this method returns build features that belong to template???
    return searchForFeatureInternal(buildType.getBuildFeatures());
  }

  @NotNull
  private Collection<SharedResourcesFeature> searchForFeatureInternal(@NotNull final Collection<SBuildFeatureDescriptor> descriptors) {
    final List<SharedResourcesFeature> result = new ArrayList<SharedResourcesFeature>();
    for (SBuildFeatureDescriptor descriptor : descriptors) {
      if (FEATURE_TYPE.equals(descriptor.getType())) {
        result.add(myFactory.createFeature(descriptor));
      }
    }
    return result;
  }

  @Override
  @NotNull
  public Collection<SharedResourcesFeature> searchForResolvedFeatures(@NotNull final SBuildType buildType) {
    final List<SharedResourcesFeature> result = new ArrayList<SharedResourcesFeature>();
    for (SBuildFeatureDescriptor descriptor: buildType.getResolvedSettings().getBuildFeatures()) {
      if (FEATURE_TYPE.equals(descriptor.getType()) && buildType.isEnabled(descriptor.getId())) {
        result.add(myFactory.createFeature(descriptor));
      }
    }
    return result;
  }



  @Override
  public boolean featuresPresent(@NotNull SBuildType buildType) {
    boolean result = false;
    for (SBuildFeatureDescriptor descriptor: buildType.getBuildFeatures()) {
      if (FEATURE_TYPE.equals(descriptor.getType())) {
        result = true;
        break;
      }
    }
    return result;
  }
}
