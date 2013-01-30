package jetbrains.buildServer.sharedResources.server.feature;

import jetbrains.buildServer.serverSide.SBuildType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Interface {@code SharedResourceFeatures}
 *
 * what does it do? What must it do?
 *
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public interface SharedResourcesFeatures {

  /**
   * Searches for features of type {@code SharedResourcesBuildFeature} in given build type
   *
   * @param buildType build type to search in
   * @return {@code Collection} of build features of type {@code SharedResourcesBuildFeature} of there are any,
   * {@code empty list} if there are none.
   *
   * <b>Be aware, that parameters are not resolved here</b>
   * @see SharedResourcesFeatures#searchForResolvedFeatures(jetbrains.buildServer.serverSide.SBuildType)
   * @see jetbrains.buildServer.sharedResources.server.SharedResourcesBuildFeature#FEATURE_TYPE
   *
   * ---
   * This method is made very light, just to ensure that feature is present in build type.
   * No settings are resolved
   * No enabled/disabled status is checked.
   * ---
   */
  @NotNull
  public Collection<SharedResourcesFeature> searchForFeatures(@NotNull final SBuildType buildType);

  /**
   * Searches for features of type {@code SharedResourcesBuildFeature} in the resolved settings.
   *
   * Only descriptors of {@code enabled} build features are returned
   *
   *
   * @param buildType build type to search in
   * @return {@code Collection} of build features of type {@code SharedResourcesBuildFeature} of there are any,
   * {@code empty list} if there are none.
   *
   * @see SBuildType#isEnabled(String)
   * @see jetbrains.buildServer.sharedResources.server.SharedResourcesBuildFeature#FEATURE_TYPE
   *
   */
  @NotNull
  public Collection<SharedResourcesFeature> searchForResolvedFeatures(@NotNull final SBuildType buildType);


  /**
   * Light method used to indicate, that feature corresponding to shared resources is present
   *
   * @param buildType build type to search in
   * @return {@code true} if we have a feature inside given {@code buildType}, {@code false} otherwise
   * @see jetbrains.buildServer.sharedResources.server.SharedResourcesBuildFeature#FEATURE_TYPE
   */
  public boolean featuresPresent(@NotNull final SBuildType buildType);


}
