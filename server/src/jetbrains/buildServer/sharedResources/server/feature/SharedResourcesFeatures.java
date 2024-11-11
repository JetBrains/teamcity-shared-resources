

package jetbrains.buildServer.sharedResources.server.feature;

import java.util.Collection;
import jetbrains.buildServer.serverSide.BuildPromotion;
import jetbrains.buildServer.serverSide.BuildTypeSettings;
import org.jetbrains.annotations.NotNull;

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
   * @param settings build type or template
   * @return {@code Collection} of build features of type {@code SharedResourcesBuildFeature} of there are any,
   *         {@code empty list} if there are none.
   *         <p/>
   *         <b>Be aware, that parameters are not resolved here</b>
   * @see jetbrains.buildServer.sharedResources.server.SharedResourcesBuildFeature#FEATURE_TYPE
   *      <p/>
   */
  @NotNull
  Collection<SharedResourcesFeature> searchForFeatures(@NotNull final BuildTypeSettings settings);

  /**
   * Searches for features of type {@code SharedResourcesBuildFeature} in given build promotion and its build type
   * Is used because in virtual configurations it's possible to add feature only to build promotion
   *
   * @param promotion build promotion
   * @return {@code Collection} of build features of type {@code SharedResourcesBuildFeature} of there are any,
   *         {@code empty list} if there are none.
   *         <p/>
   *         <b>Be aware, that parameters are not resolved here</b>
   * @see jetbrains.buildServer.sharedResources.server.SharedResourcesBuildFeature#FEATURE_TYPE
   *      <p/>
   */
  @NotNull
  Collection<SharedResourcesFeature> searchForFeatures(@NotNull final BuildPromotion promotion);
}