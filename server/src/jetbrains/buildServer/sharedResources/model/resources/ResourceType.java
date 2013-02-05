package jetbrains.buildServer.sharedResources.model.resources;

import org.jetbrains.annotations.Nullable;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public enum ResourceType {

  /**
   * Resource with quota without custom values.
   * Quota can be infinite
   */
  QUOTED,

  /**
   * Resource that has custom value space
   */
  CUSTOM;

  @Nullable
  public static ResourceType fromString(@Nullable final String str) {
    if (str == null) {
      return null;
    } else {
      for (ResourceType type: values()) {
        if (type.toString().equalsIgnoreCase(str)) {
          return type;
        }
      }
    }
    return null;
  }

}
