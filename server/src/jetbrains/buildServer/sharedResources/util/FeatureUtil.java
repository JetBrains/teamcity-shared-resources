package jetbrains.buildServer.sharedResources.util;

import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.RESOURCE_PARAM_KEY;

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
      extractResources(result, descriptor);
    }
    return result;
  }

  /**
   * Extracts single resource from build descriptor and puts it into resulting set
   * @param result resulting set of resources
   * @param descriptor descriptor to extract resource from
   */
  public static void extractResources(@NotNull Set<String> result, @NotNull SBuildFeatureDescriptor descriptor) {
    if (SharedResourcesPluginConstants.FEATURE_TYPE.equals(descriptor.getType())) {
      result.addAll(FeatureUtil.toCollection(descriptor.getParameters().get(RESOURCE_PARAM_KEY)));
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

  /**
   * Takes string with multiple parameters and converts it t collection of separate parameters
   * @param str string, with each parameter on new line
   * @return collection of individual parameters
   */
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

  /**
   * Takes collection of separate parameters and joins them into a string, separated by new line char
   * @param strings collection of separate parameters
   * @return string, with each parameter on new line
   */
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
