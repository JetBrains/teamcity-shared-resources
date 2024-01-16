

package jetbrains.buildServer.sharedResources.server.project;

import java.util.List;
import java.util.Map;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public interface ResourceProjectFeatures {

  @NotNull
  List<ResourceProjectFeature> getOwnFeatures(@NotNull final SProject project);

  SProjectFeatureDescriptor addFeature(@NotNull final SProject project,
                  @NotNull final Map<String, String> featureParameters);

  void updateFeature(@NotNull final SProject project,
                     @NotNull final String id,
                     @NotNull final Map<String, String> featureParameters);

  @Nullable
  SProjectFeatureDescriptor removeFeature(@NotNull final SProject project, @NotNull final String id);
}