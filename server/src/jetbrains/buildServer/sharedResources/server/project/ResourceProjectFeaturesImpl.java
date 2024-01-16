

package jetbrains.buildServer.sharedResources.server.project;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.FEATURE_TYPE;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class ResourceProjectFeaturesImpl implements ResourceProjectFeatures {

  @Override
  public SProjectFeatureDescriptor addFeature(@NotNull final SProject project,
                                              @NotNull final Map<String, String> featureParameters) {
    return project.addFeature(FEATURE_TYPE, featureParameters);
  }

  @Override
  @Nullable
  public SProjectFeatureDescriptor removeFeature(@NotNull final SProject project, @NotNull final String id) {
    final SProjectFeatureDescriptor descriptor = getFeatureById(project, id);
    if (descriptor != null) {
      project.removeFeature(descriptor.getId());
    }
    return descriptor;
  }

  public void updateFeature(@NotNull final SProject project,
                            @NotNull final String id,
                            @NotNull final Map<String, String> featureParameters) {
    final SProjectFeatureDescriptor descriptor = getFeatureById(project, id);
    if (descriptor != null) {
      project.updateFeature(id, FEATURE_TYPE, featureParameters);
    }
  }

  @NotNull
  @Override
  public List<ResourceProjectFeature> getOwnFeatures(@NotNull final SProject project) {
    return getResourceFeatures(project).stream()
                                       .map(ResourceProjectFeatureImpl::new)
                                       .collect(Collectors.toList());
  }

  @Nullable
  private SProjectFeatureDescriptor getFeatureById(@NotNull final SProject project, @NotNull final String id) {
    return getResourceFeatures(project).stream()
                                       .filter(fd -> id.equals(fd.getId()))
                                       .findFirst()
                                       .orElse(null);
  }

  @NotNull
  private Collection<SProjectFeatureDescriptor> getResourceFeatures(@NotNull final SProject project) {
    return project.getOwnFeaturesOfType(FEATURE_TYPE);
  }
}