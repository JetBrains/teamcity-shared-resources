

package jetbrains.buildServer.sharedResources.server.project;

import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Oleg Rybak <oleg.rybak@jetbrains.com>
 */
public class ResourceProjectFeatureImpl implements ResourceProjectFeature {

  @NotNull
  private final SProjectFeatureDescriptor myDescriptor;

  @Nullable
  private final Resource myResource;

  public ResourceProjectFeatureImpl(@NotNull final SProjectFeatureDescriptor descriptor) {
    myDescriptor = descriptor;
    myResource = ResourceFactory.fromDescriptor(myDescriptor);
  }

  @Nullable
  @Override
  public Resource getResource() {
    return myResource;
  }

  @NotNull
  @Override
  public String getId() {
    return myDescriptor.getId();
  }
}