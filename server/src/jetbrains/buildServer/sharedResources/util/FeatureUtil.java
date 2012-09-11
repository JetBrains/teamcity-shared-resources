package jetbrains.buildServer.sharedResources.util;

import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 *
 * @author Oleg Rybak
 */
public class FeatureUtil {

  /**
   * Extracts required shared resources locks from build
   * @param type build
   * @return set of required locks (Strings for now)
   *
   */
  @NotNull
  public static Set<String> extractLocks(@NotNull SBuildType type) {
    Set<String> result = new HashSet<String>();
    for (SBuildFeatureDescriptor descriptor: type.getBuildFeatures()) {
      extractResource(result, descriptor);
    }
    return result;
  }

  /**
   * Extracts single resource from build descriptor and puts it into resulting set
   * @param result resulting set of resources
   * @param descriptor descriptor to extract resource from
   */
  public static void extractResource(@NotNull Set<String> result, @NotNull SBuildFeatureDescriptor descriptor) {
    if (SharedResourcesPluginConstants.FEATURE_TYPE.equals(descriptor.getType())) {
      result.add(descriptor.getParameters().get(SharedResourcesPluginConstants.RESOURCE_PARAM_KEY));
    }
  }

  /**
   * Checks whether sets of taken locks and required locks have intersection
   * @param requiredLocks required locks
   * @param takenLocks taken locks
   * @return {@code true} if there is an intersection, {@code false} otherwise
   */
  public static boolean lockSetsCrossing(@NotNull Set<String> requiredLocks, @NotNull Set<String> takenLocks) {
    boolean result = false;
    for (String str: requiredLocks) {
      if (takenLocks.contains(str)) {
        result = true;
        break;
      }
    }
    return result;
  }

  @NotNull
  public static Collection<String> toCollection(@NotNull String str) {
    List<String> result = new LinkedList<String>();
    String[] lines = str.split(" *[,\n\r] *");
    for (String line: lines) {
      String trimmed = line.trim();
      if (!StringUtil.isEmptyOrSpaces(trimmed)) {
        result.add(trimmed);
      }
    }
    return result;
  }

  @NotNull
  public static String fromCollection(@NotNull Collection<String> strings) {
    String result;
    if (strings.isEmpty()) {
      result = "";
    } else {
      StringBuilder builder = new StringBuilder();
      for (String str: strings) {
        builder.append(str).append('\n');
      }
      result = builder.substring(0, builder.length() - 1);
    }
    return result;
  }

}
