

package jetbrains.buildServer.sharedResources.server.feature;

import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Interface {@code FeatureParams}
 *
 * Contains methods for dealing with build feature parameters
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public interface FeatureParams {

  /**
   * Key in feature parameters collection, that contains all locks
   */
  @NotNull
  String LOCKS_FEATURE_PARAM_KEY = "locks-param";

  /**
   * Provides description for build feature parameters to be shown in UI
   * @param params build feature parameters
   * @return parameters description
   */
  @NotNull
  String describeParams(@NotNull final Map<String, String> params);

  /**
   * Provides default parameters for build feature
   * @return default parameters
   */
  @NotNull
  Map<String, String> getDefault();
}