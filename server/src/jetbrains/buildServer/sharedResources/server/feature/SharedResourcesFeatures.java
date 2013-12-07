package jetbrains.buildServer.sharedResources.server.feature;

import jetbrains.buildServer.serverSide.SBuildType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Interface {@code SharedResourceFeatures}
 * <p/>
 * Detects and extracts {@code SharedResourcesFeatures} from build type
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public interface SharedResourcesFeatures {

  /**
   * Searches for features of type {@code SharedResourcesBuildFeature} in given build type
   *
   * @param buildType build type to search in
   * @return {@code Collection} of build features of type {@code SharedResourcesBuildFeature} of there are any,
   *         {@code empty list} if there are none.
   *         <p/>
   *         <b>Be aware, that parameters are not resolved here</b>
   * @see jetbrains.buildServer.sharedResources.server.SharedResourcesBuildFeature#FEATURE_TYPE
   *      <p/>
   *      ---
   *      This method is made very light, just to ensure that feature is present in build type.
   *      No settings are resolved
   *      No enabled/disabled status is checked.
   *      ---
   */
  @NotNull
  Collection<SharedResourcesFeature> searchForFeatures(@NotNull final SBuildType buildType);
}
